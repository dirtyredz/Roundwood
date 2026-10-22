package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.block.attachment.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrimsNbt;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * The saved form of a door's face trims (0.8.0), on the anchor's entity beside its appearance and
 * camouflage: {@code DoorTrim: {Version: 1, Front: {...}, Back: {...}}} ({@link FaceTrimsNbt}), kept
 * by a {@link FaceTrimStore}. Missing means no trims (0.7.0 doors had none; their fitted frames are
 * migrated separately), and an untrimmed door writes nothing. Any other version is unknown: kept and
 * saved back unchanged, the door refusing trim edits meanwhile.
 */
public final class DoorTrimNbt {
    public static final String TAG = "DoorTrim";
    public static final int VERSION = 1;
    public static final FaceTrimStore.Format FORMAT = new FaceTrimStore.Format(TAG, VERSION, false);

    private DoorTrimNbt() {
    }

    public static void write(CompoundTag parent, FaceTrims trims) {
        FaceTrimStore.write(FORMAT, parent, trims);
    }

    public static FaceTrimStore.Read read(@Nullable CompoundTag parent) {
        return FaceTrimStore.read(FORMAT, parent, FaceTrimStore.Legacy.NONE);
    }
}
