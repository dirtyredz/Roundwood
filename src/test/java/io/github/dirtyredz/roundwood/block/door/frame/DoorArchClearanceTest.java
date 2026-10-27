package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorArchClearanceTest {
    @Test void onlyEqualThinClosedPlanesAreRefused() {
        for (var depth : ArchPieceDepth.values()) for (var door : io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.values()) {
            assertEquals(!depth.name().equals(door.name()), DoorArchClearance.accepts(ArchPieceThickness.THIN, depth, door));
            assertEquals(depth == ArchPieceDepth.FRONT, DoorArchClearance.accepts(ArchPieceThickness.FULL, depth, door));
        }
    }

    @Test void fittedFullArchEqualsStandaloneFullArchAndRearCasingsKeepOnePixelCoverage() {
        for (var door : DoorDesign.values()) {
            var top = DoorArchFitPlan.topFor(door);
            var bodies = DoorArchGeometry.boxes(door, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, DoorArchGeometry.Layer.BODY);
            var backs = DoorArchGeometry.boxes(door, ArchPieceThickness.FULL, ArchPieceDepth.FRONT, DoorArchGeometry.Layer.BACK_CASING);
            assertEquals(bodies.size(), backs.size());
            for (int i = 0; i < bodies.size(); i++) {
                var body = bodies.get(i); var casing = backs.get(i);
                assertEquals(16, body.maxZ());
                assertEquals(0, body.minZ(), "no rebate: back face at the standalone full arch depth");
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

}
