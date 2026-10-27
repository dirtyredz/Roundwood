package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorArchFitPlanTest {
    @Test void bothOrdersPreserveTheAimedPose() {
        for (var design : DoorDesign.values()) for (var facing : Direction.Plane.HORIZONTAL) for (var hinge : DoorHingeSide.values()) for (var depth : DoorDepth.values()) {
            var door = new DoorLayout.Assembly(new BlockPos(12, 5, -7), facing, hinge, design, depth);
            var fitted = DoorArchFitPlan.fromDoor(door, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, false).orElseThrow();
            var expected = design == DoorDesign.ARCHED ? ArchPieceDesign.TOP_1X1 : design == DoorDesign.ROUND ? ArchPieceDesign.TOP_1X2 : ArchPieceDesign.TOP_2X3;
            assertEquals(expected, fitted.arch().design());
            assertEquals(door.anchor().above(design == DoorDesign.TALL_DOUBLE_ARCHED ? 2 : 1), fitted.arch().anchor());
            assertEquals(depth, fitted.door().depth());
            assertEquals(fitted, DoorArchFitPlan.fromArch(design, fitted.arch(), hinge, depth).orElseThrow());
            assertEquals(fitted, DoorArchFitPlan.of(door, fitted.arch(), true).orElseThrow());
            assertTrue(DoorArchFitPlan.of(door, new ArchPieceLayout.Assembly(fitted.arch().anchor().above(), facing, expected, Side.LEFT), false).isEmpty());
            assertTrue(DoorArchFitPlan.of(door, new ArchPieceLayout.Assembly(fitted.arch().anchor(), facing.getOpposite(), expected, Side.LEFT), false).isEmpty());
            assertTrue(DoorArchFitPlan.of(door, new ArchPieceLayout.Assembly(fitted.arch().anchor(), facing, ArchPieceDesign.KEYHOLE_SHOULDER, Side.LEFT), false).isEmpty());
            for (var thin : ArchPieceDepth.values()) assertEquals(thin.name().equals(depth.name()),
                    DoorArchFitPlan.fromDoor(door, ArchPieceThickness.THIN, thin, false).isEmpty());
        }
    }
}
