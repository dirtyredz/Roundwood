package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowMuntinGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowSillGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import org.jetbrains.annotations.Nullable;

/**
 * What a point on a window's face is (0.8.0): the one classification every window click is routed
 * by. Face coordinates are pixels {@code (u, v)} as seen from the front ({@code 0..16}), read on the
 * existing rasters: the profile's opening, the cross beams (half pixels, {@code 7.5..8.5}, always
 * frame), the perimeter trim band and (0.10.0) the arch sill. An attachment wins only when the face
 * being clicked carries it, so an empty face stays editable as frame and glass; the band lies on frame
 * wood and the sill inside the opening, so the two never compete.
 *
 * <p>Boundaries are defined once: callers nudge a surface hit slightly into the surface; lookups are
 * half-open, except that the exact outer edge ({@code 16}) belongs to the last cell. Points outside
 * the face are {@link Region#NONE}, never clamped into it. Plain Java.
 */
public final class WindowHitRegions {
    public enum Region {
        /** The installed perimeter trim band on the clicked face. */
        TRIM,
        /** The installed arch sill on the clicked face (0.10.0). */
        SILL,
        /** Frame wood, beams included. */
        FRAME,
        /** The glazing. */
        GLASS,
        /** A centred pane's connector arm (glass outside the slab). */
        CONNECTOR_GLASS,
        /** Nothing a window action applies to. */
        NONE;

        /** The attachment slot this region is, or null when it is no attachment. */
        @Nullable
        public TrimSlot slot() {
            return this == TRIM ? TrimSlot.PERIMETER : this == SILL ? TrimSlot.SILL : null;
        }
    }

    private static final int SIZE = WindowProfile.SIZE;

    private WindowHitRegions() {
    }

    /** {@link #classify(WindowDesign, double, double, boolean, boolean)} on a face without a sill. */
    public static Region classify(WindowDesign design, double u, double v, boolean trimOnFace) {
        return classify(design, u, v, trimOnFace, false);
    }

    /**
     * @param u, v       the point on the face, pixels as seen from the front
     * @param trimOnFace whether the clicked face carries a perimeter trim
     * @param sillOnFace whether the clicked face carries an arch sill
     */
    public static Region classify(WindowDesign design, double u, double v, boolean trimOnFace, boolean sillOnFace) {
        if (!(u >= 0 && u <= SIZE && v >= 0 && v <= SIZE)) {
            return Region.NONE;
        }
        int hu = half(u);
        int hv = half(v);
        if (trimOnFace && WindowTrimGeometry.inBand(design.profile(), hu / 2, hv / 2)) {
            return Region.TRIM;
        }
        if (sillOnFace && WindowSillGeometry.inSill(design.profile(), hu / 2, hv / 2)) {
            return Region.SILL;
        }
        if (WindowMuntinGeometry.isBeam(design, hu, hv)) {
            return Region.FRAME;
        }
        return design.profile().inOpening(hu / 2, hv / 2) ? Region.GLASS : Region.FRAME;
    }

    /** The half-pixel cell (0..31) of a coordinate in 0..16, the outer edge admitted into the last cell. */
    private static int half(double pixels) {
        return Math.min(2 * SIZE - 1, (int) Math.floor(pixels * 2));
    }
}
