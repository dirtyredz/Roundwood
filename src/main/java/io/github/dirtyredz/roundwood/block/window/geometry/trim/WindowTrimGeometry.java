package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowArchGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowCornerGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The window trim: a band that follows each window's opening rim, one pixel proud of a face of the
 * pane. Round window: the ring between diameters 16 and 12 (the round face's border); arches: their
 * quarter of the 32/28 ring around the arch circle; bottom corners: the outer jamb and sill. Cross
 * beams never get trim (the band is always over frame wood).
 *
 * <p>Since 0.8.0 each face is its own attachment, and every band projects outward from its face of
 * the slab, along the normal's positive world axis: a centred pane ({@code 7..9}) has its sides at
 * {@code 6..7} and {@code 9..10}; a flush pane at {@code 0..2} at {@code -1..0} and {@code 2..3}; one at
 * {@code 14..16} at {@code 13..14} and {@code 16..17}. The wall-side band of a flush, floor or ceiling
 * pane therefore lies one pixel beyond the cell: it is render-only (block-entity renderer), never a
 * collision, support or connection, and never occupies the neighbouring block. 0.7.0 inlaid it at
 * {@code 0..1} / {@code 15..16}, replacing the pane's own outer pixels, which read as no trim at all.
 * Plain Java apart from {@link Direction}.
 */
public final class WindowTrimGeometry {
    /** The texture variable the trim's pieces carry; surfaces split it into face and edge artwork. */
    public static final String TRIM = "trim";
    public static final String FACE = "trim_face";
    public static final String EDGE = "trim_edge";
    /** How far a band stands proud of its face, in pixels. */
    public static final double PROUD = 1;
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

    /**
     * The two depth ranges {@code {n1, n2}} along the normal's positive world axis: index 0 the band on
     * the negative side of the slab, index 1 the band on its positive side.
     */
    public static double[][] depths(WindowDepth depth) {
        return new double[][] {{depth.slabMin() - PROUD, depth.slabMin()}, {depth.slabMax(), depth.slabMax() + PROUD}};
    }

    /** The depth range of the band on {@code side} of the slab ({@code side} lies along the normal). */
    public static double[] depth(WindowPose pose, Direction side) {
        if (side.getAxis() != pose.normal()) {
            throw new IllegalArgumentException(side + " is not along the normal of " + pose);
        }
        return depths(pose.depth())[side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0];
    }

    /** The band's volumes on {@code side} of a pane posed {@code pose}, block-local (may reach one pixel past the cell). */
    public static List<Piece> pieces(WindowProfile profile, WindowPose pose, Direction side) {
        double[] range = depth(pose, side);
        List<Piece> out = new ArrayList<>();
        for (PixelCircle.Rect rect : band(profile)) {
            out.add(new Piece(pose.toWorld(rect.u1(), rect.v1(), rect.u2(), rect.v2(), range[0], range[1]), TRIM));
        }
        return List.copyOf(out);
    }

    /**
     * What the outline and rays use for the bands on {@code sides}: each band where it lies inside the
     * cell. A band beyond the cell adds nothing (the slab's own face already covers its footprint).
     */
    public static List<Box> targetBoxes(WindowProfile profile, WindowPose pose, Collection<Direction> sides) {
        List<Box> out = new ArrayList<>();
        for (Direction side : sides) {
            for (Piece piece : pieces(profile, pose, side)) {
                if (piece.box().within(Box.cell())) {
                    out.add(piece.box());
                }
            }
        }
        return List.copyOf(out);
    }

    /** The band's pixels merged into rectangles (face coordinates). */
    public static List<PixelCircle.Rect> band(WindowProfile profile) {
        return PixelCircle.rasterize(WindowProfile.SIZE, WindowProfile.SIZE, (u, v) -> inBand(profile, u, v));
    }
}
