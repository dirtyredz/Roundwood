package io.github.dirtyredz.roundwood.block.window.geometry.face;

/**
 * The square corner pieces. The two bottom corners (0.7.0) are the lower half of a two-block window
 * whose upper half is the two arches ({@link WindowArchGeometry}): each is a square wooden pane
 * glazed everywhere except a two-pixel outer jamb and a two-pixel sill, which meet in a square
 * corner; there is no top border and no inner border, so with the arches above them the four pieces
 * read as one 32 x 32 window with a continuous frame: the lower opening {@code x 2..30, y 2..16}
 * below the arch's radius-14 upper semicircle. The two top corners (0.10.0) are the bottom ones
 * reflected top to bottom: an outer jamb and a two-pixel head. Pixels are (u, v), u to the right and
 * v up as seen from the front. Plain Java.
 */
public final class WindowCornerGeometry {
    /** The jamb, sill and head width, matching the arches' two-pixel border. */
    public static final int BORDER = 2;

    private WindowCornerGeometry() {
    }

    /** True when pixel (u, v) of a bottom corner's face is glazed ({@code right}: the piece at u 16..32). */
    public static boolean inOpening(boolean right, int u, int v) {
        if (v < BORDER) {
            return false;
        }
        return right ? u < WindowProfile.SIZE - BORDER : u >= BORDER;
    }

    /** True when pixel (u, v) of a bottom corner's face is its outer jamb or sill (the wood). */
    public static boolean inFrame(boolean right, int u, int v) {
        return !inOpening(right, u, v);
    }

    /** True when pixel (u, v) of a top corner's face is glazed: the bottom corner reflected top to bottom. */
    public static boolean inTopOpening(boolean right, int u, int v) {
        return inOpening(right, u, WindowProfile.SIZE - 1 - v);
    }

    /** True when pixel (u, v) of a top corner's face is its outer jamb or head. */
    public static boolean inTopFrame(boolean right, int u, int v) {
        return !inTopOpening(right, u, v);
    }
}
