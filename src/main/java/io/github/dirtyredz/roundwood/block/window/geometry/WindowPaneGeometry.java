package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowMuntinGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

/**
 * Every single-cell window pane in any pose ({@link WindowPose}) with any face
 * ({@link WindowDesign}: profile and pattern): the panel. These are logical volumes; what a model draws of them is
 * {@link WindowSurfaceGeometry}'s job (only the visible surfaces, never glass inside glass).
 * Models (datagen), shapes and culling all read this, so what is drawn, what collides and what
 * hides a neighbour's face are one description. Plain Java apart from {@link Direction}.
 *
 * <p><b>Panel.</b> A two-pixel wooden slab over the whole face, wood in every pixel outside the
 * opening and glass, recessed half a pixel on each side, inside it; a crossed design's beams
 * ({@link WindowMuntinGeometry}) are wood at the slab's full depth, the glass cut away under them.
 * Built on the half-pixel grid the beams need.
 *
 * <p><b>No junctions.</b> A pane never adds glass beyond its own slab, whatever it touches: the 0.5.0
 * connecting arm across a centred pane's plane (a vanilla-pane strip to the cell edge) was removed in
 * 0.13.1 (owner ruling, dk-a970707d: a thin connecting pane showed where a window met a neighbour at a
 * corner or junction), after the 0.6.1 removal of the posts, stems and fins for positioned panes.
 * Neighbours simply abut. Everything here stays inside the pane's own slab.
 */
public final class WindowPaneGeometry {
    public static final String FRAME = "frame";
    public static final String GLASS = "glass";
    public static final String BEAM = "beam";
    /** Half-pixel cells across a face. */
    static final int HALF = 32;
    private static final double EPS = 1e-3;

    private WindowPaneGeometry() {
    }

    /** Wooden face (frame, then any beams) and recessed glass, block-local. */
    public static List<Piece> panel(WindowDesign design, WindowPose pose) {
        List<Piece> pieces = new ArrayList<>();
        WindowDepth depth = pose.depth();
        addHalfRects(pieces, (hu, hv) -> !design.profile().inOpening(hu / 2, hv / 2), pose,
                depth.slabMin(), depth.slabMax(), FRAME);
        addHalfRects(pieces, (hu, hv) -> WindowMuntinGeometry.isBeam(design, hu, hv), pose,
                depth.slabMin(), depth.slabMax(), BEAM);
        addHalfRects(pieces, design::glazed, pose,
                depth.slabMin() + WindowDepth.GLASS_INSET, depth.slabMax() - WindowDepth.GLASS_INSET, GLASS);
        return List.copyOf(pieces);
    }

    /** The cells passing {@code test} on the half-pixel face grid, merged into rectangles, extruded over {@code [n1,n2]}. */
    static void addHalfRects(List<Piece> out, PixelCircle.PixelTest test, WindowPose pose, double n1, double n2,
                             String texture) {
        for (PixelCircle.Rect rect : PixelCircle.rasterize(HALF, HALF, test)) {
            out.add(new Piece(pose.toWorld(rect.u1() / 2.0, rect.v1() / 2.0, rect.u2() / 2.0, rect.v2() / 2.0, n1, n2),
                    texture));
        }
    }

    /** Collision boxes: the whole slab and nothing else (a trim never collides). */
    public static List<Box> boxes(WindowPose pose) {
        return List.of(pose.slab());
    }

    /**
     * True when the {@code face} side of {@code part} is entirely against other solid geometry in
     * {@code solids} (its own pieces, the panel): such a face is never seen and is not drawn.
     * Checked on the half-pixel grid every piece is aligned to.
     */
    public static boolean buried(Box part, Direction face, Collection<Box> solids) {
        int n = WindowPose.index(face.getAxis());
        double plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max(part, n) : min(part, n);
        double outward = plane + face.getAxisDirection().getStep() * EPS;
        int a = (n + 1) % 3;
        int b = (n + 2) % 3;
        for (double x = min(part, a) + 0.25; x < max(part, a); x += 0.5) {
            for (double y = min(part, b) + 0.25; y < max(part, b); y += 0.5) {
                double[] p = new double[3];
                p[n] = outward;
                p[a] = x;
                p[b] = y;
                if (solids.stream().noneMatch(s -> s.containsStrictly(p[0], p[1], p[2]))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * What {@code boxes} show on the cell boundary {@code side}: a 32 x 32 half-pixel grid (row-major
     * over the side's two other axes, x before y before z) of the cells some box face covers there.
     */
    public static BitSet boundary(Collection<Box> boxes, Direction side) {
        int n = WindowPose.index(side.getAxis());
        double plane = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Box.BLOCK : 0;
        int a = n == 0 ? 1 : 0;
        int b = n == 2 ? 1 : 2;
        BitSet out = new BitSet(32 * 32);
        for (Box box : boxes) {
            double face = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max(box, n) : min(box, n);
            if (face != plane) {
                continue;
            }
            for (int i = (int) Math.round(min(box, a) * 2); i < (int) Math.round(max(box, a) * 2); i++) {
                for (int j = (int) Math.round(min(box, b) * 2); j < (int) Math.round(max(box, b) * 2); j++) {
                    out.set(i * 32 + j);
                }
            }
        }
        return out;
    }

    static double min(Box box, int axis) {
        return axis == 0 ? box.minX() : axis == 1 ? box.minY() : box.minZ();
    }

    static double max(Box box, int axis) {
        return axis == 0 ? box.maxX() : axis == 1 ? box.maxY() : box.maxZ();
    }
}
