package io.github.dirtyredz.roundwood.block.window.placement;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pose resolution from a targeted zone, the player's heading and eye: what the server places and the client previews. */
class WindowPlacementTest {
    private static WindowPlacement.Request request(Direction face, WindowTargetZones.Zone zone, Direction facing, Vec3 eye) {
        return new WindowPlacement.Request(BlockPos.ZERO, BlockPos.ZERO.below(), face, zone, facing, eye);
    }

    @Test
    void roundWindowCentreFacesThePlayerAndEdgesAreFlush() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            WindowPlacement.Request centre = request(Direction.UP, WindowTargetZones.Zone.CENTER, facing, null);
            assertEquals(WindowPosition.CENTER, WindowPlacement.roundPosition(centre));
            // The 0.5.0 rule: the plane runs across the line of sight.
            assertEquals(facing.getClockWise().getAxis(), WindowPlacement.roundAxis(centre), facing.toString());
        }
        assertEquals(WindowPosition.NORTH, WindowPlacement.roundPosition(
                request(Direction.UP, WindowTargetZones.Zone.V_MIN, Direction.SOUTH, null)));
        assertEquals(WindowPosition.EAST, WindowPlacement.roundPosition(
                request(Direction.UP, WindowTargetZones.Zone.U_MAX, Direction.SOUTH, null)));
        assertEquals(WindowPosition.UP, WindowPlacement.roundPosition(
                request(Direction.SOUTH, WindowTargetZones.Zone.V_MAX, Direction.NORTH, null)));
        assertEquals(WindowPosition.DOWN, WindowPlacement.roundPosition(
                request(Direction.WEST, WindowTargetZones.Zone.V_MIN, Direction.EAST, null)));
        assertEquals(WindowPosition.SOUTH, WindowPlacement.roundPosition(
                request(Direction.EAST, WindowTargetZones.Zone.U_MAX, Direction.WEST, null)));
    }

    @Test
    void roundPositionsResolveToFlushPoses() {
        assertEquals(WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER), WindowPosition.CENTER.pose(Direction.Axis.X));
        assertEquals(WindowPose.upright(Direction.WEST, WindowDepth.CENTER), WindowPosition.CENTER.pose(Direction.Axis.Z));
        for (WindowPosition position : WindowPosition.values()) {
            if (position == WindowPosition.CENTER) {
                continue;
            }
            Direction side = position.side().orElseThrow();
            for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                WindowPose pose = position.pose(axis);
                assertEquals(Optional.of(side), pose.flushSide(), position + " " + axis);
                assertEquals(side.getAxis(), pose.normal(), position + " normal");
            }
        }
    }

    @Test
    void archCentreIsUprightFacingThePlayerSoLeftIsOnTheirLeft() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            WindowPose pose = WindowPlacement.archPose(Optional.empty(), facing, null);
            assertEquals(WindowPose.upright(facing.getOpposite(), WindowDepth.CENTER), pose);
            // The viewer's right, seen from the front, is the player's right.
            assertEquals(facing.getClockWise(), pose.right(), facing.toString());
        }
    }

    @Test
    void archOnAWallSideFacesTheEye() {
        // Flush against the west side (slab x 0..2): an eye east of it sees the east face.
        Vec3 east = new Vec3(0.8, 1.6, 0.5);
        Vec3 west = new Vec3(-0.5, 1.6, 0.5);
        assertEquals(WindowPose.upright(Direction.EAST, WindowDepth.NEGATIVE),
                WindowPlacement.archPose(Optional.of(Direction.WEST), Direction.NORTH, east));
        assertEquals(WindowPose.upright(Direction.WEST, WindowDepth.NEGATIVE),
                WindowPlacement.archPose(Optional.of(Direction.WEST), Direction.NORTH, west));
        // Flush south (slab z 14..16): eye at the slab's middle is a tie; the player looks north, so the front faces south.
        assertEquals(WindowPose.upright(Direction.SOUTH, WindowDepth.POSITIVE),
                WindowPlacement.archPose(Optional.of(Direction.SOUTH), Direction.NORTH, new Vec3(0.5, 1.6, 15 / 16.0)));
        // Without a player the front points into the cell.
        assertEquals(WindowPose.upright(Direction.EAST, WindowDepth.NEGATIVE),
                WindowPlacement.archPose(Optional.of(Direction.WEST), Direction.NORTH, null));
    }

    @Test
    void floorAndCeilingArchesTakeTheTopThePlayerSeesAsUp() {
        Vec3 above = new Vec3(0.5, 1.6, 0.5);
        Vec3 below = new Vec3(0.5, -1.0, 0.5);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            WindowPose floor = WindowPlacement.archPose(Optional.of(Direction.DOWN), facing, above);
            assertEquals(new WindowPose(Direction.UP, facing, WindowDepth.NEGATIVE), floor, "floor " + facing);
            assertEquals(facing.getClockWise(), floor.right(), "floor: left piece on the player's left " + facing);
            WindowPose ceiling = WindowPlacement.archPose(Optional.of(Direction.UP), facing, below);
            assertEquals(new WindowPose(Direction.DOWN, facing.getOpposite(), WindowDepth.POSITIVE), ceiling, "ceiling " + facing);
            assertEquals(facing.getClockWise(), ceiling.right(), "ceiling: left piece on the player's left " + facing);
        }
    }

    @Test
    void everyArchPoseHasAnOrientationValue() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (Direction edge : Direction.values()) {
                for (Vec3 eye : new Vec3[] {null, new Vec3(3, 3, 3), new Vec3(-3, -3, -3)}) {
                    WindowPose pose = WindowPlacement.archPose(Optional.of(edge), facing, eye);
                    assertEquals(pose, WindowOrientation.of(pose).pose(pose.depth()));
                }
            }
        }
    }
}
