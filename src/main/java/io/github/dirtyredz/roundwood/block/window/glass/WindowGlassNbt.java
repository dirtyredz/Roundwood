package io.github.dirtyredz.roundwood.block.window.glass;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A window's glass colour (0.8.0), saved apart from the frame's appearance in block entities and
 * item stacks alike: {@code WindowGlass: {Version: 1, Color: "<dye name>"}}. Missing means clear (the
 * crafted and default state, never written). One colour covers the whole window, its connector
 * glass included. Reading never fails: an unknown version, a missing or unknown colour reads as clear.
 */
public final class WindowGlassNbt {
    public static final String TAG = "WindowGlass";
    public static final int VERSION = 1;
    private static final String VERSION_KEY = "Version";
    private static final String COLOR = "Color";

    private WindowGlassNbt() {
    }

    /** Puts {@code color} under {@link #TAG} in {@code parent}, or removes the tag for clear glass. */
    public static void write(CompoundTag parent, Optional<DyeColor> color) {
        if (color.isEmpty()) {
            parent.remove(TAG);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, VERSION);
        tag.putString(COLOR, color.get().getName());
        parent.put(TAG, tag);
    }

    public static Optional<DyeColor> read(@Nullable CompoundTag parent) {
        if (parent == null || !parent.contains(TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag tag = parent.getCompound(TAG);
        if (tag.getInt(VERSION_KEY) != VERSION || !tag.contains(COLOR, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.ofNullable(DyeColor.byName(tag.getString(COLOR), null));
    }
}
