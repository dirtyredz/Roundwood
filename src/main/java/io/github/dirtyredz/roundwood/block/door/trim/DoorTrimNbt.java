package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrimsNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The saved form of a door's face trims (0.8.0), on the anchor's entity beside its appearance and
 * camouflage: {@code DoorTrim: {Version: 1, Front: {...}, Back: {...}}} ({@link FaceTrimsNbt}).
 * Missing means no trims (0.7.0 doors had none; their fitted frames are migrated separately). A
 * payload of an unknown version reads as no trims and is kept to be saved back unchanged (the door
 * refuses trim edits meanwhile).
 */
public final class DoorTrimNbt {
    public static final String TAG = "DoorTrim";
    public static final int VERSION = 1;

    /** What was read: the faces, and a payload of an unknown version to keep (null when understood). */
    public record Read(FaceTrims trims, @Nullable CompoundTag unknown) {
    }

    private DoorTrimNbt() {
    }

    public static void write(CompoundTag parent, FaceTrims trims) {
        parent.put(TAG, FaceTrimsNbt.encode(trims, VERSION));
    }

    public static Read read(@Nullable CompoundTag parent) {
        if (parent == null || !parent.contains(TAG, Tag.TAG_COMPOUND)) {
            return new Read(FaceTrims.NONE, null);
        }
        CompoundTag payload = parent.getCompound(TAG);
        Optional<Integer> version = FaceTrimsNbt.version(payload);
        if (version.isPresent() && version.get() == VERSION) {
            return new Read(FaceTrimsNbt.decodeFaces(payload), null);
        }
        return new Read(FaceTrims.NONE, payload.copy());
    }
}
