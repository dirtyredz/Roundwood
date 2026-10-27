package io.github.dirtyredz.roundwood.block.door.interaction;

import io.github.dirtyredz.roundwood.block.door.hardware.*;
import io.github.dirtyredz.roundwood.block.door.leaf.*;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DoorHardwareTargetTest {
    @Test void nearestHardwareIsNotEditableThroughANearerSurface(){
        var part=new DoorHardwareGeometry.Part(DoorHardwareSlot.KNOBS,new Box(0,0,0,2,2,2));
        var from=new Vec3(1,1,10);var to=new Vec3(1,1,-1);
        assertFalse(DoorHardwareTarget.nearest(List.of(part),List.of(),from,to).orElseThrow().blocked());
        assertTrue(DoorHardwareTarget.nearest(List.of(part),List.of(new Box(0,0,3,2,2,4)),from,to).orElseThrow().blocked());
        assertFalse(DoorHardwareTarget.nearest(List.of(part),List.of(new Box(0,0,-3,2,2,-2)),from,to).orElseThrow().blocked());
    }
    @Test void bothRestingEndpointsAndMirroredLeavesCarryBothSlots(){
        for(var design:DoorDesign.values())for(var hinge:DoorHingeSide.values())for(boolean open:new boolean[]{false,true}){
            var parts=DoorHardwareGeometry.placed(design,hinge,open);
            for(var slot:DoorHardwareSlot.values())assertTrue(parts.stream().anyMatch(p->p.slot()==slot));
            assertEquals(design.doubleLeaf()?4:2,parts.stream().filter(p->p.slot()==DoorHardwareSlot.KNOBS).count());
        }
    }
}
