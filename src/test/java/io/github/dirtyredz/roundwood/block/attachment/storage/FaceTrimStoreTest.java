package io.github.dirtyredz.roundwood.block.attachment.storage;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimNbt;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimNbt;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared face-trim holder (0.9.0) against the two hosts' real formats: the door omits an empty
 * payload and the window writes it; explicit empty window data beats a stale summary; the 0.7.0
 * window adapter still uplifts; an unknown version is held raw, saved back byte-for-byte, refuses
 * edits and is sent to clients as explicit empty; a known update replaces whatever was held; two
 * slots on one parent never see each other's payload.
 */
class FaceTrimStoreTest {
    static {
        MinecraftBootstrap.ensure();
    }

    private static final Appearance BRICK = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
    private static final Appearance SPRUCE = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));
    private static final FaceTrims BACK_BRICK = FaceTrims.NONE.with(AttachmentFace.BACK, BRICK);

    private static CompoundTag future(String tag) {
        CompoundTag parent = new CompoundTag();
        CompoundTag payload = new CompoundTag();
        payload.putInt("Version", 99);
        payload.putString("Shape", "gothic");
        payload.put("Front", new CompoundTag());
        parent.put(tag, payload);
        return parent;
    }

    @Test
    void hostsKeepTheirOwnAbsenceRules() {
        FaceTrimStore door = new FaceTrimStore(DoorTrimNbt.FORMAT);
        FaceTrimStore window = new FaceTrimStore(WindowTrimNbt.FORMAT);
        CompoundTag doorTag = new CompoundTag();
        CompoundTag windowTag = new CompoundTag();
        door.save(doorTag);
        window.save(windowTag);
        assertFalse(doorTag.contains(DoorTrimNbt.TAG), "an untrimmed door writes nothing");
        assertTrue(windowTag.contains(WindowTrimNbt.TAG), "an untrimmed window writes explicit empty");
        assertEquals(2, windowTag.getCompound(WindowTrimNbt.TAG).getInt("Version"));
        assertTrue(door.set(BACK_BRICK) && window.set(BACK_BRICK));
        door.save(doorTag);
        window.save(windowTag);
        assertEquals(1, doorTag.getCompound(DoorTrimNbt.TAG).getInt("Version"));
        FaceTrimStore doorCopy = new FaceTrimStore(DoorTrimNbt.FORMAT);
        doorCopy.load(doorTag, FaceTrimStore.Legacy.NONE);
        FaceTrimStore windowCopy = new FaceTrimStore(WindowTrimNbt.FORMAT);
        windowCopy.load(windowTag, WindowTrimNbt.legacy(false));
        assertEquals(BACK_BRICK, doorCopy.trims());
        assertEquals(BACK_BRICK, windowCopy.trims());
    }

    /** A missing door payload is no trims; a missing window payload is the 0.7.0 omitted-oak case only when trimmed. */
    @Test
    void legacyWindowUpliftAndExplicitEmpty() {
        FaceTrimStore window = new FaceTrimStore(WindowTrimNbt.FORMAT);
        window.load(new CompoundTag(), WindowTrimNbt.legacy(true));
        assertEquals(FaceTrims.both(Appearance.DEFAULT), window.trims());
        assertTrue(window.editable());
        window.load(new CompoundTag(), WindowTrimNbt.legacy(false));
        assertEquals(FaceTrims.NONE, window.trims());
        CompoundTag empty = new CompoundTag();
        WindowTrimNbt.write(empty, FaceTrims.NONE);
        window.load(empty, WindowTrimNbt.legacy(true));
        assertEquals(FaceTrims.NONE, window.trims(), "explicit version-2 empty overrides a stale trimmed summary");
        FaceTrimStore door = new FaceTrimStore(DoorTrimNbt.FORMAT);
        door.load(new CompoundTag(), FaceTrimStore.Legacy.NONE);
        assertEquals(FaceTrims.NONE, door.trims());
        assertTrue(door.editable());
    }

    @Test
    void anUnknownVersionIsHeldRawRefusesEditsAndIsSavedBackUnchanged() {
        for (FaceTrimStore.Format format : new FaceTrimStore.Format[] {DoorTrimNbt.FORMAT, WindowTrimNbt.FORMAT}) {
            CompoundTag loaded = future(format.tag());
            CompoundTag original = loaded.copy();
            FaceTrimStore store = new FaceTrimStore(format);
            store.load(loaded, format == WindowTrimNbt.FORMAT ? WindowTrimNbt.legacy(true) : FaceTrimStore.Legacy.NONE);
            assertEquals(FaceTrims.NONE, store.trims(), format.tag());
            assertFalse(store.editable(), format.tag());
            assertFalse(store.set(BACK_BRICK), format.tag() + ": an edit was accepted");
            loaded.getCompound(format.tag()).putString("Shape", "changed after loading");
            CompoundTag saved = new CompoundTag();
            store.save(saved);
            assertEquals(original, saved, format.tag() + ": not saved back byte-for-byte");
        }
    }

    /** Clients never see an unknown payload: they get explicit empty, and a later known update replaces it. */
    @Test
    void networkUpdatesClearAndReplace() {
        FaceTrimStore server = new FaceTrimStore(DoorTrimNbt.FORMAT);
        server.load(future(DoorTrimNbt.TAG), FaceTrimStore.Legacy.NONE);
        CompoundTag update = new CompoundTag();
        server.writeUpdate(update);
        assertEquals(1, update.getCompound(DoorTrimNbt.TAG).getInt("Version"));
        assertFalse(update.getCompound(DoorTrimNbt.TAG).contains("Front"), "the unknown payload leaked to clients");
        FaceTrimStore client = new FaceTrimStore(DoorTrimNbt.FORMAT);
        client.set(BACK_BRICK);
        client.load(update, FaceTrimStore.Legacy.NONE);
        assertEquals(FaceTrims.NONE, client.trims(), "an explicit empty update clears the client");
        assertTrue(client.editable());
        FaceTrimStore known = new FaceTrimStore(DoorTrimNbt.FORMAT);
        known.set(FaceTrims.both(SPRUCE));
        CompoundTag knownUpdate = new CompoundTag();
        known.writeUpdate(knownUpdate);
        client.load(knownUpdate, FaceTrimStore.Legacy.NONE);
        assertEquals(FaceTrims.both(SPRUCE), client.trims());
        CompoundTag removal = new CompoundTag();
        new FaceTrimStore(DoorTrimNbt.FORMAT).writeUpdate(removal);
        assertTrue(removal.contains(DoorTrimNbt.TAG), "an empty door still sends its payload so a removal syncs");
    }

    /** Two slots on one host: separate keys, so one slot's unknown payload never blocks or leaks into the other. */
    @Test
    void slotsSharingAParentAreIndependent() {
        FaceTrimStore.Format sill = new FaceTrimStore.Format("WindowSillTrim", 1, false);
        CompoundTag parent = future("WindowSillTrim");
        WindowTrimNbt.write(parent, BACK_BRICK);
        FaceTrimStore perimeter = new FaceTrimStore(WindowTrimNbt.FORMAT);
        FaceTrimStore sills = new FaceTrimStore(sill);
        perimeter.load(parent, WindowTrimNbt.legacy(true));
        sills.load(parent, FaceTrimStore.Legacy.NONE);
        assertEquals(BACK_BRICK, perimeter.trims());
        assertTrue(perimeter.editable(), "the other slot's unknown payload blocked the perimeter");
        assertFalse(sills.editable());
        assertTrue(perimeter.set(FaceTrims.both(SPRUCE)));
        CompoundTag saved = new CompoundTag();
        perimeter.save(saved);
        sills.save(saved);
        assertEquals(parent.getCompound("WindowSillTrim"), saved.getCompound("WindowSillTrim"));
        assertEquals(Optional.of(SPRUCE), FaceTrimStore.read(WindowTrimNbt.FORMAT, saved, WindowTrimNbt.legacy(false)).trims()
                .get(AttachmentFace.FRONT));
    }
}
