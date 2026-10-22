package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The saved arch sills (0.10.0, {@code WindowSillTrim} version 1): its own key beside the perimeter
 * trims, a missing payload means no sills (no older layout to adapt, so a 0.7.0 {@code trimmed}
 * summary never invents one), empty is not written, any other version is kept unread and blocks only
 * sill edits, and the two payloads never read each other.
 */
class WindowSillNbtTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    private static final Appearance BRICK = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
    private static final Appearance SPRUCE = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));

    @Test
    void version1RoundTripsEachFaceIndependently() {
        Appearance[] looks = {null, Appearance.DEFAULT, SPRUCE, SPRUCE.painted(DyeColor.BLUE), BRICK};
        for (Appearance front : looks) {
            for (Appearance back : looks) {
                FaceTrims sills = new FaceTrims(Optional.ofNullable(front), Optional.ofNullable(back));
                CompoundTag parent = new CompoundTag();
                WindowSillNbt.write(parent, sills);
                assertEquals(1, parent.getCompound("WindowSillTrim").getInt("Version"));
                FaceTrimStore.Read read = WindowSillNbt.read(parent);
                assertEquals(sills, read.trims());
                assertNull(read.unknown());
            }
        }
    }

    @Test
    void aMissingPayloadIsNoSillsAndEmptyIsNotSaved() {
        assertEquals(FaceTrims.NONE, WindowSillNbt.read(null).trims());
        assertEquals(FaceTrims.NONE, WindowSillNbt.read(new CompoundTag()).trims());
        FaceTrimStore store = new FaceTrimStore(WindowSillNbt.FORMAT);
        CompoundTag saved = new CompoundTag();
        store.save(saved);
        assertFalse(saved.contains("WindowSillTrim"), "an empty sill pair writes nothing: " + saved);
        CompoundTag update = new CompoundTag();
        store.writeUpdate(update);
        assertTrue(update.contains("WindowSillTrim"), "update packets always carry the payload so removals reach clients");
    }

    @Test
    void anUnknownVersionIsKeptUnreadAndRefusesEdits() {
        for (int version : new int[] {0, 2, 99}) {
            CompoundTag parent = new CompoundTag();
            CompoundTag payload = new CompoundTag();
            payload.putInt("Version", version);
            payload.put("Front", new CompoundTag());
            payload.putString("Future", "kept");
            parent.put("WindowSillTrim", payload);
            FaceTrimStore store = new FaceTrimStore(WindowSillNbt.FORMAT);
            store.load(parent, FaceTrimStore.Legacy.NONE);
            assertEquals(FaceTrims.NONE, store.trims(), "version " + version + " misread");
            assertFalse(store.editable());
            assertFalse(store.set(FaceTrims.NONE.with(AttachmentFace.FRONT, BRICK)));
            CompoundTag saved = new CompoundTag();
            store.save(saved);
            assertEquals(payload, saved.getCompound("WindowSillTrim"), "saved back unchanged");
        }
        CompoundTag unversioned = new CompoundTag();
        unversioned.put("WindowSillTrim", new CompoundTag());
        assertNotNull(WindowSillNbt.read(unversioned).unknown(), "a payload without a version is not understood");
    }

    /** The sill payload and the perimeter payload are separate keys: neither reads the other's data. */
    @Test
    void theTwoSlotsNeverReadEachOther() {
        CompoundTag parent = new CompoundTag();
        WindowTrimNbt.write(parent, FaceTrims.both(BRICK));
        assertEquals(FaceTrims.NONE, WindowSillNbt.read(parent).trims(), "perimeter trims read as sills");
        CompoundTag sillsOnly = new CompoundTag();
        WindowSillNbt.write(sillsOnly, FaceTrims.both(SPRUCE));
        WindowTrimNbt.write(sillsOnly, FaceTrims.NONE);
        assertEquals(FaceTrims.NONE, WindowTrimNbt.read(sillsOnly, true).trims(),
                "a sill-only window with a true summary must not read as the 0.7.0 omitted-oak trim");
        assertEquals(FaceTrims.both(SPRUCE), WindowSillNbt.read(sillsOnly).trims());
        assertFalse(AppearanceNbt.read(sillsOnly).isPresent(), "nothing written at the top level");
    }
}
