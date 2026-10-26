package io.github.dirtyredz.roundwood.block.arch.assembly;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchPieceTargetingTest {
    @Test void overlayZonesSelectTheSameSideAsPlacementOnEveryFace() {
        for (var facing : Direction.Plane.HORIZONTAL) for (var face : Direction.values()) {
            if (face == facing.getClockWise() || face == facing.getCounterClockWise()) {
                var side = ArchPieceTargeting.side(facing, face, .5, .5);
                assertEquals(new ArchPieceTargeting.FaceZone(0, 0, 1, 1), ArchPieceTargeting.zone(facing, face, side));
                continue;
            }
            for (var side : new io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side[]{LEFT, RIGHT}) {
                var zone = ArchPieceTargeting.zone(facing, face, side);
                assertEquals(.5, (zone.maxU() - zone.minU()) * (zone.maxV() - zone.minV()));
                double u = (zone.minU() + zone.maxU()) / 2, v = (zone.minV() + zone.maxV()) / 2;
                double x = face.getAxis() == Direction.Axis.X ? .5 : u;
                double z = face.getAxis() == Direction.Axis.Y ? v : face.getAxis() == Direction.Axis.X ? u : .5;
                assertEquals(side, ArchPieceTargeting.side(facing, face, x, z));
            }
        }
    }
    @Test
    void frontBackUpDownShareTheSameSplitInEveryFacing() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Direction right = facing.getClockWise();
            for (Direction face : new Direction[]{facing, facing.getOpposite(), Direction.UP, Direction.DOWN}) {
                for (double along : new double[]{0, .25, .5, .75, 1}) {
                    double x = .5 + right.getStepX() * (along - .5), z = .5 + right.getStepZ() * (along - .5);
                    assertEquals(along, ArchPieceTargeting.across(facing, x, z));
                    assertEquals(along <= .5 ? LEFT : RIGHT, ArchPieceTargeting.side(facing, face, x, z));
                }
                // Exact tie and immediately adjacent binary-exact positions.
                double delta = Math.scalb(1.0, -20);
                assertEquals(LEFT, ArchPieceTargeting.side(facing, face, .5, .5));
                assertEquals(RIGHT, ArchPieceTargeting.side(facing, face, .5 + right.getStepX() * delta, .5 + right.getStepZ() * delta));
                assertEquals(LEFT, ArchPieceTargeting.side(facing, face, .5 - right.getStepX() * delta, .5 - right.getStepZ() * delta));
            }
        }
    }

    @Test
    void sideWallsSelectTheEdgeTouchingTheWallRegardlessOfHitHalf() {
        for (Direction facing : Direction.Plane.HORIZONTAL) for (double hit : new double[]{0, .5, 1}) {
            assertEquals(LEFT, ArchPieceTargeting.side(facing, facing.getClockWise(), hit, hit));
            assertEquals(RIGHT, ArchPieceTargeting.side(facing, facing.getCounterClockWise(), hit, hit));
        }
        assertThrows(IllegalArgumentException.class, () -> ArchPieceTargeting.across(Direction.UP, .5, .5));
        assertThrows(IllegalArgumentException.class, () -> ArchPieceTargeting.across(Direction.NORTH, Double.NaN, .5));
    }
}
