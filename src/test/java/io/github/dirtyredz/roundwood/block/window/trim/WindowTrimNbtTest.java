package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The saved window trims (0.8.0, version 2) and the adapter from 0.7.0 (version 1, one look for both
 * faces, meaningful only on a {@code trimmed} state): 0.7.0 data becomes both faces, a trimmed 0.7.0
 * window whose oak trim stored nothing becomes oak on both faces, explicit version-2 data is
 * authoritative even when empty, and an unknown version is never misread and is kept.
 */
class WindowTrimNbtTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    private static final Appearance BRICK = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
    private static final Appearance SPRUCE = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));

    /** What 0.7.0 wrote for a trim look (it omitted the payload for oak). */
    private static CompoundTag v1(Appearance look) {
        CompoundTag parent = new CompoundTag();
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1);
        AppearanceNbt.write(tag, look);
        parent.put(WindowTrimNbt.TAG, tag);
        return parent;
    }

    @Test
    void version2RoundTripsEveryFaceIndependentlyBesideThePane() {
        Appearance[] looks = {null, Appearance.DEFAULT, SPRUCE, SPRUCE.painted(DyeColor.RED), BRICK};
        for (Appearance front : looks) {
            for (Appearance back : looks) {
                FaceTrims trims = new FaceTrims(Optional.ofNullable(front), Optional.ofNullable(back));
                CompoundTag tag = new CompoundTag();
                AppearanceNbt.write(tag, SPRUCE);
                WindowTrimNbt.write(tag, trims);
                assertEquals(2, tag.getCompound(WindowTrimNbt.TAG).getInt("Version"));
                for (boolean trimmed : new boolean[] {false, true}) {
                    FaceTrimStore.Read read = WindowTrimNbt.read(tag, trimmed);
                    assertEquals(trims, read.trims(), trims + " trimmed " + trimmed);
                    assertNull(read.unknown());
                }
                assertEquals(Optional.of(SPRUCE), AppearanceNbt.read(tag), "the pane's own tag is untouched");
            }
        }
    }

    @Test
    void sevenOhDataBecomesBothFaces() {
        for (Appearance look : new Appearance[] {SPRUCE, SPRUCE.painted(DyeColor.CYAN), BRICK}) {
            assertEquals(FaceTrims.both(look), WindowTrimNbt.read(v1(look), true).trims(), look.key());
            assertEquals(FaceTrims.NONE, WindowTrimNbt.read(v1(look), false).trims(), "0.7.0 ignored data on an untrimmed window");
        }
    }

    /** The omitted-oak case: 0.7.0 stored no payload for an oak trim, so the state alone says it is there. */
    @Test
    void aTrimmedWindowWithoutPayloadGetsOakOnBothFaces() {
        assertEquals(FaceTrims.both(Appearance.DEFAULT), WindowTrimNbt.read(new CompoundTag(), true).trims());
        assertEquals(FaceTrims.NONE, WindowTrimNbt.read(new CompoundTag(), false).trims());
        assertEquals(FaceTrims.NONE, WindowTrimNbt.read(null, false).trims());
        CompoundTag missingVersion = v1(BRICK);
        missingVersion.getCompound(WindowTrimNbt.TAG).remove("Version");
        assertEquals(FaceTrims.both(BRICK), WindowTrimNbt.read(missingVersion, true).trims(), "no version reads as 0.7.0");
        CompoundTag unusable = new CompoundTag();
        CompoundTag inner = new CompoundTag();
        inner.putInt("Version", 1);
        inner.putString("Kind", "plastic");
        unusable.put(WindowTrimNbt.TAG, inner);
        assertEquals(FaceTrims.both(Appearance.DEFAULT), WindowTrimNbt.read(unusable, true).trims());
    }

    /** Explicit empty version-2 data wins over a stale {@code trimmed}: a removed trim never comes back. */
    @Test
    void explicitEmptyVersion2IsAuthoritative() {
        CompoundTag tag = new CompoundTag();
        WindowTrimNbt.write(tag, FaceTrims.NONE);
        assertEquals(FaceTrims.NONE, WindowTrimNbt.read(tag, true).trims());
        WindowTrimNbt.write(tag, FaceTrims.NONE.with(AttachmentFace.BACK, BRICK));
        assertEquals(FaceTrims.NONE.with(AttachmentFace.BACK, BRICK), WindowTrimNbt.read(tag, true).trims());
    }

    @Test
    void anUnknownVersionIsKeptAndReadAsNothing() {
        CompoundTag tag = new CompoundTag();
        CompoundTag future = new CompoundTag();
        future.putInt("Version", 3);
        future.putString("Shape", "gothic");
        future.put("Front", new CompoundTag());
        tag.put(WindowTrimNbt.TAG, future);
        FaceTrimStore.Read read = WindowTrimNbt.read(tag, true);
        assertEquals(FaceTrims.NONE, read.trims());
        assertNotNull(read.unknown());
        assertEquals(future, read.unknown());
        assertFalse(read.unknown() == future, "a copy, so later edits to the tag cannot change it");
    }
}
