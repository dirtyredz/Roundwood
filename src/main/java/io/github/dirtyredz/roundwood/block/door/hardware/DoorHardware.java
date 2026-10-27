package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import java.util.Optional;

/** Missing slot means original brass; this is independent of the door panel's default oak. */
public record DoorHardware(Optional<Appearance> hinges,Optional<Appearance> knobs) {
    public static final DoorHardware DEFAULT=new DoorHardware(Optional.empty(),Optional.empty());
    public Optional<Appearance> get(DoorHardwareSlot slot){return slot==DoorHardwareSlot.HINGES?hinges:knobs;}
    public DoorHardware with(DoorHardwareSlot slot,Optional<Appearance> look){
        return slot==DoorHardwareSlot.HINGES?new DoorHardware(look,knobs):new DoorHardware(hinges,look);
    }
}
