package io.github.dirtyredz.roundwood.geometry;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rasterizer invariants, checked against independent pixel maths rather than the rasterizer's own code. */
class PixelCircleTest {

    /** Expands rectangles back to a pixel set, failing on any overlap. */
    static Set<Long> pixels(List<PixelCircle.Rect> rects) {
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

    static long key(int u, int v) {
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
    void ringPlusInnerDiscIsTheDiscAndTheComplementIsTheRest() {
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
        Set<Long> outside = pixels(PixelCircle.rasterize(d, d, (u, v) -> !PixelCircle.inDisc(d, d, u, v)));
        assertEquals(d * d, outside.size() + disc.size());
        outside.forEach(p -> assertFalse(disc.contains(p)));
    }
}
