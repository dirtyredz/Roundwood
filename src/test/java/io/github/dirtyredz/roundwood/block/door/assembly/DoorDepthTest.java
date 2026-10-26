package io.github.dirtyredz.roundwood.block.door.assembly;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorDepthTest {
    @Test void faceBandsAndPlacementAgreeForEveryFacingAndClickedFace() {
        for (var facing : Direction.Plane.HORIZONTAL) for (var face : Direction.values()) for (var depth : DoorDepth.values()) {
            var zone = depth.zone(facing, face);
            double u = (zone.minU() + zone.maxU()) / 2, v = (zone.minV() + zone.maxV()) / 2;
            double x = face.getAxis() == Direction.Axis.X ? .5 : u;
            double y = face.getAxis() == Direction.Axis.Y ? .5 : v;
            double z = face.getAxis() == Direction.Axis.Y ? v : face.getAxis() == Direction.Axis.X ? u : .5;
            assertEquals(depth, DoorDepth.target(facing, face, x, y, z), facing + "/" + face);
            assertEquals(1.0 / 3, (zone.maxU() - zone.minU()) * (zone.maxV() - zone.minV()), 1e-9);
        }
    }
    @Test void boundaryTiesChooseMiddleAndOffsetsPreserveOldFront() {
        assertEquals(DoorDepth.MIDDLE, DoorDepth.target(Direction.NORTH, Direction.UP, .5, 0, 1.0 / 3));
        assertEquals(DoorDepth.MIDDLE, DoorDepth.target(Direction.NORTH, Direction.UP, .5, 0, 2.0 / 3));
        assertEquals(0, DoorDepth.FRONT.offset());
        assertEquals(-6.5, DoorDepth.MIDDLE.offset());
        assertEquals(-13, DoorDepth.BACK.offset());
        assertThrows(IllegalArgumentException.class, () -> DoorDepth.target(Direction.NORTH, Direction.UP, .5, 0, Double.NaN));
    }
}
