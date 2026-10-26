package io.github.dirtyredz.roundwood.block.arch.geometry;

import net.minecraft.util.StringRepresentable;

/**
 * The historical corbel transition thinned by the two pixels the flush-spring arch gained: a four-pixel base
 * to the floor, stepping to nothing at the seam (the top's bottom rows are zero-width there too).
 */
public final class KeyholeShoulderProfile {
    private static final int[] WIDTHS = {4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1, 0};

    public enum Side implements StringRepresentable {
        LEFT, RIGHT;
        public Side mirrored() { return this == LEFT ? RIGHT : LEFT; }
        @Override public String getSerializedName() { return this == LEFT ? "left" : "right"; }
    }

    private KeyholeShoulderProfile() {}

    public static boolean contains(Side side, int u, int v) {
        if (u < 0 || u >= 16 || v < 0 || v >= 16) return false;
        return (side == Side.LEFT ? u : 15 - u) < WIDTHS[v];
    }
}
