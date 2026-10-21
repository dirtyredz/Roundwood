package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/**
 * The saved form of a fitted window trim's look, nested so the pane's own top-level
 * {@code RoundwoodAppearance} keeps its meaning: {@code WindowTrim: {Version: 1, RoundwoodAppearance:
 * {...}}}. Reading never fails: a missing compound or an unusable look reads as nothing (an oak trim
 * when the window is trimmed); any trim look (wood, paint or masonry) is accepted.
 */
public final class WindowTrimNbt {
    public static final String TAG = "WindowTrim";
    public static final int VERSION = 1;
    private static final String VERSION_KEY = "Version";

    private WindowTrimNbt() {
    }

    public static void write(CompoundTag parent, Appearance trim) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, VERSION);
        AppearanceNbt.write(tag, trim);
        parent.put(TAG, tag);
    }

    public static Optional<Appearance> read(CompoundTag parent) {
        if (parent == null || !parent.contains(TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return AppearanceNbt.read(parent.getCompound(TAG)).filter(AppearanceFamily.TRIM::accepts);
    }
}
