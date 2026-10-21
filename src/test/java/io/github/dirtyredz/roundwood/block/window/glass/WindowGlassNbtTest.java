package io.github.dirtyredz.roundwood.block.window.glass;

import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The glass colour payload (0.8.0): every dye round-trips under its own versioned tag, clear glass is
 * never written (and writing clear removes a tint), and malformed or future data reads as clear.
 */
class WindowGlassNbtTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    @Test
    void everyDyeRoundTrips() {
        for (DyeColor dye : DyeColor.values()) {
            CompoundTag tag = new CompoundTag();
            WindowGlassNbt.write(tag, Optional.of(dye));
            assertEquals(1, tag.getCompound(WindowGlassNbt.TAG).getInt("Version"));
            assertEquals(dye.getName(), tag.getCompound(WindowGlassNbt.TAG).getString("Color"));
            assertEquals(Optional.of(dye), WindowGlassNbt.read(tag), dye.getName());
        }
    }

    @Test
    void clearIsNeverStored() {
        CompoundTag tag = new CompoundTag();
        WindowGlassNbt.write(tag, Optional.empty());
        assertTrue(tag.isEmpty());
        WindowGlassNbt.write(tag, Optional.of(DyeColor.RED));
        WindowGlassNbt.write(tag, Optional.empty());
        assertFalse(tag.contains(WindowGlassNbt.TAG), "clearing removes the tint");
        assertTrue(WindowGlassNbt.read(tag).isEmpty());
        assertTrue(WindowGlassNbt.read(null).isEmpty());
    }

    @Test
    void malformedOrFutureDataReadsAsClear() {
        CompoundTag unknownColour = glass(1, "chartreuse");
        CompoundTag future = glass(2, "red");
        CompoundTag noColour = new CompoundTag();
        CompoundTag inner = new CompoundTag();
        inner.putInt("Version", 1);
        noColour.put(WindowGlassNbt.TAG, inner);
        CompoundTag notCompound = new CompoundTag();
        notCompound.putString(WindowGlassNbt.TAG, "red");
        for (CompoundTag tag : new CompoundTag[] {unknownColour, future, noColour, notCompound}) {
            assertTrue(WindowGlassNbt.read(tag).isEmpty(), tag.toString());
        }
    }

    private static CompoundTag glass(int version, String colour) {
        CompoundTag parent = new CompoundTag();
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", version);
        tag.putString("Color", colour);
        parent.put(WindowGlassNbt.TAG, tag);
        return parent;
    }
}
