package io.github.dirtyredz.roundwood.block.attachment;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared face-trim payload (0.8.0): a face is present exactly when its key is, an empty face
 * compound is a present oak trim, the two faces round-trip independently, and the version is read
 * only as an integer.
 */
class FaceTrimsNbtTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    private static final Appearance BRICK = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
    private static final Appearance SPRUCE = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));

    @Test
    void absentAndPresentOakAreDifferent() {
        CompoundTag oakFront = FaceTrimsNbt.encode(FaceTrims.NONE.with(AttachmentFace.FRONT, Appearance.DEFAULT), 7);
        assertTrue(oakFront.contains("Front"), "a present oak face is stored");
        assertTrue(oakFront.getCompound("Front").isEmpty(), "as an empty compound");
        assertFalse(oakFront.contains("Back"), "an absent face has no key");
        assertEquals(new FaceTrims(Optional.of(Appearance.DEFAULT), Optional.empty()), FaceTrimsNbt.decodeFaces(oakFront));
        assertEquals(FaceTrims.NONE, FaceTrimsNbt.decodeFaces(FaceTrimsNbt.encode(FaceTrims.NONE, 7)));
        assertEquals(Optional.of(7), FaceTrimsNbt.version(oakFront));
    }

    @Test
    void everyCombinationRoundTripsIndependently() {
        Appearance[] looks = {null, Appearance.DEFAULT, SPRUCE, SPRUCE.painted(DyeColor.RED), BRICK};
        for (Appearance front : looks) {
            for (Appearance back : looks) {
                FaceTrims trims = new FaceTrims(Optional.ofNullable(front), Optional.ofNullable(back));
                assertEquals(trims, FaceTrimsNbt.decodeFaces(FaceTrimsNbt.encode(trims, 1)), trims.toString());
            }
        }
    }

    @Test
    void anUnusableFaceLookIsAPresentOakTrim() {
        CompoundTag payload = new CompoundTag();
        CompoundTag face = new CompoundTag();
        CompoundTag look = new CompoundTag();
        look.putString("Kind", "plastic");
        face.put(AppearanceNbt.TAG, look);
        payload.put("Back", face);
        payload.putString("Front", "not a compound");
        assertEquals(new FaceTrims(Optional.empty(), Optional.of(Appearance.DEFAULT)), FaceTrimsNbt.decodeFaces(payload));
        CompoundTag noVersion = new CompoundTag();
        noVersion.putString("Version", "2");
        assertTrue(FaceTrimsNbt.version(noVersion).isEmpty(), "a string version is no version");
    }
}
