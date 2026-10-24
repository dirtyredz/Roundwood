package io.github.dirtyredz.roundwood.geometry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The round frame ring: an exact 32-over-28 pixel annulus, one pixel deep, split into four cell quarters. */
class RingGeometryTest {
    private static final double EPS = 1e-9;

    /** Per-pixel coverage count of the boxes in the 32 x 32 plane. */
    private static int[][] coverage(List<Box> boxes) {
        int[][] grid = new int[32][32];
        for (Box box : boxes) {
            for (int u = (int) box.minX(); u < (int) box.maxX(); u++) {
                for (int v = (int) box.minY(); v < (int) box.maxY(); v++) {
                    grid[u][v]++;
                }
            }
        }
        return grid;
    }

    private static boolean insideDisc(int u, int v, double diameter) {
        double du = u + 0.5 - 16;
        double dv = v + 0.5 - 16;
        double r = diameter / 2;
        return du * du + dv * dv <= r * r;
    }

    private static List<Box> quartersInAssembly() {
        List<Box> all = new ArrayList<>();
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                for (Box box : RingGeometry.quarter(column, row)) {
                    all.add(box.translate(16 * column, 16 * row, 0));
                }
            }
        }
        return all;
    }

    @Test
    void ringMaskIsExactlyTheDiscMinusTheInnerDisc() {
        int[][] grid = coverage(RingGeometry.ring());
        for (int u = 0; u < 32; u++) {
            for (int v = 0; v < 32; v++) {
                boolean expected = insideDisc(u, v, 32) && !insideDisc(u, v, 28);
                assertEquals(expected ? 1 : 0, grid[u][v], "pixel " + u + "," + v);
            }
        }
    }

    @Test
    void everyRingBoxIsOnePixelDeepAtTheBack() {
        assertEquals(0, RingGeometry.BACK, EPS);
        assertEquals(1, RingGeometry.FRONT, EPS);
        for (Box box : RingGeometry.ring()) {
            assertEquals(0, box.minZ(), EPS, "min z: " + box);
            assertEquals(1, box.maxZ(), EPS, "max z: " + box);
            assertTrue(box.within(new Box(0, 0, 0, 32, 32, 1)), "outside the assembly: " + box);
        }
    }

    @Test
    void theApertureIsOpen() {
        int[][] grid = coverage(RingGeometry.ring());
        assertEquals(0, grid[16][16], "centre pixel");
        for (int u = 0; u < 32; u++) {
            for (int v = 0; v < 32; v++) {
                if (insideDisc(u, v, 28)) {
                    assertEquals(0, grid[u][v], "aperture pixel covered: " + u + "," + v);
                }
            }
        }
    }

    @Test
    void theFourQuartersAreDisjointInsideTheirCellAndSumToTheRing() {
        double total = 0;
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                for (Box box : RingGeometry.quarter(column, row)) {
                    assertTrue(box.within(new Box(0, 0, 0, 16, 16, 1)), "quarter box outside its cell: " + box);
                    total += box.volume();
                }
            }
        }
        double ring = RingGeometry.ring().stream().mapToDouble(Box::volume).sum();
        assertEquals(ring, total, EPS, "quarters must add up to the ring");
        int[][] grid = coverage(quartersInAssembly());
        int[][] whole = coverage(RingGeometry.ring());
        for (int u = 0; u < 32; u++) {
            for (int v = 0; v < 32; v++) {
                assertEquals(whole[u][v], grid[u][v], "quarter union differs at " + u + "," + v);
                assertTrue(grid[u][v] <= 1, "quarters overlap at " + u + "," + v);
            }
        }
    }

    @Test
    void inRingAgreesWithBoxCoverage() {
        int[][] grid = coverage(RingGeometry.ring());
        for (int u = 0; u < 32; u++) {
            for (int v = 0; v < 32; v++) {
                assertEquals(grid[u][v] == 1, RingGeometry.inRing(u, v), "inRing at " + u + "," + v);
            }
        }
        assertFalse(RingGeometry.inRing(16, 16));
    }

    @Test
    void theRingTouchesAllFourOuterEdges() {
        List<Box> ring = RingGeometry.ring();
        assertEquals(0, ring.stream().mapToDouble(Box::minX).min().orElseThrow(), EPS, "x = 0");
        assertEquals(32, ring.stream().mapToDouble(Box::maxX).max().orElseThrow(), EPS, "x = 32");
        assertEquals(0, ring.stream().mapToDouble(Box::minY).min().orElseThrow(), EPS, "y = 0");
        assertEquals(32, ring.stream().mapToDouble(Box::maxY).max().orElseThrow(), EPS, "y = 32");
        assertEquals(2, RingGeometry.WIDTH);
    }
}
