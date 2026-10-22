package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** One whole-pane cross, stored at Front. Back and every unsupported payload are held unchanged. */
public final class WindowBeamNbt {
    public static final String TAG = "WindowCrossBeams";
    public static final FaceTrimStore.Format FORMAT = new FaceTrimStore.Format(TAG, 1, false, TrimSlot.Scope.WHOLE_HOST);

    private WindowBeamNbt() { }

    public static FaceTrimStore.Read read(@Nullable CompoundTag parent) {
        return FaceTrimStore.read(FORMAT, parent, FaceTrimStore.Legacy.NONE);
    }

    public static void write(CompoundTag parent, FaceTrims beams) {
        FaceTrimStore.write(FORMAT, parent, beams);
    }
}
