package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.block.door.leaf.*;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorHardwareSlotsTest {
    @Test void hingesAndKnobsRetainDistinctMeshIdentitiesBeforeBaking() {
        for(var design:DoorDesign.values()) for(var hinge:DoorHingeSide.values()) {
            var pieces=DoorGeometry.leaf(design,hinge);
            assertTrue(pieces.stream().anyMatch(p->p.texture().equals("hinges")),"hinges share the anonymous brass group");
            assertTrue(pieces.stream().anyMatch(p->p.texture().equals("knobs")),"knobs share the anonymous brass group");
        }
    }
}
