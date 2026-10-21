package io.github.dirtyredz.roundwood.block.window.geometry.face;

/**
 * The cross beams of a {@link WindowPattern#CROSS} piece: one pixel wide, a vertical and a horizontal
 * beam through the middle of the piece ({@code u, v 7.5..8.5}), clipped to its own opening. Every
 * piece has its own cross (owner ruling, 0.7.0): the round window, each arch and each bottom corner,
 * so four crossed pieces show four crosses, never one shared cross. Beams are wood at the pane's full
 * depth; the glass is cut away where they run. Coordinates are half pixels (0..31), the grid the pane
 * geometry is built on. Plain Java.
 */
public final class WindowMuntinGeometry {
    /** The half-pixel columns and rows the beams occupy ({@code 7.5..8.5}). */
    public static final int BEAM_LOW = 15;
    public static final int BEAM_HIGH = 16;

    private WindowMuntinGeometry() {
    }

    /** True when half-pixel cell (hu, hv) lies on the cross's vertical or horizontal beam (unclipped). */
    public static boolean onCross(int hu, int hv) {
        return onBeam(hu) || onBeam(hv);
    }

    /** True when half-pixel cell (hu, hv) of {@code design} is a beam: on its cross and inside its opening. */
    public static boolean isBeam(WindowDesign design, int hu, int hv) {
        return design.pattern() == WindowPattern.CROSS && onCross(hu, hv) && design.profile().inOpening(hu / 2, hv / 2);
    }

    private static boolean onBeam(int h) {
        return h == BEAM_LOW || h == BEAM_HIGH;
    }
}
