package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class DoorHardwareStoreTest {
    private static final Appearance IRON=Appearance.surface(ResourceLocation.fromNamespaceAndPath("minecraft","iron_block"));
    @Test void independentSlotsRoundTripAndExplicitUpdateClearsPreviousValues(){
        var store=new DoorHardwareStore();assertEquals(DoorHardware.DEFAULT,store.value());
        var hinges=DoorHardware.DEFAULT.with(DoorHardwareSlot.HINGES,Optional.of(IRON));assertTrue(store.set(hinges));
        var parent=new CompoundTag();store.save(parent);var copy=new DoorHardwareStore();copy.load(parent);
        assertEquals(hinges,copy.value());assertTrue(copy.value().knobs().isEmpty());
        store.set(DoorHardware.DEFAULT);store.writeUpdate(parent);copy.load(parent);
        assertEquals(DoorHardware.DEFAULT,copy.value());assertTrue(copy.editable());
        store.save(parent);assertFalse(parent.contains(DoorHardwareStore.TAG));
    }
    @Test void unknownVersionsFieldsKindsAndWrongTypesRemainOpaqueAndRejectEdits(){
        var future=DoorHardwareStore.encode(DoorHardware.DEFAULT);future.putInt("Version",9);
        var extra=DoorHardwareStore.encode(DoorHardware.DEFAULT);extra.putBoolean("Future",true);
        var nested=DoorHardwareStore.encode(DoorHardware.DEFAULT.with(DoorHardwareSlot.KNOBS,Optional.of(IRON)));
        nested.getCompound("KNOBS").putString("Future","keep");
        var kind=nested.copy();kind.getCompound("KNOBS").putString("Kind","future");
        for(Tag raw:new Tag[]{future,extra,nested,kind,StringTag.valueOf("opaque"),new CompoundTag()}){
            var parent=new CompoundTag();parent.put(DoorHardwareStore.TAG,raw.copy());
            var store=new DoorHardwareStore();store.load(parent);
            assertFalse(store.editable());assertFalse(store.set(DoorHardware.DEFAULT));
            var disk=new CompoundTag();store.save(disk);assertEquals(raw,disk.get(DoorHardwareStore.TAG));
            var update=new CompoundTag();store.writeUpdate(update);var client=new DoorHardwareStore();client.load(update);
            assertFalse(client.editable());assertEquals(DoorHardware.DEFAULT,client.value());
        }
    }
    @Test void loadingMissingDataReplacesAFormerFinish(){
        var store=new DoorHardwareStore();store.set(DoorHardware.DEFAULT.with(DoorHardwareSlot.KNOBS,Optional.of(IRON)));
        store.load(new CompoundTag());assertEquals(DoorHardware.DEFAULT,store.value());
    }
}
