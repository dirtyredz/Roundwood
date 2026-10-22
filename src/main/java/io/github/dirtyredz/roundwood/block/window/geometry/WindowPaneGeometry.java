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
 * ({@link WindowDesign}: profile and pattern): the panel, and the glass that joins it to vanilla pane
 * connections. These are logical volumes; what a model draws of them is
 * {@link WindowSurfaceGeometry}'s job (only the visible surfaces, never glass inside glass).
 * Models (datagen), shapes and culling all read this, so what is drawn, what collides and what
 * hides a neighbour's face are one description. Plain Java apart from {@link Direction}.
 *
 * <p><b>Panel.</b> A two-pixel wooden slab over the whole face, wood in every pixel outside the
 * opening and glass, recessed half a pixel on each side, inside it; a crossed design's beams
 * ({@link WindowMuntinGeometry}) are wood at the slab's full depth, the glass cut away under them.
 * Built on the half-pixel grid the beams need.
 *
 * <p><b>Junctions.</b> Only a centred upright pane (the 0.5.0 window) bridges: a connection across
 * its plane gets the vanilla-pane <em>arm</em>, a two-pixel glass strip (x or z 7..9, full height)
 * from the cell edge to the slab, reaching on to the recessed glass where it ends over the opening.
 * Its in-plane sides need nothing (the slab already meets the neighbour). A positioned pane (flush,
 * floor or ceiling) never grows glass beyond its slab, whatever it is connected to (0.6.1 owner
 * ruling: the 0.6.0 posts, stems and fins back to the cell's middle filled corners where flush
 * windows met). The pieces never leave the cell. A pane with no active junction has none.
 */
public final class WindowPaneGeometry {
    public static final String FRAME = "frame";
    public static final String GLASS = "glass";
    /** Half-pixel cells across a face. */
    static final int HALF = 32;
    private static final Box ARM_NORTH = new Box(7, 0, 0, 9, 16, 7);
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
                depth.slabMin(), depth.slabMax(), FRAME);
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

    /** True for the one pose family that bridges to its neighbours: upright, slab in the middle of the cell. */
    public static boolean bridges(WindowPose pose) {
        return pose.upright() && pose.depth() == WindowDepth.CENTER;
    }

    /**
     * The horizontal sides (north, east, south, west order) a connection gets an arm on: the two
     * across a centred upright pane's plane; none for a positioned pane.
     */
    public static List<Direction> neededSides(WindowPose pose) {
        if (!bridges(pose)) {
            return List.of();
        }
        List<Direction> sides = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side.getAxis() == pose.normal()) {
                sides.add(side);
            }
        }
        return List.copyOf(sides);
    }

    /** The arm towards {@code side} (empty when that side needs none). */
    public static List<Piece> arm(WindowDesign design, WindowPose pose, Direction side) {
        if (!neededSides(pose).contains(side)) {
            return List.of();
        }
        return withReach(armBox(side).subtract(pose.slab()), design, pose);
    }

    /** The arms for the connected sides that need one; nothing when none does. */
    public static List<Piece> junction(WindowDesign design, WindowPose pose, Collection<Direction> connected) {
        List<Piece> pieces = new ArrayList<>();
        for (Direction side : neededSides(pose).stream().filter(connected::contains).toList()) {
            pieces.addAll(arm(design, pose, side));
        }
        return List.copyOf(pieces);
    }

    /**
     * Everything the chunk model draws for a state: panel then junction. Trims are attachments drawn
     * apart since 0.8.0 ({@code geometry/trim}); they never replace pane pixels, and the pane imports
     * nothing of them (0.10.0: {@link WindowShapes} composes the targeted selection).
     */
    public static List<Piece> pieces(WindowDesign design, WindowPose pose, Collection<Direction> connected) {
        List<Piece> pieces = new ArrayList<>(panel(design, pose));
        pieces.addAll(junction(design, pose, connected));
        return List.copyOf(pieces);
    }

    /** Collision boxes: the whole slab plus the active junction (a trim never collides). */
    public static List<Box> boxes(WindowDesign design, WindowPose pose, Collection<Direction> connected) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(pose.slab());
        junction(design, pose, connected).forEach(piece -> boxes.add(piece.box()));
        return List.copyOf(boxes);
    }

    /** The unconnected arm towards {@code side}, before the slab is cut away. */
    public static Box armBox(Direction side) {
        return ARM_NORTH.rotateY(switch (side) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        });
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

    /**
     * The boxes as glass, plus, where one ends on the slab's face over the glazing, glass reaching on
     * through the half-pixel recess to the pane's glass (never into a beam, which fills the recess).
     */
    private static List<Piece> withReach(List<Box> boxes, WindowDesign design, WindowPose pose) {
        Box slab = pose.slab();
        int n = WindowPose.index(pose.normal());
        List<Piece> pieces = new ArrayList<>();
        boxes.forEach(box -> pieces.add(new Piece(box, GLASS)));
        for (Box box : boxes) {
            double r1;
            double r2;
            if (max(box, n) == min(slab, n)) {
                r1 = min(slab, n);
                r2 = r1 + WindowDepth.GLASS_INSET;
            } else if (min(box, n) == max(slab, n)) {
                r2 = max(slab, n);
                r1 = r2 - WindowDepth.GLASS_INSET;
            } else {
                continue;
            }
            double[] u = pose.uRange(box);
            double[] v = pose.vRange(box);
            int u0 = (int) Math.round(u[0] * 2);
            int v0 = (int) Math.round(v[0] * 2);
            int width = (int) Math.round(u[1] * 2) - u0;
            int height = (int) Math.round(v[1] * 2) - v0;
            for (PixelCircle.Rect rect : PixelCircle.rasterize(width, height, (pu, pv) -> design.glazed(u0 + pu, v0 + pv))) {
                PixelCircle.Rect at = rect.offset(u0, v0);
                pieces.add(new Piece(pose.toWorld(at.u1() / 2.0, at.v1() / 2.0, at.u2() / 2.0, at.v2() / 2.0, r1, r2), GLASS));
            }
        }
        return List.copyOf(pieces);
    }

    static double min(Box box, int axis) {
        return axis == 0 ? box.minX() : axis == 1 ? box.minY() : box.minZ();
    }

    static double max(Box box, int axis) {
        return axis == 0 ? box.maxX() : axis == 1 ? box.maxY() : box.maxZ();
    }
}
