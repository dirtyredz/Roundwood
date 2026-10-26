package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimsNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.Optional;

/** Strict versioned frame persistence, separate from door look and door casing stores. */
public final class DoorArchStore {
    public static final String TAG = "DoorArch";
    private DoorArchAttachment attachment;
    private Tag unknown;

    public Optional<DoorArchAttachment> attachment() { return Optional.ofNullable(attachment); }
    public boolean editable() { return unknown == null; }
    public boolean set(DoorArchAttachment next) {
        if (!editable()) return false;
        attachment = next;
        return true;
    }
    public void load(CompoundTag parent) {
        attachment = null; unknown = null;
        Tag raw = parent.get(TAG);
        if (raw == null) return;
        if (raw instanceof CompoundTag payload) {
            if (payload.equals(empty())) return;
            try {
                var next = new DoorArchAttachment(ArchPieceDesign.valueOf(payload.getString("Product")),
                        ArchPieceThickness.valueOf(payload.getString("Thickness")), ArchPieceDepth.valueOf(payload.getString("Depth")),
                        AppearanceNbt.decode(payload.getCompound("Appearance")).orElseThrow(),
                        FaceTrimsNbt.decodeFaces(payload.getCompound("Casings")));
                if (encode(next).equals(payload)) { attachment = next; return; }
            } catch (IllegalArgumentException | java.util.NoSuchElementException ignored) { }
        }
        unknown = raw.copy();
    }
    public void save(CompoundTag parent) {
        if (unknown != null) parent.put(TAG, unknown.copy());
        else if (attachment != null) parent.put(TAG, encode(attachment));
        else parent.remove(TAG);
    }
    public void writeUpdate(CompoundTag parent) {
        var payload = attachment == null ? empty() : encode(attachment);
        if (unknown != null) payload.putBoolean("Locked", true);
        parent.put(TAG, payload);
    }
    private static CompoundTag empty() { var tag = new CompoundTag(); tag.putInt("Version", 1); return tag; }
    public static CompoundTag encode(DoorArchAttachment attachment) {
        var tag = empty();
        tag.putString("Product", attachment.product().name());
        tag.putString("Thickness", attachment.thickness().name());
        tag.putString("Depth", attachment.depth().name());
        tag.put("Appearance", AppearanceNbt.encode(attachment.appearance()));
        tag.put("Casings", FaceTrimsNbt.encode(attachment.casings(), 1));
        return tag;
    }
}
