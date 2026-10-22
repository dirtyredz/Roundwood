package io.github.dirtyredz.roundwood.block.window.geometry.face;

/**
 * The single-edge pieces (0.10.0): a square pane framed on exactly one side by a
 * {@link WindowCornerGeometry#BORDER}-pixel band and glazed everywhere else, so runs of them make
 * long windows whose only wood is the outer border. Which side is fixed per piece ({@link Side}, as
 * seen from the front). Pixels are (u, v), u to the right and v up. Plain Java.
 */
public final class WindowEdgeGeometry {
    /** The framed side of a single-edge piece, as seen from its front. */
    public enum Side {
        TOP, LEFT, BOTTOM, RIGHT
    }

    private static final int BORDER = WindowCornerGeometry.BORDER;
    private static final int SIZE = WindowProfile.SIZE;

    private WindowEdgeGeometry() {
    }

    /** True when pixel (u, v) lies in the border on {@code side}. */
    public static boolean inFrame(Side side, int u, int v) {
        return switch (side) {
            case TOP -> v >= SIZE - BORDER;
            case LEFT -> u < BORDER;
            case BOTTOM -> v < BORDER;
            case RIGHT -> u >= SIZE - BORDER;
        };
    }

    public static boolean inOpening(Side side, int u, int v) {
        return !inFrame(side, u, v);
    }
}
