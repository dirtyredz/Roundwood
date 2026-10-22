package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;

/**
 * The arch sill (0.10.0): a separately fitted bar across the bottom of an upper arch's opening, one
 * face at a time ({@code TrimSlot.SILL}). It covers the bottom {@link #ROWS} pixel rows <em>inside</em>
 * the profile's opening, so it never overlaps the curved perimeter band on the same face (the two may
 * be fitted together in different looks without coplanar faces), and it stands
 * {@code AttachmentFace.PROUD} pixel proud at the perimeter band's depth. It is decorative: it
 * lies over the glazing and the beam ends without changing the pane, its glass or its tint. Only
 * profiles that {@link WindowProfile#takesSill() take a sill} have one. Plain Java.
 */
public final class WindowSillGeometry {
    /** The sill's height, in pixels. */
    public static final int ROWS = 2;

    private WindowSillGeometry() {
    }

    /** True when pixel (u, v) of {@code profile}'s face is under the sill. */
    public static boolean inSill(WindowProfile profile, int u, int v) {
        return profile.takesSill() && v < ROWS && profile.inOpening(u, v);
    }
}
