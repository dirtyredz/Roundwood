package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CamouflageCoverageTest {
    @Test void selectionCacheDistinguishesFittedAndUnfittedCornersAtEveryDepth() {
        for (var hinge : DoorHingeSide.values()) for (var depth : DoorDepth.values()) {
            var part = DoorLayout.Part.of(DoorDesign.ROUND).get(0);
            var all = io.github.dirtyredz.roundwood.block.door.selection.DoorSelectionShapes.shape(
                    DoorDesign.ROUND, Direction.NORTH, hinge, part, false, true, 0, true, depth.offset(), CamouflageCoverage.ALL);
            var lower = io.github.dirtyredz.roundwood.block.door.selection.DoorSelectionShapes.shape(
                    DoorDesign.ROUND, Direction.NORTH, hinge, part, false, true, 0, true, depth.offset(), CamouflageCoverage.LOWER);
            assertNotEquals(all.toAabbs(), lower.toAabbs());
            for (double y : new double[]{.03125, 1.96875}) {
                var from = new net.minecraft.world.phys.Vec3(.03125, y, -1);
                var to = new net.minecraft.world.phys.Vec3(.03125, y, 2);
                assertNotNull(all.clip(from, to, BlockPos.ZERO));
                assertEquals(y < 1, lower.clip(from, to, BlockPos.ZERO) != null);
            }
        }
    }
    @Test void fittedMasksVolumesAndHitsAgreePixelForPixel() {
        for (var design : DoorDesign.values()) for (boolean fitted : new boolean[]{false, true}) {
            var coverage = CamouflageCoverage.of(design, fitted);
            var boxes = CamouflageGeometry.corners(design, coverage);
            for (int u = 0; u < design.width(); u++) for (int v = 0; v < design.height(); v++) {
                boolean expected = !design.inLeaf(u, v) && (!fitted || design == DoorDesign.ROUND && v < 16);
                assertEquals(expected, coverage.contains(design, u, v));
                double x = u + .5, y = v + .5;
                assertEquals(expected, boxes.stream().anyMatch(b -> b.containsStrictly(x, y, 14.5)));
                for (var hinge : DoorHingeSide.values()) {
                    assertEquals(expected, CamouflageGeometry.isCornerHit(design, hinge, false, x, y, 14.5, coverage));
                }
            }
            for (int col = 0; col < design.columns(); col++) for (int row = 0; row < design.height() / 16; row++) {
                if (fitted && (row > 0 || design != DoorDesign.ROUND)) {
                    assertTrue(CamouflageGeometry.quarter(design, col, row, coverage).isEmpty());
                    assertTrue(CamouflageGeometry.exposedFaces(design, col, row, coverage).isEmpty());
                }
            }
        }
        assertFalse(CamouflageGeometry.corners(DoorDesign.ROUND, CamouflageCoverage.LOWER).isEmpty());
    }
    @Test void existingCamouflageDoesNotPreventCompatibleFitting() {
        for (var hinge : DoorHingeSide.values()) for (var depth : DoorDepth.values()) {
            var door = new DoorLayout.Assembly(BlockPos.ZERO, Direction.NORTH, hinge, DoorDesign.ROUND, depth);
            assertTrue(DoorArchFitPlan.fromDoor(door, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, true).isPresent(),
                    "A camouflaged round door must accept a fitted arch: " + hinge + " " + depth);
        }
    }
}
