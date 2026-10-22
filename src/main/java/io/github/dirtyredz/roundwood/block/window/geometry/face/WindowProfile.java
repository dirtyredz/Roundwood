package io.github.dirtyredz.roundwood.block.window.geometry.face;

import io.github.dirtyredz.roundwood.geometry.PixelCircle;

/**
 * The thirteen single-cell window faces: which pixels of a square wooden pane are glazed. The round
 * window is a centred 12-pixel circle; the upper arches are the two halves of an upper semicircle
 * ({@link WindowArchGeometry}); the bottom corners complete them into a 2 x 2 arched-top window
 * ({@link WindowCornerGeometry}). 0.10.0 adds explicit reflections and edges: the lower arches (the
 * upper ones reflected top to bottom, so four arches glaze a full circle), the top corners (the
 * bottom ones reflected), and four single-edge pieces framed on one side only
 * ({@link WindowEdgeGeometry}). Pixels are (u, v), u to the right and v up as seen from the front.
 * The pieces of the 2 x 2 window also know where they sit in that 32 x 32 assembly; the round window
 * and the edges are their own 16-pixel face. New profiles are appended: the creative order and the
 * index-based tests follow this order.
 */
public enum WindowProfile {
    ROUND("round_window", 0, 0),
    ARCH_LEFT("round_window_arch_left", 0, 1),
    ARCH_RIGHT("round_window_arch_right", 1, 1),
    BOTTOM_LEFT("round_window_bottom_left", 0, 0),
    BOTTOM_RIGHT("round_window_bottom_right", 1, 0),
    ARCH_BOTTOM_LEFT("round_window_arch_bottom_left", 0, 0),
    ARCH_BOTTOM_RIGHT("round_window_arch_bottom_right", 1, 0),
    TOP_LEFT("round_window_top_left", 0, 1),
    TOP_RIGHT("round_window_top_right", 1, 1),
    EDGE_TOP("round_window_edge_top", 0, 0),
    EDGE_LEFT("round_window_edge_left", 0, 0),
    EDGE_BOTTOM("round_window_edge_bottom", 0, 0),
    EDGE_RIGHT("round_window_edge_right", 0, 0);

    public static final int SIZE = 16;
    /** The round window's opening (unchanged since 0.3). */
    public static final int ROUND_OPENING = 12;
    /** The 2 x 2 arched window's face, in pixels. */
    public static final int ASSEMBLY = 32;

    private final String id;
    private final int column;
    private final int row;

    WindowProfile(String id, int column, int row) {
        this.id = id;
        this.column = column;
        this.row = row;
    }

    /** The registry path of the plain block with this face (also the stem of its model names). */
    public String id() {
        return id;
    }

    /** False only for the round window, whose state keeps the 0.5.0 axis and position encoding. */
    public boolean oriented() {
        return this != ROUND;
    }

    /** The framed side of a single-edge piece, or null for every other profile. */
    public WindowEdgeGeometry.Side edge() {
        return switch (this) {
            case EDGE_TOP -> WindowEdgeGeometry.Side.TOP;
            case EDGE_LEFT -> WindowEdgeGeometry.Side.LEFT;
            case EDGE_BOTTOM -> WindowEdgeGeometry.Side.BOTTOM;
            case EDGE_RIGHT -> WindowEdgeGeometry.Side.RIGHT;
            default -> null;
        };
    }

    /** True for the pieces of the 2 x 2 window (arches and corners), which share one 32-pixel artwork. */
    public boolean inAssembly() {
        return this != ROUND && edge() == null;
    }

    /**
     * True when the piece takes an arch sill (0.10.0): the upper arches, whose bottom edge is the
     * circle's diameter. The lower arches' top diameter does not (an explicit product choice, not yet made).
     */
    public boolean takesSill() {
        return this == ARCH_LEFT || this == ARCH_RIGHT;
    }

    /** Pixel width and height of the face the whole-face artwork spans: 16 for a lone piece, 32 for the 2 x 2 window. */
    public int assemblySize() {
        return inAssembly() ? ASSEMBLY : SIZE;
    }

    /** Where this piece's face starts inside {@link #assemblySize()}, in pixels across. */
    public int assemblyU() {
        return column * SIZE;
    }

    /** Where this piece's face starts inside {@link #assemblySize()}, in pixels up. */
    public int assemblyV() {
        return row * SIZE;
    }

    public boolean inOpening(int u, int v) {
        return switch (this) {
            case ROUND -> PixelCircle.inDisc(SIZE, ROUND_OPENING, u, v);
            case ARCH_LEFT -> WindowArchGeometry.inOpening(false, u, v);
            case ARCH_RIGHT -> WindowArchGeometry.inOpening(true, u, v);
            case BOTTOM_LEFT -> WindowCornerGeometry.inOpening(false, u, v);
            case BOTTOM_RIGHT -> WindowCornerGeometry.inOpening(true, u, v);
            case ARCH_BOTTOM_LEFT -> WindowArchGeometry.inLowerOpening(false, u, v);
            case ARCH_BOTTOM_RIGHT -> WindowArchGeometry.inLowerOpening(true, u, v);
            case TOP_LEFT -> WindowCornerGeometry.inTopOpening(false, u, v);
            case TOP_RIGHT -> WindowCornerGeometry.inTopOpening(true, u, v);
            case EDGE_TOP, EDGE_LEFT, EDGE_BOTTOM, EDGE_RIGHT -> WindowEdgeGeometry.inOpening(edge(), u, v);
        };
    }
}
