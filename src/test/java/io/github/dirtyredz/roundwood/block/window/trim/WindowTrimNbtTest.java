package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The saved window trim (0.7.0): nested under its own versioned tag so the pane's top-level look keeps
 * its meaning, every trim look round-trips, and anything missing or unusable reads as nothing.
 */
class WindowTrimNbtTest {
    private static final Appearance BRICK = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
    private static final Appearance SPRUCE = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));

    @Test
    void everyTrimLookRoundTripsUnderItsOwnVersionedTag() {
        for (Appearance look : new Appearance[] {Appearance.DEFAULT, SPRUCE, SPRUCE.painted(DyeColor.RED), BRICK}) {
            CompoundTag tag = new CompoundTag();
            WindowTrimNbt.write(tag, look);
            assertEquals(Optional.of(look), WindowTrimNbt.read(tag), look.key());
            assertEquals(WindowTrimNbt.VERSION, tag.getCompound(WindowTrimNbt.TAG).getInt("Version"));
            assertFalse(tag.contains(AppearanceNbt.TAG), "the pane's own tag is untouched");
        }
    }

    @Test
    void thePaneAndTheTrimAreIndependent() {
        CompoundTag tag = new CompoundTag();
        AppearanceNbt.write(tag, SPRUCE);
        WindowTrimNbt.write(tag, BRICK);
        assertEquals(Optional.of(SPRUCE), AppearanceNbt.read(tag));
        assertEquals(Optional.of(BRICK), WindowTrimNbt.read(tag));
    }

    @Test
    void missingOrUnusableTrimDataReadsAsNothing() {
        assertTrue(WindowTrimNbt.read(new CompoundTag()).isEmpty());
        assertTrue(WindowTrimNbt.read(null).isEmpty());
        CompoundTag broken = new CompoundTag();
        CompoundTag inner = new CompoundTag();
        inner.putString("Kind", "plastic");
        broken.put(WindowTrimNbt.TAG, inner);
        assertTrue(WindowTrimNbt.read(broken).isEmpty());
        CompoundTag notCompound = new CompoundTag();
        notCompound.putString(WindowTrimNbt.TAG, "bricks");
        assertTrue(WindowTrimNbt.read(notCompound).isEmpty());
    }
}
