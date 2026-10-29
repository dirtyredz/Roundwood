package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchMasonryJoints;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchMasonryModelsTest {
    @Test void partitionCoversEveryFaceOnceAndClipsMortarToTheAnalyticLayout() {
        for (var design : ArchPieceDesign.values()) for (var side : Side.values()) {
            var joints = ArchMasonryModels.joints(design);
            double mortarArea = 0;
            for (int column = 0; column < design.columns(); column++) for (int row = 0; row < design.rows(); row++)
                for (var surface : ArchPieceGeometry.surfaces(design, side, column, row, 0, 16, "")) {
                    if (surface.face() != Direction.SOUTH) continue;
                    var box = surface.backing();
                    var patches = PolygonPartition.partition(box.minX() + column * 16, box.minY() + row * 16,
                            box.maxX() + column * 16, box.maxY() + row * 16, joints);
                    double area = (box.maxX() - box.minX()) * (box.maxY() - box.minY());
                    assertEquals(area, patches.stream().mapToDouble(p -> PolygonPartition.area(p.polygon())).sum(), 1e-7);
                    for (var patch : patches) {
                        double x = patch.polygon().stream().mapToDouble(PolygonPartition.Point::x).average().orElseThrow();
                        double y = patch.polygon().stream().mapToDouble(PolygonPartition.Point::y).average().orElseThrow();
                        assertTrue(design.contains(side, (int) Math.floor(x), (int) Math.floor(y)));
                        assertEquals(patch.masked(), ArchMasonryJoints.contains(design, side, x, y), design + " " + x + "," + y);
                        if (patch.masked()) mortarArea += PolygonPartition.area(patch.polygon());
                    }
                    // Independently sample coverage: every interior sample belongs to precisely one patch.
                    for (double x = box.minX() + .173; x < box.maxX(); x += .731)
                        for (double y = box.minY() + .239; y < box.maxY(); y += .677) {
                            double px = x + column * 16, py = y + row * 16;
                            assertEquals(1, patches.stream().filter(p -> contains(p.polygon(), px, py)).count());
                        }
                }
            assertTrue(mortarArea > 0, "masonry has visible joints");
        }
    }

    @Test void quarterPixelRadialJointsLeaveACentredKeystoneAndNoCellSeams() {
        for (var design : ArchPieceDesign.values()) {
            if (design.shoulder()) continue;
            var joints = ArchMasonryModels.joints(design);
            double radius = design.top().radius() + 1, centre = design.width() / 2.0;
            assertFalse(joints.stream().anyMatch(p -> contains(p, centre, design.top().spring() + radius)), "centred keystone");
            for (double angle : ArchMasonryJoints.radialAngles(design)) {
                double x = centre + radius * Math.cos(angle), y = design.top().spring() + radius * Math.sin(angle);
                // Avoid exact polygon edges along the radial centreline.
                assertTrue(joints.stream().anyMatch(p -> contains(p, x - .03 * Math.sin(angle), y + .03 * Math.cos(angle))));
                assertFalse(joints.stream().anyMatch(p -> contains(p, x - .13 * Math.sin(angle), y + .13 * Math.cos(angle))));
            }
            assertFalse(joints.stream().anyMatch(p -> contains(p, 16, design.height() - .25)), "no joint at cell boundary");
        }
    }

    private static boolean contains(java.util.List<PolygonPartition.Point> polygon, double x, double y) {
        for (int i = 0; i < polygon.size(); i++) {
            var a = polygon.get(i); var b = polygon.get((i + 1) % polygon.size());
            if ((b.x() - a.x()) * (y - a.y()) - (b.y() - a.y()) * (x - a.x()) < -1e-8) return false;
        }
        return true;
    }
}
