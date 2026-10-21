package io.github.dirtyredz.roundwood.block.window.geometry.face;

import io.github.dirtyredz.roundwood.geometry.PixelCircle;

/**
 * The two arch windows together: the upper half of a round window two blocks wide. The combined face
 * is authored over {@code u 0..32, v 0..16} as seen from the front, around one circle centred on the
 * middle of its bottom edge {@code (16, 0)}: {@link #OUTER} pixels across, glazed inside a
 * {@link #OPENING}-pixel circle (the round window's two-pixel border). Each piece is one 16-pixel
 * half (left {@code u 0..16}, right {@code 16..32}): a square wooden pane whose glass is a quarter
 * circle in its bottom inner corner. No mullion between them and nothing below the diameter. Pixels
 * belong by their centre, like {@link PixelCircle}. Plain Java.
 */
public final class WindowArchGeometry {
    public static final int OUTER = 32;
    public static final int OPENING = OUTER - 4;
    public static final double CENTRE_U = 16;
    public static final double CENTRE_V = 0;

    private WindowArchGeometry() {
    }

    /** True when pixel (u, v) of the combined 32 x 16 face is glazed. */
    public static boolean inCombinedOpening(int u, int v) {
        return PixelCircle.inCircle(CENTRE_U, CENTRE_V, OPENING, u, v);
    }

    /** True when pixel (u, v) of one piece's face is glazed ({@code right}: the piece at u 16..32). */
    public static boolean inOpening(boolean right, int u, int v) {
        return inCombinedOpening(right ? u + 16 : u, v);
    }
}
