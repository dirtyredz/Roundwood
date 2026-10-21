package io.github.dirtyredz.roundwood.block.window.geometry.face;

/**
 * The two bottom corner pieces (0.7.0): the lower half of a two-block window whose upper half is the
 * two arches ({@link WindowArchGeometry}). Each piece is a square wooden pane glazed everywhere except
 * a two-pixel outer jamb and a two-pixel sill, which meet in a square corner; there is no top border
 * and no inner border, so with the arches above them the four pieces read as one 32 x 32 window with
 * a continuous frame: the lower opening {@code x 2..30, y 2..16} below the arch's radius-14 upper
 * semicircle. Pixels are (u, v), u to the right and v up as seen from the front. Plain Java.
 */
public final class WindowCornerGeometry {
    /** The jamb and sill width, matching the arches' two-pixel border. */
    public static final int BORDER = 2;

    private WindowCornerGeometry() {
    }

    /** True when pixel (u, v) of one corner's face is glazed ({@code right}: the piece at u 16..32). */
    public static boolean inOpening(boolean right, int u, int v) {
        if (v < BORDER) {
            return false;
        }
        return right ? u < WindowProfile.SIZE - BORDER : u >= BORDER;
    }

    /** True when pixel (u, v) of a corner's face is its outer jamb or sill (the wood). */
    public static boolean inFrame(boolean right, int u, int v) {
        return !inOpening(right, u, v);
    }
}
