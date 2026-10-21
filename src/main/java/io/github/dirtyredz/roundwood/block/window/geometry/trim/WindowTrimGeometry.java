package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowArchGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowCornerGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;

import java.util.ArrayList;
import java.util.List;

/**
 * The window trim (0.7.0): a band that follows each window's opening rim, on both faces, inside the
 * window's own cell. Round window: the ring between diameters 16 and 12 (the round face's border);
 * arches: their quarter of the 32/28 ring around the arch circle; bottom corners: the outer jamb and
 * sill. Cross beams never get trim (the band is always frame wood).
 *
 * <p>Depth, along the normal's positive world axis: a centred pane ({@code 7..9}) gets one pixel
 * proud on each side ({@code 6..7}, {@code 9..10}); a flush pane gets its outer skin replaced by trim
 * ({@code 0..1} or {@code 15..16}, so the wall plane stays flat) and one pixel proud on the inner side
 * ({@code 2..3} or {@code 13..14}). Nothing leaves the cell. Plain Java.
 */
public final class WindowTrimGeometry {
    /** The texture variable the trim's pieces carry; models split it into face and edge artwork. */
    public static final String TRIM = "trim";
    public static final String FACE = "trim_face";
    public static final String EDGE = "trim_edge";
    private static final int RING_OUTER = 32;
    private static final int RING_INNER = 28;

    private WindowTrimGeometry() {
    }

    /** True when pixel (u, v) of {@code profile}'s face is under the trim band. */
    public static boolean inBand(WindowProfile profile, int u, int v) {
        return switch (profile) {
            case ROUND -> PixelCircle.inDisc(WindowProfile.SIZE, WindowProfile.SIZE, u, v)
                    && !PixelCircle.inDisc(WindowProfile.SIZE, WindowProfile.ROUND_OPENING, u, v);
            case ARCH_LEFT, ARCH_RIGHT -> {
                int cu = u + profile.assemblyU();
                yield PixelCircle.inCircle(WindowArchGeometry.CENTRE_U, WindowArchGeometry.CENTRE_V, RING_OUTER, cu, v)
                        && !PixelCircle.inCircle(WindowArchGeometry.CENTRE_U, WindowArchGeometry.CENTRE_V, RING_INNER, cu, v);
            }
            case BOTTOM_LEFT -> WindowCornerGeometry.inFrame(false, u, v);
            case BOTTOM_RIGHT -> WindowCornerGeometry.inFrame(true, u, v);
        };
    }

    /** The two depth ranges {@code {n1, n2}} the trim occupies for a slab at {@code depth}. */
    public static double[][] depths(WindowDepth depth) {
        return switch (depth) {
            case CENTER -> new double[][] {{6, 7}, {9, 10}};
            case NEGATIVE -> new double[][] {{0, 1}, {2, 3}};
            case POSITIVE -> new double[][] {{13, 14}, {15, 16}};
        };
    }

    /** The trim's volumes for a pane posed {@code pose}, block-local. */
    public static List<Piece> pieces(WindowProfile profile, WindowPose pose) {
        List<Piece> out = new ArrayList<>();
        for (double[] range : depths(pose.depth())) {
            for (PixelCircle.Rect rect : PixelCircle.rasterize(WindowProfile.SIZE, WindowProfile.SIZE,
                    (u, v) -> inBand(profile, u, v))) {
                out.add(new Piece(pose.toWorld(rect.u1(), rect.v1(), rect.u2(), rect.v2(), range[0], range[1]), TRIM));
            }
        }
        return List.copyOf(out);
    }
}
