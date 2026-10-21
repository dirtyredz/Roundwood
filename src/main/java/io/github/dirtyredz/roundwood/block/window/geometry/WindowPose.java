package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.core.Direction;

import java.util.Optional;

/**
 * How a single-cell window pane sits in its cell: the direction its front face points
 * ({@code front}, the pane's normal), which in-plane direction is the top of its artwork
 * ({@code top}) and where its slab lies along the normal ({@code depth}). The pane's face is
 * authored in pixels {@code (u, v)}, u to the right and v up as seen from the front; this frame maps
 * that face into the cell. Every blockstate encoding (the round window's axis and position, an arch's
 * orientation and depth) resolves to one of these. Plain Java apart from {@link Direction}.
 */
public record WindowPose(Direction front, Direction top, WindowDepth depth) {
    public static final double SIZE = 16;

    public WindowPose {
        if (front.getAxis() == top.getAxis()) {
            throw new IllegalArgumentException("front " + front + " and top " + top + " share an axis");
        }
    }

    /** The upright pane facing {@code front} (top up). */
    public static WindowPose upright(Direction front, WindowDepth depth) {
        return new WindowPose(front, Direction.UP, depth);
    }

    public Direction.Axis normal() {
        return front.getAxis();
    }

    /** True for a pane standing upright (normal horizontal); false for a floor or ceiling pane. */
    public boolean upright() {
        return normal().isHorizontal();
    }

    /** The viewer's right as seen from the front: {@code top x front}. */
    public Direction right() {
        return cross(top, front);
    }

    /** The cell side the slab lies against, or empty when it is centred. */
    public Optional<Direction> flushSide() {
        return depth.side(normal());
    }

    /** The whole two-pixel slab, block-local. */
    public Box slab() {
        return toWorld(0, 0, SIZE, SIZE, depth.slabMin(), depth.slabMax());
    }

    /**
     * Maps a face rectangle {@code [u1,u2] x [v1,v2]} (pixels, as seen from the front) extruded over
     * {@code [n1,n2]} along the normal's positive world axis to a block-local box.
     */
    public Box toWorld(double u1, double v1, double u2, double v2, double n1, double n2) {
        double[] min = new double[3];
        double[] max = new double[3];
        place(min, max, right(), u1, u2);
        place(min, max, top, v1, v2);
        int n = index(normal());
        min[n] = n1;
        max[n] = n2;
        return new Box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    /** The face pixel range {u1, u2} covering world range {@code [a,b]} on the right's axis. */
    public double[] uRange(Box box) {
        return range(box, right());
    }

    /** The face pixel range {v1, v2} covering the box on the top's axis. */
    public double[] vRange(Box box) {
        return range(box, top);
    }

    /** Turns the pose clockwise (seen from above) by quarter turns, as {@link Box#rotateY} turns geometry. */
    public WindowPose rotateY(int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        Direction f = front;
        Direction t = top;
        for (int i = 0; i < turns; i++) {
            f = turnClockwise(f);
            t = turnClockwise(t);
        }
        Optional<Direction> side = flushSide();
        WindowDepth d = depth;
        if (side.isPresent()) {
            Direction s = side.get();
            for (int i = 0; i < turns; i++) {
                s = turnClockwise(s);
            }
            d = WindowDepth.flushAgainst(s);
        }
        return new WindowPose(f, t, d);
    }

    /**
     * The same geometry reflected across the plane perpendicular to {@code axis} (x or z), expressed
     * for a piece of the same handedness: a reflected left arch is a left arch seen from its other
     * face, so the front flips round as well. The slab follows the reflection.
     */
    public WindowPose mirror(Direction.Axis axis) {
        Direction f = flip(front, axis).getOpposite();
        Direction t = flip(top, axis);
        WindowDepth d = flushSide().map(s -> WindowDepth.flushAgainst(flip(s, axis))).orElse(WindowDepth.CENTER);
        return new WindowPose(f, t, d);
    }

    public static Direction flip(Direction direction, Direction.Axis axis) {
        return direction.getAxis() == axis ? direction.getOpposite() : direction;
    }

    private static Direction turnClockwise(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction.getClockWise() : direction;
    }

    /** The direction of {@code a x b} (both axis-aligned unit vectors on different axes). */
    static Direction cross(Direction a, Direction b) {
        int x = a.getStepY() * b.getStepZ() - a.getStepZ() * b.getStepY();
        int y = a.getStepZ() * b.getStepX() - a.getStepX() * b.getStepZ();
        int z = a.getStepX() * b.getStepY() - a.getStepY() * b.getStepX();
        for (Direction d : Direction.values()) {
            if (d.getStepX() == x && d.getStepY() == y && d.getStepZ() == z) {
                return d;
            }
        }
        throw new IllegalArgumentException(a + " x " + b);
    }

    static int index(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    private static void place(double[] min, double[] max, Direction along, double a1, double a2) {
        int i = index(along.getAxis());
        if (along.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            min[i] = a1;
            max[i] = a2;
        } else {
            min[i] = SIZE - a2;
            max[i] = SIZE - a1;
        }
    }

    private static double[] range(Box box, Direction along) {
        int i = index(along.getAxis());
        double lo = i == 0 ? box.minX() : i == 1 ? box.minY() : box.minZ();
        double hi = i == 0 ? box.maxX() : i == 1 ? box.maxY() : box.maxZ();
        return along.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? new double[] {lo, hi} : new double[] {SIZE - hi, SIZE - lo};
    }
}
