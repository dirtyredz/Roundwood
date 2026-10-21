package io.github.dirtyredz.roundwood.geometry;

import java.util.List;

/**
 * The round door's ring profile (shared since 0.8.0): a flat ring in the 32 x 32 plane of a 2 x 2
 * assembly, outer diameter {@link #OUTER}, inner {@link #INNER} (the two-pixel border around the
 * door's disc). The freestanding frame ({@code block/trim}) and the door's trim attachment
 * ({@code block/door/trim}) both extrude it; neither family imports the other for it. Plain Java.
 */
public final class RingGeometry {
    public static final int OUTER = 32;
    public static final int INNER = 28;
    public static final int WIDTH = (OUTER - INNER) / 2;

    private static final List<PixelCircle.Rect> RECTS = PixelCircle.ring(OUTER, WIDTH);

    private RingGeometry() {
    }

    /** True when pixel (u, v) of the 32 x 32 assembly plane is part of the ring. */
    public static boolean inRing(int u, int v) {
        return PixelCircle.inDisc(OUTER, OUTER, u, v) && !PixelCircle.inDisc(OUTER, INNER, u, v);
    }

    /** True when pixel (u, v) is inside the ring's outer edge (the ring or the disc it frames). */
    public static boolean inOuterDisc(int u, int v) {
        return PixelCircle.inDisc(OUTER, OUTER, u, v);
    }

    /** The ring's pixels merged into rectangles (x across, y up, 0..32). */
    public static List<PixelCircle.Rect> rects() {
        return RECTS;
    }

    /** The ring extruded over {@code [z1, z2]}, assembly coordinates. */
    public static List<Box> extrude(double z1, double z2) {
        return RECTS.stream().map(rect -> rect.extrude(z1, z2)).toList();
    }
}
