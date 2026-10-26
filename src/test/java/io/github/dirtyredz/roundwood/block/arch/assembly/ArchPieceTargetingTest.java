package io.github.dirtyredz.roundwood.block.arch.assembly;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class ArchPieceTargetingTest {
    @Test void everyFaceOffersAllFourEdgesAndOverlayCentroidsSelectTheirOwnZone() {
        for (var facing : Direction.Plane.HORIZONTAL) for (var face : Direction.values()) {
            var edges = new HashSet<Direction>();
            for (var zone : ArchPieceTargeting.Zone.values()) {
                var polygon = ArchPieceTargeting.polygon(zone);
                double u = polygon.stream().mapToDouble(ArchPieceTargeting.Point::u).average().orElseThrow();
                double v = polygon.stream().mapToDouble(ArchPieceTargeting.Point::v).average().orElseThrow();
                double x = face.getAxis() == Direction.Axis.X ? .5 : u;
                double y = face.getAxis() == Direction.Axis.Y ? .5 : v;
                double z = face.getAxis() == Direction.Axis.Y ? v : face.getAxis() == Direction.Axis.X ? u : .5;
                var target = ArchPieceTargeting.target(facing, face, x, y, z);
                assertEquals(zone, target.zone());
                var edge = target.side() == Side.LEFT ? target.facing().getCounterClockWise() : target.facing().getClockWise();
                assertTrue(edges.add(edge), "unreachable shoulder edge");
                var boxes = ArchPieceGeometry.placed(ArchPieceDesign.KEYHOLE_SHOULDER, target.side(), target.facing());
                assertFalse(boxes.isEmpty());
                for (var box : boxes) {
                    if (edge == Direction.WEST) assertTrue(box.maxX() <= 4);
                    if (edge == Direction.EAST) assertTrue(box.minX() >= 12);
                    if (edge == Direction.NORTH) assertTrue(box.maxZ() <= 4);
                    if (edge == Direction.SOUTH) assertTrue(box.minZ() >= 12);
                }
            }
            assertEquals(4, edges.size());
        }
    }
    @Test void tiesAndNonFiniteHitsAreExplicit() {
        assertEquals(ArchPieceTargeting.Zone.U_MIN, ArchPieceTargeting.classify(.5, .5));
        assertEquals(ArchPieceTargeting.Zone.U_MAX, ArchPieceTargeting.classify(.75, .75));
        assertEquals(ArchPieceTargeting.Zone.V_MAX, ArchPieceTargeting.classify(.75, .875));
        assertThrows(IllegalArgumentException.class, () -> ArchPieceTargeting.classify(Double.NaN, .5));
        assertThrows(IllegalArgumentException.class, () -> ArchPieceTargeting.target(Direction.UP, Direction.UP, .5, .5, .5));
    }
}
