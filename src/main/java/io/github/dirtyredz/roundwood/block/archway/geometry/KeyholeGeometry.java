package io.github.dirtyredz.roundwood.block.archway.geometry;

/**
 * The keyhole archway's opening (0.12.0): the ordinary archway's radius-14 crown over its spring line,
 * then shoulders that step each side in by {@link #SHOULDER} pixels over the next {@link #DROP}
 * pixels down, leaving a {@link #LOWER_WIDTH}-pixel passage ({@code x 6..26}) from {@code y 8} to
 * the floor. The edge moves in a straight line between the two, and a pixel belongs to the opening
 * when its centre lies strictly between the two edges (the {@code PixelCircle} pixel-centre
 * convention), so the shoulders rasterise as a mirror-symmetric two-pixel staircase. Plain Java.
 */
public final class KeyholeGeometry {
    /** How far each side steps in below the spring line, in pixels. */
    public static final int SHOULDER = 4;
    /** Over how many pixels of height the shoulder steps in. */
    public static final int DROP = 8;
    /** The lower passage's left edge. */
    public static final int LOWER_LEFT = ArchwayDesign.OPENING_LEFT + SHOULDER;
    /** The lower passage's width. */
    public static final int LOWER_WIDTH = ArchwayDesign.SIZE - 2 * LOWER_LEFT;
    /** The height below which the passage is its full lower width. */
    public static final int SHOULDER_FOOT = ArchwayDesign.SPRING - DROP;

    private KeyholeGeometry() {
    }

    /** True when pixel (u, v)'s centre lies inside the keyhole opening. */
    public static boolean inOpening(int u, int v) {
        double y = v + 0.5;
        if (y >= ArchwayDesign.SPRING) {
            return ArchwayDesign.OPENING.contains(u, v);
        }
        double x = u + 0.5;
        double left = leftEdge(y);
        return y >= 0 && x > left && x < ArchwayDesign.SIZE - left;
    }

    /** The opening's left edge at height {@code y} below the spring line, in pixels. */
    public static double leftEdge(double y) {
        if (y <= SHOULDER_FOOT) {
            return LOWER_LEFT;
        }
        return ArchwayDesign.OPENING_LEFT + (ArchwayDesign.SPRING - y) * SHOULDER / (double) DROP;
    }
}
