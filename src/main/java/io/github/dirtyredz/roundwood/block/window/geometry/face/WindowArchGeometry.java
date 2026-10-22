package io.github.dirtyredz.roundwood.block.window.geometry.face;

import io.github.dirtyredz.roundwood.geometry.PixelCircle;

/**
 * The arch windows: the quarters of one circle two blocks wide. The two upper arches together are
 * the upper half of that circle, authored over {@code u 0..32, v 0..16} as seen from the front around
 * the middle of its bottom edge {@code (16, 0)}: {@link #OUTER} pixels across, glazed inside a
 * {@link #OPENING}-pixel circle (the round window's two-pixel border). Each piece is one 16-pixel
 * half (left {@code u 0..16}, right {@code 16..32}): a square wooden pane whose glass is a quarter
 * circle in its bottom inner corner. No mullion between them and nothing below the diameter.
 *
 * <p>The lower arches (0.10.0) are the upper ones reflected top to bottom: pixel {@code v} of a lower
 * piece is pixel {@code 15 - v} of the upper piece on the same side. Placed under the upper pair they
 * glaze the full {@link #OPENING}-pixel disc of a 32 x 32 window ({@code PixelCircle.inDisc(32, 28)}).
 * Pixels belong by their centre, like {@link PixelCircle}. Plain Java.
 */
public final class WindowArchGeometry {
    public static final int OUTER = 32;
    public static final int OPENING = OUTER - 4;
    public static final double CENTRE_U = 16;
    public static final double CENTRE_V = 0;

    private WindowArchGeometry() {
    }

    /** True when pixel (u, v) of the combined 32 x 16 upper face is glazed. */
    public static boolean inCombinedOpening(int u, int v) {
        return PixelCircle.inCircle(CENTRE_U, CENTRE_V, OPENING, u, v);
    }

    /** True when pixel (u, v) of one upper piece's face is glazed ({@code right}: the piece at u 16..32). */
    public static boolean inOpening(boolean right, int u, int v) {
        return inCombinedOpening(right ? u + 16 : u, v);
    }

    /** True when pixel (u, v) of one lower piece's face is glazed: the upper piece reflected top to bottom. */
    public static boolean inLowerOpening(boolean right, int u, int v) {
        return inOpening(right, u, WindowProfile.SIZE - 1 - v);
    }
}
