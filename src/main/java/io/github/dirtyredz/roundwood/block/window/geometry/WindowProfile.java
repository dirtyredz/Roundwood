package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.geometry.PixelCircle;

/**
 * The three single-cell window faces: which pixels of a square wooden pane are glazed. The round
 * window is a centred 12-pixel circle; the arches are the two halves of an upper semicircle
 * ({@link WindowArchGeometry}). Pixels are (u, v), u to the right and v up as seen from the front.
 */
public enum WindowProfile {
    ROUND("round_window"),
    ARCH_LEFT("round_window_arch_left"),
    ARCH_RIGHT("round_window_arch_right");

    public static final int SIZE = 16;
    /** The round window's opening (unchanged since 0.3). */
    public static final int ROUND_OPENING = 12;

    private final String id;

    WindowProfile(String id) {
        this.id = id;
    }

    /** The registry path of the block with this face (also the stem of its model names). */
    public String id() {
        return id;
    }

    public boolean inOpening(int u, int v) {
        return switch (this) {
            case ROUND -> PixelCircle.inDisc(SIZE, ROUND_OPENING, u, v);
            case ARCH_LEFT -> WindowArchGeometry.inOpening(false, u, v);
            case ARCH_RIGHT -> WindowArchGeometry.inOpening(true, u, v);
        };
    }
}
