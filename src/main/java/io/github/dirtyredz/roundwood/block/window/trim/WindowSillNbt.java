package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimsNbt;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * The saved form of a window's arch sills (0.10.0, {@code TrimSlot.SILL}), its own payload beside the
 * perimeter trims' {@code WindowTrim}: {@code WindowSillTrim: {Version: 1, Front: {...}, Back: {...}}}
 * ({@link FaceTrimsNbt}), kept by a second {@link FaceTrimStore} so an unknown sill payload never
 * blocks the perimeter trims (and the other way round). There is no older layout: a missing payload is
 * no sills, and nothing is written while neither face has one. Any other version is not understood:
 * it reads as no sills, is saved back unchanged, and sill edits are refused.
 */
public final class WindowSillNbt {
    public static final String TAG = "WindowSillTrim";
    public static final int VERSION = 1;
    public static final FaceTrimStore.Format FORMAT = new FaceTrimStore.Format(TAG, VERSION, false);

    private WindowSillNbt() {
    }

    public static void write(CompoundTag parent, FaceTrims sills) {
        FaceTrimStore.write(FORMAT, parent, sills);
    }

    public static FaceTrimStore.Read read(@Nullable CompoundTag parent) {
        return FaceTrimStore.read(FORMAT, parent, FaceTrimStore.Legacy.NONE);
    }
}
