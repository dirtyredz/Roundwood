package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.appearance.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Strict complete payload, retaining malformed or future data without interpreting it. */
public final class DoorHardwareStore {
    public static final String TAG="DoorHardware";
    private DoorHardware value=DoorHardware.DEFAULT;
    private Tag unknown;
    public DoorHardware value(){return value;}
    public boolean editable(){return unknown==null;}
    public boolean set(DoorHardware next){if(!editable()) return false;value=next;return true;}
    public void load(CompoundTag parent){
        value=DoorHardware.DEFAULT;unknown=null;
        Tag raw=parent==null?null:parent.get(TAG);if(raw==null)return;
        if(raw instanceof CompoundTag payload) try {
            var next=DoorHardware.DEFAULT;
            for(var slot:DoorHardwareSlot.values()) if(payload.contains(slot.name())) {
                var look=AppearanceNbt.decode(payload.getCompound(slot.name())).orElseThrow();
                if(!AppearanceFamily.DOOR.accepts(look)) throw new IllegalArgumentException("Unsupported hardware look");
                next=next.with(slot,java.util.Optional.of(look));
            }
            if(encode(next).equals(payload)){value=next;return;}
        } catch(IllegalArgumentException | java.util.NoSuchElementException ignored) {}
        unknown=raw.copy();
    }
    public void save(CompoundTag parent){
        if(unknown!=null)parent.put(TAG,unknown.copy());
        else if(!value.equals(DoorHardware.DEFAULT))parent.put(TAG,encode(value));
        else parent.remove(TAG);
    }
    public void writeUpdate(CompoundTag parent){
        var payload=encode(value);
        if(unknown!=null)payload.putBoolean("Locked",true);
        parent.put(TAG,payload);
    }
    public static CompoundTag encode(DoorHardware value){
        var payload=new CompoundTag();payload.putInt("Version",1);
        for(var slot:DoorHardwareSlot.values()) value.get(slot).ifPresent(look->payload.put(slot.name(),AppearanceNbt.encode(look)));
        return payload;
    }
}
