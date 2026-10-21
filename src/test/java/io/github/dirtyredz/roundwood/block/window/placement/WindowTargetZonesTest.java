package io.github.dirtyredz.roundwood.block.window.placement;

import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones.FacePoint;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones.Zone;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Hammer-style face partition shared by placement and the client overlay. */
class WindowTargetZonesTest {
    private static Zone zone(double u, double v) {
        return WindowTargetZones.classify(new FacePoint(u, v));
    }

    @Test
    void centreSquareIncludesItsBoundary() {
        assertEquals(Zone.CENTER, zone(0.5, 0.5));
        for (double[] p : new double[][] {{0.25, 0.25}, {0.75, 0.75}, {0.25, 0.75}, {0.75, 0.25}, {0.25, 0.5}, {0.5, 0.75}}) {
            assertEquals(Zone.CENTER, zone(p[0], p[1]), p[0] + "," + p[1]);
        }
        assertEquals(Zone.U_MIN, zone(0.2499, 0.5));
        assertEquals(Zone.U_MAX, zone(0.7501, 0.5));
        assertEquals(Zone.V_MIN, zone(0.5, 0.2499));
        assertEquals(Zone.V_MAX, zone(0.5, 0.7501));
    }

    @Test
    void outsideTheCentreTheLargerDisplacementPicksTheEdge() {
        assertEquals(Zone.U_MIN, zone(0.05, 0.4));
        assertEquals(Zone.U_MAX, zone(0.95, 0.6));
        assertEquals(Zone.V_MIN, zone(0.4, 0.05));
        assertEquals(Zone.V_MAX, zone(0.6, 0.95));
        assertEquals(Zone.U_MAX, zone(0.9, 0.85));
        assertEquals(Zone.V_MAX, zone(0.85, 0.9));
    }

    @Test
    void exactDiagonalTiesAndCornersChooseTheUEdge() {
        assertEquals(Zone.U_MIN, zone(0, 0));
        assertEquals(Zone.U_MAX, zone(1, 0));
        assertEquals(Zone.U_MAX, zone(1, 1));
        assertEquals(Zone.U_MIN, zone(0, 1));
        assertEquals(Zone.U_MIN, zone(0.1, 0.1));
        assertEquals(Zone.U_MAX, zone(0.8, 0.8));
    }

    @Test
    void faceCoordinatesFollowTheFixedWorldTable() {
        // up/down: u east (x), v south (z); north/south: u east (x), v up (y); east/west: u south (z), v up (y)
        assertPoint(WindowTargetZones.facePoint(Direction.UP, 0.2, 1, 0.7), 0.2, 0.7);
        assertPoint(WindowTargetZones.facePoint(Direction.DOWN, 0.2, 0, 0.7), 0.2, 0.7);
        assertPoint(WindowTargetZones.facePoint(Direction.NORTH, 0.3, 0.9, 0), 0.3, 0.9);
        assertPoint(WindowTargetZones.facePoint(Direction.SOUTH, 0.3, 0.9, 1), 0.3, 0.9);
        assertPoint(WindowTargetZones.facePoint(Direction.EAST, 1, 0.6, 0.1), 0.1, 0.6);
        assertPoint(WindowTargetZones.facePoint(Direction.WEST, 0, 0.6, 0.1), 0.1, 0.6);
    }

    @Test
    void exactBoundaryHitsStayOnTheirFaceAndSlightErrorIsClamped() {
        // An exact hit on the far edge is 1.0, not 0.0 (no modulo).
        assertPoint(WindowTargetZones.facePoint(Direction.UP, 1.0, 1, 1.0), 1, 1);
        assertEquals(Zone.U_MAX, WindowTargetZones.classify(WindowTargetZones.facePoint(Direction.UP, 1.0, 1, 0.5)));
        // Slightly negative and slightly beyond one.
        assertPoint(WindowTargetZones.facePoint(Direction.UP, -1e-9, 1, 1 + 1e-9), 0, 1);
        assertEquals(Zone.U_MIN, WindowTargetZones.classify(WindowTargetZones.facePoint(Direction.NORTH, -1e-7, 0.5, 0)));
    }

