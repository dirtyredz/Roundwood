package io.github.dirtyredz.roundwood.geometry;

import io.github.dirtyredz.roundwood.door.DoorGeometry;
import io.github.dirtyredz.roundwood.window.WindowGeometry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Geometry invariants, checked against independent pixel maths rather than the rasterizer's own code. */
class PixelCircleTest {

    /** Expands rectangles back to a pixel set, failing on any overlap. */
    private static Set<Long> pixels(List<PixelCircle.Rect> rects) {
        Set<Long> out = new HashSet<>();
        for (PixelCircle.Rect r : rects) {
            for (int u = r.u1(); u < r.u2(); u++) {
                for (int v = r.v1(); v < r.v2(); v++) {
                    assertTrue(out.add(key(u, v)), "overlap at " + u + "," + v);
                }
            }
        }
        return out;
    }

    private static long key(int u, int v) {
        return ((long) u << 32) | (v & 0xffffffffL);
    }

    private static boolean inside(double cx, double cy, double r, int u, int v) {
        double dx = u + 0.5 - cx;
        double dy = v + 0.5 - cy;
        return Math.hypot(dx, dy) <= r;
    }

    @Test
    void discMatchesPointInCircleAndIsSymmetric() {
        for (int d : new int[] {12, 16, 28, 32}) {
            Set<Long> px = pixels(PixelCircle.disc(d));
            for (int u = -1; u <= d; u++) {
                for (int v = -1; v <= d; v++) {
                    boolean expected = u >= 0 && v >= 0 && u < d && v < d && inside(d / 2.0, d / 2.0, d / 2.0, u, v);
                    assertEquals(expected, px.contains(key(u, v)), "d=" + d + " pixel " + u + "," + v);
                    // Mirror symmetry in both axes and across the diagonal.
                    assertEquals(px.contains(key(u, v)), px.contains(key(d - 1 - u, v)));
                    assertEquals(px.contains(key(u, v)), px.contains(key(u, d - 1 - v)));
                    assertEquals(px.contains(key(u, v)), px.contains(key(v, u)));
                }
            }
            // The widest row spans the full diameter; corners are empty.
            assertTrue(px.contains(key(0, d / 2)) && px.contains(key(d - 1, d / 2)));
            assertFalse(px.contains(key(0, 0)));
        }
    }

    @Test
    void ringPlusInnerDiscIsTheDiscAndCornersAreTheRest() {
        int d = 32;
        Set<Long> disc = pixels(PixelCircle.disc(d));
        Set<Long> ring = pixels(PixelCircle.ring(d, 2));
        Set<Long> inner = new HashSet<>();
        for (PixelCircle.Rect r : PixelCircle.disc(d - 4)) {
            pixels(List.of(r.offset(2, 2))).forEach(inner::add);
        }
        Set<Long> union = new HashSet<>(ring);
        for (Long p : inner) {
            assertTrue(union.add(p), "ring and inner disc overlap");
        }
        assertEquals(disc, union);
        Set<Long> outside = pixels(PixelCircle.outside(d));
        assertEquals(d * d, outside.size() + disc.size());
        outside.forEach(p -> assertFalse(disc.contains(p)));
    }

    @Test
    void doorQuartersPartitionTheAssemblyAndStayInsideTheirCell() {
        double closedVolume = 0;
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                for (boolean open : new boolean[] {false, true}) {
                    double volume = 0;
                    for (Piece piece : DoorGeometry.pieces(column, row, open)) {
                        assertTrue(piece.box().within(Box.cell()), "piece outside cell: " + piece);
                        volume += piece.box().volume();
                    }
                    for (Box box : DoorGeometry.envelope(column, row, open)) {
                        assertTrue(box.within(Box.cell()), "envelope outside cell: " + box);
                        assertTrue(open ? box.maxX() <= 3 || box.minX() >= 13 : box.minZ() >= 13,
                                "leaf thicker than 3/16 or out of its slab: " + box);
                    }
                    if (!open) {
                        closedVolume += volume;
                    } else {
                        // Swinging moves geometry; it never adds or loses any.
                        double closed = DoorGeometry.pieces(column, row, false).stream().mapToDouble(p -> p.box().volume()).sum();
                        assertEquals(closed, volume, 1e-9);
                    }
                }
            }
        }
        double assembly = DoorGeometry.assembly().stream().mapToDouble(p -> p.box().volume()).sum();
        assertEquals(assembly, closedVolume, 1e-9, "the four quarters must add up to the whole door");
    }

    @Test
    void openLeavesLeaveAtLeast26PixelsOfClearWidth() {
        // Both open leaves hug their outer edges: left leaf within x <= 3 of the left cell, right leaf
        // within x >= 13 of the right cell, so the gap spans (16 - 3) + 13 = 26 pixels.
        double leftMax = 0;
        double rightMin = 16;
        for (int row = 0; row < 2; row++) {
            for (Box box : DoorGeometry.envelope(0, row, true)) {
                leftMax = Math.max(leftMax, box.maxX());
            }
            for (Box box : DoorGeometry.envelope(1, row, true)) {
                rightMin = Math.min(rightMin, box.minX());
            }
        }
        assertTrue((16 - leftMax) + rightMin >= 26, "clear width " + ((16 - leftMax) + rightMin));
    }

    @Test
    void closedCornersAreEmptyAndTheCentreIsSolid() {
        List<Box> lowerLeft = DoorGeometry.envelope(0, 0, false);
        assertFalse(covers(lowerLeft, 0.5, 0.5), "outer corner should be open");
        assertTrue(covers(lowerLeft, 15.5, 15.5), "centre should be solid");
        assertTrue(covers(lowerLeft, 0.5, 15.5), "disc reaches the outer edge at mid height");
    }

    private static boolean covers(List<Box> boxes, double x, double y) {
        return boxes.stream().anyMatch(b -> b.minX() <= x && x <= b.maxX() && b.minY() <= y && y <= b.maxY());
    }

    @Test
    void fourQuarterTurnsAreIdentityAndOneTurnMovesSouthToWest() {
        Box box = new Box(1, 2, 13, 5, 6, 16);
        assertEquals(box, box.rotateY(4));
        Box turned = box.rotateY(1);
        assertEquals(new Box(0, 2, 1, 3, 6, 5), turned);
        assertEquals(box.volume(), turned.volume(), 1e-9);
    }

    @Test
    void windowPiecesStayInsideTheBlockAndFillsMirror() {
        for (Piece piece : WindowGeometry.core()) {
            assertTrue(piece.box().within(Box.cell()));
        }
        double east = 0;
        for (Piece piece : WindowGeometry.sideFill()) {
            assertTrue(piece.box().minX() >= 8 && piece.box().within(Box.cell()));
            east += piece.box().volume();
        }
        double west = 0;
        for (Piece piece : WindowGeometry.sideFill()) {
            Box b = piece.rotateY(2).box();
            assertTrue(b.maxX() <= 8);
            west += b.volume();
        }
        assertEquals(east, west, 1e-9);
        for (Piece piece : WindowGeometry.arm()) {
            assertTrue(piece.box().minZ() == 0 && piece.box().within(Box.cell()));
        }
    }
}
