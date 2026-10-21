package io.github.dirtyredz.roundwood.block.window.geometry;

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
 * ({@link WindowProfile}): the panel, and the glass that joins it to vanilla pane connections.
 * Models (datagen), shapes and culling all read this, so what is drawn, what collides and what
 * hides a neighbour's face are one description. Plain Java apart from {@link Direction}.
 *
 * <p><b>Panel.</b> A two-pixel wooden slab over the whole face, wood in every pixel outside the
 * opening and glass, recessed half a pixel on each side, inside it.
 *
 * <p><b>Junctions.</b> Vanilla panes always reach for the middle of the cell's side: a two-pixel
 * strip at x or z 7..9, full height. A side whose strip the slab already covers (the in-plane sides
 * of a centred upright pane, the flush side of a flush one) needs nothing. Every other connected
 * side gets an <em>arm</em> from that strip to a central glass <em>post</em> (x and z 7..9, full
 * height); a flush upright pane adds a <em>stem</em> from the post to its slab; a floor or ceiling
 * pane meets the post and arms as vertical fins. Post and stem together are the <em>hub</em>. All
 * of it is cut away where the slab is, and wherever it ends on the slab over the opening it reaches
 * on to the recessed glass. The pieces never leave the cell. A pane with no active junction has none.
 */
public final class WindowPaneGeometry {
    public static final String FRAME = "frame";
    public static final String GLASS = "glass";
    public static final Box POST = new Box(7, 0, 7, 9, 16, 9);
    private static final Box ARM_NORTH = new Box(7, 0, 0, 9, 16, 7);
    private static final double EPS = 1e-3;

    private WindowPaneGeometry() {
    }

    /** Wooden face and recessed glass, block-local. */
    public static List<Piece> panel(WindowProfile profile, WindowPose pose) {
        List<Piece> pieces = new ArrayList<>();
        WindowDepth depth = pose.depth();
        for (PixelCircle.Rect rect : PixelCircle.rasterize(WindowProfile.SIZE, WindowProfile.SIZE,
                (u, v) -> !profile.inOpening(u, v))) {
            pieces.add(new Piece(pose.toWorld(rect.u1(), rect.v1(), rect.u2(), rect.v2(), depth.slabMin(), depth.slabMax()), FRAME));
        }
        for (PixelCircle.Rect rect : PixelCircle.rasterize(WindowProfile.SIZE, WindowProfile.SIZE, profile::inOpening)) {
            pieces.add(new Piece(pose.toWorld(rect.u1(), rect.v1(), rect.u2(), rect.v2(),
                    depth.slabMin() + WindowDepth.GLASS_INSET, depth.slabMax() - WindowDepth.GLASS_INSET), GLASS));
        }
        return List.copyOf(pieces);
    }

    /** The horizontal sides (north, east, south, west order) whose vanilla strip the slab does not cover. */
    public static List<Direction> neededSides(WindowPose pose) {
        Box slab = pose.slab();
        List<Direction> sides = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!coversStrip(slab, side)) {
                sides.add(side);
            }
        }
        return List.copyOf(sides);
    }

    /** The post and stem (empty when the slab already contains the post, as for centred upright panes). */
    public static List<Piece> hub(WindowProfile profile, WindowPose pose) {
        if (neededSides(pose).isEmpty()) {
            return List.of();
        }
        Box slab = pose.slab();
        List<Box> boxes = new ArrayList<>(POST.subtract(slab));
        if (pose.upright() && slab.intersect(POST).isEmpty()) {
            boxes.add(stem(pose, slab));
        }
        return withReach(boxes, profile, pose);
    }

    /** The arm towards {@code side} (empty when that side needs none). */
    public static List<Piece> arm(WindowProfile profile, WindowPose pose, Direction side) {
        if (!neededSides(pose).contains(side)) {
            return List.of();
        }
        return withReach(armBox(side).subtract(pose.slab()), profile, pose);
    }

    /** Hub plus arms for the connected sides that need one; nothing when none does. */
    public static List<Piece> junction(WindowProfile profile, WindowPose pose, Collection<Direction> connected) {
        List<Direction> active = neededSides(pose).stream().filter(connected::contains).toList();
        if (active.isEmpty()) {
            return List.of();
        }
        List<Piece> pieces = new ArrayList<>(hub(profile, pose));
        for (Direction side : active) {
            pieces.addAll(arm(profile, pose, side));
        }
        return List.copyOf(pieces);
    }

    /** Everything drawn for a state: panel then junction. */
    public static List<Piece> pieces(WindowProfile profile, WindowPose pose, Collection<Direction> connected) {
        List<Piece> pieces = new ArrayList<>(panel(profile, pose));
        pieces.addAll(junction(profile, pose, connected));
        return List.copyOf(pieces);
    }

    /** Collision and outline boxes: the whole slab plus the active junction. */
    public static List<Box> boxes(WindowProfile profile, WindowPose pose, Collection<Direction> connected) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(pose.slab());
        junction(profile, pose, connected).forEach(piece -> boxes.add(piece.box()));
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
     * {@code solids} (its own pieces, the panel, the hub): such a face is never seen and is not drawn.
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

    private static Box stem(WindowPose pose, Box slab) {
        if (pose.normal() == Direction.Axis.Z) {
            return slab.maxZ() <= POST.minZ() ? new Box(7, 0, slab.maxZ(), 9, 16, POST.minZ())
                    : new Box(7, 0, POST.maxZ(), 9, 16, slab.minZ());
        }
        return slab.maxX() <= POST.minX() ? new Box(slab.maxX(), 0, 7, POST.minX(), 16, 9)
                : new Box(POST.maxX(), 0, 7, slab.minX(), 16, 9);
    }

    /** Whether the slab covers the vanilla pane strip on {@code side} (two pixels wide, full height, at the boundary). */
    private static boolean coversStrip(Box slab, Direction side) {
        boolean fullHeight = slab.minY() <= 0 && slab.maxY() >= Box.BLOCK;
        return fullHeight && switch (side) {
            case NORTH -> slab.minZ() == 0 && slab.minX() <= 7 && slab.maxX() >= 9;
            case SOUTH -> slab.maxZ() == Box.BLOCK && slab.minX() <= 7 && slab.maxX() >= 9;
            case WEST -> slab.minX() == 0 && slab.minZ() <= 7 && slab.maxZ() >= 9;
            case EAST -> slab.maxX() == Box.BLOCK && slab.minZ() <= 7 && slab.maxZ() >= 9;
            default -> true;
        };
    }

    /**
     * The boxes as glass, plus, where one ends on the slab's face over the opening, glass reaching on
     * through the half-pixel recess to the pane's glass.
     */
    private static List<Piece> withReach(List<Box> boxes, WindowProfile profile, WindowPose pose) {
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
            int u0 = (int) Math.round(u[0]);
            int v0 = (int) Math.round(v[0]);
            int width = (int) Math.round(u[1]) - u0;
            int height = (int) Math.round(v[1]) - v0;
            for (PixelCircle.Rect rect : PixelCircle.rasterize(width, height, (pu, pv) -> profile.inOpening(u0 + pu, v0 + pv))) {
                PixelCircle.Rect at = rect.offset(u0, v0);
                pieces.add(new Piece(pose.toWorld(at.u1(), at.v1(), at.u2(), at.v2(), r1, r2), GLASS));
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