    @Test
    void everyFaceAndZoneMapsToItsWorldDirection() {
        Map<Direction, Direction[]> expected = Map.of(
                // {U_MIN, U_MAX, V_MIN, V_MAX}
                Direction.UP, new Direction[] {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
                Direction.DOWN, new Direction[] {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
                Direction.NORTH, new Direction[] {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
                Direction.SOUTH, new Direction[] {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
                Direction.EAST, new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP},
                Direction.WEST, new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP});
        Zone[] edges = {Zone.U_MIN, Zone.U_MAX, Zone.V_MIN, Zone.V_MAX};
        for (Direction face : Direction.values()) {
            assertEquals(Optional.empty(), WindowTargetZones.edge(face, Zone.CENTER), face + " centre");
            for (int i = 0; i < 4; i++) {
                assertEquals(Optional.of(expected.get(face)[i]), WindowTargetZones.edge(face, edges[i]), face + " " + edges[i]);
            }
        }
    }

    /** Points chosen independently of the thresholds lie inside exactly the polygon of the zone the classifier names. */
    @Test
    void polygonsAgreeWithTheClassifier() {
        for (int i = 1; i < 40; i++) {
            for (int j = 1; j < 40; j++) {
                double u = i / 40.0 + 0.0013;
                double v = j / 40.0 + 0.0007;
                if (u >= 1 || v >= 1) {
                    continue;
                }
                Zone expected = zone(u, v);
                for (Zone zone : Zone.values()) {
                    assertEquals(zone == expected, inside(WindowTargetZones.polygon(zone), u, v),
                            u + "," + v + " in " + zone + " (classified " + expected + ")");
                }
            }
        }
    }

    @Test
    void polygonsTileTheFaceWithTheCentreSquareInTheMiddle() {
        assertEquals(List.of(new FacePoint(0.25, 0.25), new FacePoint(0.75, 0.25), new FacePoint(0.75, 0.75),
                new FacePoint(0.25, 0.75)), WindowTargetZones.polygon(Zone.CENTER));
        double area = 0;
        for (Zone zone : Zone.values()) {
            area += area(WindowTargetZones.polygon(zone));
        }
        assertEquals(1.0, area, 1e-12);
    }

    @Test
    void toCellPutsFaceCoordinatesOnTheFace() {
        assertArrayEquals(new double[] {0.2, 1, 0.7}, WindowTargetZones.toCell(Direction.UP, 0.2, 0.7), 1e-12);
        assertArrayEquals(new double[] {0.2, 0, 0.7}, WindowTargetZones.toCell(Direction.DOWN, 0.2, 0.7), 1e-12);
        assertArrayEquals(new double[] {0.3, 0.9, 0}, WindowTargetZones.toCell(Direction.NORTH, 0.3, 0.9), 1e-12);
        assertArrayEquals(new double[] {1, 0.6, 0.1}, WindowTargetZones.toCell(Direction.EAST, 0.1, 0.6), 1e-12);
        for (Direction face : Direction.values()) {
            double[] p = WindowTargetZones.toCell(face, 0.31, 0.77);
            FacePoint back = WindowTargetZones.facePoint(face, p[0], p[1], p[2]);
            assertEquals(0.31, back.u(), 1e-12, face + " u");
            assertEquals(0.77, back.v(), 1e-12, face + " v");
        }
    }

    private static void assertPoint(FacePoint point, double u, double v) {
        assertEquals(u, point.u(), 1e-12);
        assertEquals(v, point.v(), 1e-12);
    }

    private static boolean inside(List<FacePoint> polygon, double u, double v) {
        boolean in = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            FacePoint a = polygon.get(i);
            FacePoint b = polygon.get(j);
            if ((a.v() > v) != (b.v() > v) && u < (b.u() - a.u()) * (v - a.v()) / (b.v() - a.v()) + a.u()) {
                in = !in;
            }
        }
        return in;
    }

    private static double area(List<FacePoint> polygon) {
        double sum = 0;
        for (int i = 0; i < polygon.size(); i++) {
            FacePoint a = polygon.get(i);
            FacePoint b = polygon.get((i + 1) % polygon.size());
            sum += a.u() * b.v() - b.u() * a.v();
        }
        assertTrue(sum > 0, "polygon is not counter-clockwise");
        return sum / 2;
    }
}
