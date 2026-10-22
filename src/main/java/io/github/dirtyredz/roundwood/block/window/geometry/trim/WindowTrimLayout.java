package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;

/**
 * Which whole-face artwork a window attachment's masonry face is drawn from (0.10.0): an explicit
 * layout per profile and slot, so a piece never inherits artwork whose courses do not fit its band.
 * The artwork spans {@link WindowProfile#assemblySize()} ({@link WindowTrimUv}); the client binds
 * each layout to its generated textures. Plain Java.
 * <ul>
 *     <li>{@link #ROUND}: the round window's 32 x 32 radial ring;</li>
 *     <li>{@link #ARCH}: the 2 x 2 window, radial above the spring line and straight jambs and sill
 *     below (upper arches, bottom corners; unchanged since 0.7.0);</li>
 *     <li>{@link #CIRCLE}: the full radial circle (lower arches: the {@link #ARCH} art has straight
 *     courses below its spring line, so it cannot draw a lower circular quarter);</li>
 *     <li>{@link #FRAME}: straight courses with a head on top (top corners);</li>
 *     <li>{@link #EDGE}: one straight 16-pixel run per side (single-edge pieces);</li>
 *     <li>{@link #SILL}: one horizontal course (the arch sill).</li>
 * </ul>
 */
public enum WindowTrimLayout {
    ROUND, ARCH, CIRCLE, FRAME, EDGE, SILL;

    public static WindowTrimLayout of(TrimSlot slot, WindowProfile profile) {
        if (slot == TrimSlot.SILL) {
            return SILL;
        }
        return switch (profile) {
            case ROUND -> ROUND;
            case ARCH_LEFT, ARCH_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT -> ARCH;
            case ARCH_BOTTOM_LEFT, ARCH_BOTTOM_RIGHT -> CIRCLE;
            case TOP_LEFT, TOP_RIGHT -> FRAME;
            case EDGE_TOP, EDGE_LEFT, EDGE_BOTTOM, EDGE_RIGHT -> EDGE;
        };
    }
}
