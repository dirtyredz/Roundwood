package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WindowBeamNbtTest {
    static { MinecraftBootstrap.ensure(); }

    @Test
    void bothSidesAddressOneRecordAndSettersRejectBack() {
        assertEquals(List.of(AttachmentFace.FRONT), TrimSlot.CROSS_BEAMS.addresses());
        for (AttachmentFace side : AttachmentFace.values()) {
            assertEquals(AttachmentFace.FRONT, TrimSlot.CROSS_BEAMS.address(side));
        }
        var store = new FaceTrimStore(WindowBeamNbt.FORMAT);
        assertFalse(store.set(FaceTrims.both(Appearance.DEFAULT)));
        assertTrue(store.set(FaceTrims.NONE.with(AttachmentFace.FRONT, Appearance.DEFAULT)));
        assertFalse(store.set(FaceTrims.NONE.with(AttachmentFace.BACK, Appearance.DEFAULT)));
        assertEquals(1, store.trims().count());
    }

    @Test
    void malformedAndFuturePayloadsRemainInCustody() {
        var bad = new java.util.ArrayList<net.minecraft.nbt.Tag>();
        bad.add(StringTag.valueOf("future data"));
        for (String field : List.of("Back", "Unknown", "Front")) {
            var payload = new CompoundTag();
            payload.putInt("Version", 1);
            payload.putString(field, "unrecognized");
            bad.add(payload);
        }
        var conflicting = new CompoundTag();
        conflicting.putInt("Version", 1);
        conflicting.put("Front", new CompoundTag());
        conflicting.put("Back", new CompoundTag());
        bad.add(conflicting);
        var badLook = new CompoundTag();
        io.github.dirtyredz.roundwood.appearance.AppearanceNbt.write(badLook, Appearance.DEFAULT);
        badLook.getCompound(io.github.dirtyredz.roundwood.appearance.AppearanceNbt.TAG).putString("Future", "keep");
        var malformed = new CompoundTag();
        malformed.putInt("Version", 1);
        malformed.put("Front", badLook);
        bad.add(malformed);
        var wrongType = new CompoundTag();
        wrongType.putByte("Version", (byte) 1);
        bad.add(wrongType);
        var future = new CompoundTag();
        future.putInt("Version", 2);
        bad.add(future);
        for (var payload : bad) {
            var disk = new CompoundTag();
            disk.put(WindowBeamNbt.TAG, payload);
            var store = new FaceTrimStore(WindowBeamNbt.FORMAT);
            store.load(disk, FaceTrimStore.Legacy.NONE);
            assertFalse(store.editable());
            assertFalse(store.set(FaceTrims.NONE));
            var saved = new CompoundTag();
            store.save(saved);
            assertEquals(disk, saved);
            var packet = new CompoundTag();
            store.writeUpdate(packet);
            assertEquals(FaceTrims.NONE, WindowBeamNbt.read(packet).trims());
            assertNull(WindowBeamNbt.read(packet).unknown());
        }
    }

    @Test
    void beamOnlyDiskNeverInventsPerimeterTrims() {
        var disk = new CompoundTag();
        WindowBeamNbt.write(disk, FaceTrims.NONE.with(AttachmentFace.FRONT, Appearance.DEFAULT));
        WindowTrimNbt.write(disk, FaceTrims.NONE);
        assertEquals(1, WindowBeamNbt.read(disk).trims().count());
        assertEquals(FaceTrims.NONE, WindowTrimNbt.read(disk, true).trims());
    }
}
