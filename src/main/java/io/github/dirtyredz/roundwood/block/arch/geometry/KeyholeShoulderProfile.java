package io.github.dirtyredz.roundwood.block.arch.geometry;

import net.minecraft.util.StringRepresentable;

/** Only the historical corbel transition; no hidden strip survives below row eight. */
public final class KeyholeShoulderProfile {
    private static final int[] WIDTHS = {0, 0, 0, 0, 0, 0, 0, 0, 6, 5, 5, 4, 4, 3, 3, 2};

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
