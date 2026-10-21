package io.github.dirtyredz.roundwood.geometry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rasterizes circles onto the 1/16 pixel grid as merged rectangles. A pixel belongs to a circle
 * when its centre lies inside it, which makes every result symmetric in both axes.
 */
public final class PixelCircle {
    private PixelCircle() {
    }

    /** A rectangle of whole pixels, {@code [u1,u2) x [v1,v2)}. */
    public record Rect(int u1, int v1, int u2, int v2) {
        public Rect {
            if (u1 >= u2 || v1 >= v2) {
                throw new IllegalArgumentException("Empty rect " + u1 + "," + v1 + " " + u2 + "," + v2);
            }
        }

        public Rect offset(int du, int dv) {
            return new Rect(u1 + du, v1 + dv, u2 + du, v2 + dv);
        }

        public int area() {
            return (u2 - u1) * (v2 - v1);
        }

        /** Extrudes the rectangle (u = x, v = y) between two depths. */
        public Box extrude(double z1, double z2) {
            return new Box(u1, v1, z1, u2, v2, z2);
        }
    }

    @FunctionalInterface
    public interface PixelTest {
        boolean contains(int u, int v);
    }

    /** True when pixel (u, v) of a {@code size}-wide grid lies inside the centred circle of that diameter. */
    public static boolean inDisc(int size, int diameter, int u, int v) {
        double centre = size / 2.0;
        double radius = diameter / 2.0;
        double du = u + 0.5 - centre;
        double dv = v + 0.5 - centre;
        return du * du + dv * dv <= radius * radius;
    }

    /**
     * True when pixel (u, v)'s centre lies inside the circle of {@code diameter} centred at
     * ({@code centreU}, {@code centreV}) in pixel coordinates (which may sit on a grid corner, outside
     * the grid being drawn: the arch windows cut one circle across two blocks).
     */
    public static boolean inCircle(double centreU, double centreV, double diameter, int u, int v) {
        double radius = diameter / 2.0;
        double du = u + 0.5 - centreU;
        double dv = v + 0.5 - centreV;
        return du * du + dv * dv <= radius * radius;
    }

    /** A filled disc of the given diameter. */
    public static List<Rect> disc(int diameter) {
        return rasterize(diameter, diameter, (u, v) -> inDisc(diameter, diameter, u, v));
    }

    /** The outer {@code width} pixels of a disc: inside the disc, outside the concentric smaller one. */
    public static List<Rect> ring(int diameter, int width) {
        int inner = diameter - 2 * width;
        return rasterize(diameter, diameter,
                (u, v) -> inDisc(diameter, diameter, u, v) && !inDisc(diameter, inner, u, v));
    }


    /**
     * Turns a pixel predicate into rectangles: runs per row, then identical runs on consecutive rows
     * merged vertically. Deterministic order (by first row, then by u).
     */
    public static List<Rect> rasterize(int width, int height, PixelTest test) {
        List<Rect> done = new ArrayList<>();
        Map<Long, int[]> open = new LinkedHashMap<>(); // key (u1,u2) -> {u1, v1, u2}
        for (int v = 0; v < height; v++) {
            Map<Long, int[]> next = new LinkedHashMap<>();
            int u = 0;
            while (u < width) {
                if (!test.contains(u, v)) {
                    u++;
                    continue;
                }
                int start = u;
                while (u < width && test.contains(u, v)) {
                    u++;
                }
                long key = ((long) start << 32) | u;
                int[] run = open.remove(key);
                next.put(key, run != null ? run : new int[] {start, v, u});
            }
            for (int[] closed : open.values()) {
                done.add(new Rect(closed[0], closed[1], closed[2], v));
            }
            open = next;
        }
        for (int[] closed : open.values()) {
            done.add(new Rect(closed[0], closed[1], closed[2], height));
        }
        done.sort((a, b) -> a.v1() != b.v1() ? Integer.compare(a.v1(), b.v1()) : Integer.compare(a.u1(), b.u1()));
        return List.copyOf(done);
    }
}
