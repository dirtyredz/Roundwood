package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowArchGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowCornerGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowEdgeGeometry;
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
 * pane. Round window: the ring between diameters 16 and 12 (the round face's border); upper arches:
 * their quarter of the 32/28 ring around the arch circle, and the lower arches (0.10.0) the same
 * ring reflected, so four arches carry one full ring; bottom corners: the outer jamb and sill, top
 * corners the outer jamb and head; single-edge pieces only their one framed side (never three
 * missing borders). Cross beams never get trim (the band is always over frame wood).
 *
 * <p>Since 0.8.0 each face is its own attachment, and every band projects outward from its face of
 * the slab, along the normal's positive world axis: a centred pane ({@code 7..9}) has its sides at
 * {@code 6..7} and {@code 9..10}; a flush pane at {@code 0..2} at {@code -1..0} and {@code 2..3}; one at
 * {@code 14..16} at {@code 13..14} and {@code 16..17}. The wall-side band of a flush, floor or ceiling
 * pane therefore lies one pixel beyond the cell: it is render-only (block-entity renderer), never a
 * collision, support or connection, and never occupies the neighbouring block. 0.7.0 inlaid it at
 * {@code 0..1} / {@code 15..16}, replacing the pane's own outer pixels, which read as no trim at all.
 *
 * <p>Since 0.10.0 a face may also carry an arch sill ({@link TrimSlot#SILL}, {@link WindowSillGeometry})
 * at the same depth; every volume method here takes the slot, and the slot's pixels never overlap
 * another slot's. Plain Java apart from {@link Direction}.
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
    private static final int SIZE = WindowProfile.SIZE;

    private WindowTrimGeometry() {
    }

    /** True when pixel (u, v) of {@code profile}'s face is under the perimeter trim band. */
    public static boolean inBand(WindowProfile profile, int u, int v) {
        return switch (profile) {
            case ROUND -> PixelCircle.inDisc(SIZE, SIZE, u, v) && !PixelCircle.inDisc(SIZE, WindowProfile.ROUND_OPENING, u, v);
            case ARCH_LEFT, ARCH_RIGHT -> inArchRing(u + profile.assemblyU(), v);
            case ARCH_BOTTOM_LEFT, ARCH_BOTTOM_RIGHT -> inArchRing(u + profile.assemblyU(), SIZE - 1 - v);
            case BOTTOM_LEFT -> WindowCornerGeometry.inFrame(false, u, v);
            case BOTTOM_RIGHT -> WindowCornerGeometry.inFrame(true, u, v);
            case TOP_LEFT -> WindowCornerGeometry.inTopFrame(false, u, v);
            case TOP_RIGHT -> WindowCornerGeometry.inTopFrame(true, u, v);
            case EDGE_TOP, EDGE_LEFT, EDGE_BOTTOM, EDGE_RIGHT -> WindowEdgeGeometry.inFrame(profile.edge(), u, v);
        };
    }

    /** True when pixel (u, v) of {@code profile}'s face is under the attachment in {@code slot}. */
    public static boolean covers(TrimSlot slot, WindowProfile profile, int u, int v) {
        return slot == TrimSlot.SILL ? WindowSillGeometry.inSill(profile, u, v) : inBand(profile, u, v);
    }

    /** True when {@code profile} can carry anything in {@code slot}. */
    public static boolean supports(TrimSlot slot, WindowProfile profile) {
        return slot == TrimSlot.PERIMETER || profile.takesSill();
    }

    /** Pixel (cu, v) of the upper 32 x 16 arch face lies in the 32/28 ring around the arch circle. */
    private static boolean inArchRing(int cu, int v) {
        return PixelCircle.inCircle(WindowArchGeometry.CENTRE_U, WindowArchGeometry.CENTRE_V, RING_OUTER, cu, v)
                && !PixelCircle.inCircle(WindowArchGeometry.CENTRE_U, WindowArchGeometry.CENTRE_V, RING_INNER, cu, v);
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

    /** The perimeter band's volumes on {@code side} of a pane posed {@code pose}, block-local. */
    public static List<Piece> pieces(WindowProfile profile, WindowPose pose, Direction side) {
        return pieces(TrimSlot.PERIMETER, profile, pose, side);
    }

    /** The volumes of the attachment in {@code slot} on {@code side}, block-local (may reach one pixel past the cell). */
    public static List<Piece> pieces(TrimSlot slot, WindowProfile profile, WindowPose pose, Direction side) {
        double[] range = depth(pose, side);
        List<Piece> out = new ArrayList<>();
        for (PixelCircle.Rect rect : band(slot, profile)) {
            out.add(new Piece(pose.toWorld(rect.u1(), rect.v1(), rect.u2(), rect.v2(), range[0], range[1]), TRIM));
        }
        return List.copyOf(out);
    }

    /** The perimeter bands' target boxes on {@code sides} ({@link #targetBoxes(TrimSlot, WindowProfile, WindowPose, Collection)}). */
    public static List<Box> targetBoxes(WindowProfile profile, WindowPose pose, Collection<Direction> sides) {
        return targetBoxes(TrimSlot.PERIMETER, profile, pose, sides);
    }

    /**
     * What the outline and rays use for the attachments in {@code slot} on {@code sides}: each where it
     * lies inside the cell. A band beyond the cell adds nothing (the slab's own face already covers its
     * footprint).
     */
    public static List<Box> targetBoxes(TrimSlot slot, WindowProfile profile, WindowPose pose, Collection<Direction> sides) {
        List<Box> out = new ArrayList<>();
        for (Direction side : sides) {
            for (Piece piece : pieces(slot, profile, pose, side)) {
                if (piece.box().within(Box.cell())) {
                    out.add(piece.box());
                }
            }
        }
        return List.copyOf(out);
    }

    /** The perimeter band's pixels merged into rectangles (face coordinates). */
    public static List<PixelCircle.Rect> band(WindowProfile profile) {
        return band(TrimSlot.PERIMETER, profile);
    }

    /** The pixels of the attachment in {@code slot} merged into rectangles (face coordinates); empty when unsupported. */
    public static List<PixelCircle.Rect> band(TrimSlot slot, WindowProfile profile) {
        return PixelCircle.rasterize(SIZE, SIZE, (u, v) -> covers(slot, profile, u, v));
    }
}
