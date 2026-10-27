package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorArchStoreTest {
    @Test void knownDataRoundTripsAndExplicitEmptyUpdatesClearIt() {
        for (var depth : ArchPieceDepth.values()) {
            var attachment = new DoorArchAttachment(ArchPieceDesign.TOP_1X2, ArchPieceThickness.THIN, depth, Appearance.DEFAULT, FaceTrims.both(Appearance.DEFAULT));
            var store = new DoorArchStore(); assertTrue(store.set(attachment));
            var saved = new CompoundTag(); store.save(saved);
            var loaded = new DoorArchStore(); loaded.load(saved);
            assertEquals(attachment, loaded.attachment().orElseThrow());
            var clear = new CompoundTag(); new DoorArchStore().writeUpdate(clear);
            loaded.load(clear); assertTrue(loaded.attachment().isEmpty()); assertTrue(loaded.editable());
            loaded.load(saved); loaded.load(new CompoundTag()); assertTrue(loaded.attachment().isEmpty());
        }
    }
    @Test void futureMalformedAndExtraFieldsRemainInCustody() {
        var attachment = new DoorArchAttachment(ArchPieceDesign.TOP_1X1, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, Appearance.DEFAULT, FaceTrims.NONE);
        var future = DoorArchStore.encode(attachment); future.putInt("Version", 99);
        var extra = DoorArchStore.encode(attachment); extra.putString("FutureField", "opaque");
        for (var raw : new net.minecraft.nbt.Tag[]{future, extra, StringTag.valueOf("opaque")}) {
            var saved = new CompoundTag(); saved.put(DoorArchStore.TAG, raw);
            var store = new DoorArchStore(); store.load(saved);
            assertFalse(store.editable()); assertFalse(store.set(attachment)); assertTrue(store.attachment().isEmpty());
            var after = new CompoundTag(); store.save(after); assertEquals(saved, after);
            var update = new CompoundTag(); store.writeUpdate(update);
            assertFalse(update.getCompound(DoorArchStore.TAG).contains("Product"));
        }
    }
}
