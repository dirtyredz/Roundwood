package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class DoorArchClearanceTest {
    @Test void intervalExtremaIncludeInteriorStationaryPoints() {
        var range = DoorArchClearance.range(3, 4, 0, Math.PI / 2);
        assertEquals(3, range[0], 1e-8);
        assertEquals(5, range[1], 1e-8);
        var negative = DoorArchClearance.range(-3, -4, 0, Math.PI / 2);
        assertEquals(-5, negative[0], 1e-8);
        assertEquals(-3, negative[1], 1e-8);
        for (double a : new double[]{-19, -1.5, 0, 1.5, 33}) for (double b : new double[]{-2, -.5, 0, 2}) {
            double lo = .173, hi = .619;
            var bounded = DoorArchClearance.range(a, b, lo, hi);
            for (int i = 0; i <= 100; i++) {
                double angle = lo + (hi - lo) * i / 100;
                double value = a * Math.cos(angle) + b * Math.sin(angle);
                assertTrue(value >= bounded[0] && value <= bounded[1]);
            }
        }
    }

    @Test void fullRebatesAndEligibleThinPlanesClearBothHingesAndAllHardware() {
        for (var door : DoorDesign.values()) {
            assertFalse(DoorArchClearance.accepts(door, ArchPieceThickness.THIN, ArchPieceDepth.BACK));
            for (var thickness : ArchPieceThickness.values()) for (var depth : ArchPieceDepth.values()) {
                if (!DoorArchClearance.accepts(door, thickness, depth)) continue;
                var frame = new ArrayList<Box>();
                for (var layer : DoorArchGeometry.Layer.values())
                    frame.addAll(DoorArchGeometry.boxes(door, thickness, depth, layer).stream()
                            .map(b -> b.translate(0, DoorArchFitPlan.topOffset(door) * 16, 0)).toList());
                for (var hinge : DoorHingeSide.values()) for (var piece : DoorGeometry.leaf(door, hinge))
                    for (double step : new double[]{0, .1, 11.25, 37, 45, 73.9, 89.9, 90}) {
                        Box swung = bounds(door, hinge, piece.box(), step);
                        for (Box box : frame) assertFalse(overlaps(swung, box), () -> door + " " + thickness + " " + depth + " " + swung + " / " + box);
                    }
            }
            assertTrue(DoorArchClearance.accepts(door, ArchPieceThickness.THIN, ArchPieceDepth.MIDDLE));
            assertTrue(DoorArchClearance.accepts(door, ArchPieceThickness.THIN, ArchPieceDepth.FRONT));
        }
    }

    @Test void frontMasksStayExactAndRearCasingsKeepCompleteOnePixelCoverage() {
        for (var door : DoorDesign.values()) {
            var top = DoorArchFitPlan.topFor(door);
            var bodies = DoorArchGeometry.boxes(door, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, DoorArchGeometry.Layer.BODY);
            var backs = DoorArchGeometry.boxes(door, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, DoorArchGeometry.Layer.BACK_CASING);
            assertEquals(bodies.size(), backs.size());
            for (int i = 0; i < bodies.size(); i++) {
                var body = bodies.get(i); var casing = backs.get(i);
                assertEquals(16, body.maxZ());
                assertTrue(body.minZ() < 8, "rear recess remains less than half depth");
                assertEquals(body.minZ(), casing.maxZ());
                assertEquals(1, casing.maxZ() - casing.minZ(), 1e-12);
            }
            for (int x = 0; x < top.width(); x++) for (int y = 0; y < top.height(); y++) {
                final int px = x, py = y;
                assertEquals(top.contains(Side.LEFT, x, y), bodies.stream().anyMatch(b -> b.containsStrictly(px + .5, py + .5, 15.9)));
                assertNotEquals(top.contains(Side.LEFT, x, y), door.inLeaf(x, y + DoorArchFitPlan.topOffset(door) * 16));
            }
        }
    }

    @Test void visibleSurfacesHaveNoInternalFacesOrCrossTileSideUvs() {
        for (var door : DoorDesign.values()) for (var layer : DoorArchGeometry.Layer.values()) {
            var top = DoorArchFitPlan.topFor(door);
            var boxes = DoorArchGeometry.boxes(door, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, layer);
            for (int column = 0; column < top.columns(); column++) for (int row = 0; row < top.rows(); row++)
                for (var surface : DoorArchGeometry.surfaces(door, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, layer, column, row)) {
                    var b = surface.backing(); var face = surface.face();
                    if (face.getAxis() != Direction.Axis.Z)
                        assertEquals(Math.floor(b.minZ() / 16), Math.floor((b.maxZ() - 1e-8) / 16), "side UV crosses tile");
                    double x = (b.minX() + b.maxX()) / 2 + column * 16;
                    double y = (b.minY() + b.maxY()) / 2 + row * 16;
                    double z = (b.minZ() + b.maxZ()) / 2;
                    switch (face) {
                        case WEST -> x = b.minX() + column * 16 - 1e-6;
                        case EAST -> x = b.maxX() + column * 16 + 1e-6;
                        case DOWN -> y = b.minY() + row * 16 - 1e-6;
                        case UP -> y = b.maxY() + row * 16 + 1e-6;
                        case NORTH -> z = b.minZ() - 1e-6;
                        case SOUTH -> z = b.maxZ() + 1e-6;
                    }
                    final double px = x, py = y, pz = z;
                    assertFalse(boxes.stream().anyMatch(box -> box.containsStrictly(px, py, pz)), "internal surface");
                }
        }
    }

    private static Box bounds(DoorDesign door, DoorHingeSide hinge, Box box, double degrees) {
        double minX = Double.POSITIVE_INFINITY, minZ = minX, maxX = Double.NEGATIVE_INFINITY, maxZ = maxX;
        for (double x : new double[]{box.minX(), box.maxX()}) for (double z : new double[]{box.minZ(), box.maxZ()}) {
            var point = DoorGeometry.turn(door, x, z, hinge == DoorHingeSide.LEFT ? degrees : -degrees, hinge);
            minX = Math.min(minX, point[0]); maxX = Math.max(maxX, point[0]);
            minZ = Math.min(minZ, point[1] - 13); maxZ = Math.max(maxZ, point[1] - 13);
        }
        return new Box(minX, box.minY(), minZ, maxX, box.maxY(), maxZ);
    }
    private static boolean overlaps(Box a, Box b) {
        return Math.min(a.maxX(), b.maxX()) - Math.max(a.minX(), b.minX()) > 1e-8
                && Math.min(a.maxY(), b.maxY()) - Math.max(a.minY(), b.minY()) > 1e-8
                && Math.min(a.maxZ(), b.maxZ()) - Math.max(a.minZ(), b.minZ()) > 1e-8;
    }
}
