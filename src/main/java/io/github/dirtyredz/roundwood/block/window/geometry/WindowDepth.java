package io.github.dirtyredz.roundwood.block.window.geometry;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

/**
 * Where a pane's two-pixel slab sits along its normal, measured along the normal's positive world
 * axis (east, up, south): mid-cell like a glass pane, or flush against the cell's negative or
 * positive side. Also the arch windows' {@code depth} blockstate value.
 */
public enum WindowDepth implements StringRepresentable {
    CENTER("center", 7),
    NEGATIVE("negative", 0),
    POSITIVE("positive", 14);

    /** The wooden slab's thickness; the glass is recessed half a pixel on each face. */
    public static final double SLAB = 2;
    public static final double GLASS_INSET = 0.5;

    private final String name;
    private final double slabMin;

    WindowDepth(String name, double slabMin) {
        this.name = name;
        this.slabMin = slabMin;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The slab's lower coordinate along the normal's positive axis, in pixels. */
    public double slabMin() {
        return slabMin;
    }

    public double slabMax() {
        return slabMin + SLAB;
    }

    /** The cell side a flush slab lies against on {@code normal}, or empty when centred. */
    public Optional<Direction> side(Direction.Axis normal) {
        return switch (this) {
            case CENTER -> Optional.empty();
            case NEGATIVE -> Optional.of(Direction.fromAxisAndDirection(normal, Direction.AxisDirection.NEGATIVE));
            case POSITIVE -> Optional.of(Direction.fromAxisAndDirection(normal, Direction.AxisDirection.POSITIVE));
        };
    }

    /** The depth of a slab flush against {@code side}. */
    public static WindowDepth flushAgainst(Direction side) {
        return side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? POSITIVE : NEGATIVE;
    }
}
