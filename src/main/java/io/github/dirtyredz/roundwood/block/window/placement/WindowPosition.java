package io.github.dirtyredz.roundwood.block.window.placement;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

/**
 * The round window's {@code position} blockstate value: centred in the cell (its {@code axis} then
 * picks the plane, as before 0.6.0, and states saved without a position load as centred), or flush
 * against one of the cell's six sides, which alone decides the plane.
 */
public enum WindowPosition implements StringRepresentable {
    CENTER("center", null),
    NORTH("north", Direction.NORTH),
    EAST("east", Direction.EAST),
    SOUTH("south", Direction.SOUTH),
    WEST("west", Direction.WEST),
    UP("up", Direction.UP),
    DOWN("down", Direction.DOWN);

    private final String name;
    private final Direction side;

    WindowPosition(String name, Direction side) {
        this.name = name;
        this.side = side;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The cell side the pane is flush against; empty when centred. */
    public Optional<Direction> side() {
        return Optional.ofNullable(side);
    }

    public static WindowPosition flush(Direction side) {
        for (WindowPosition position : values()) {
            if (position.side == side) {
                return position;
            }
        }
        throw new IllegalArgumentException(String.valueOf(side));
    }

    /**
     * The pose of a round window. Centred: upright across {@code axis} (axis x is the pane in the x-y
     * plane, the 0.5.0 meaning). Flush: the slab against its side, the front pointing into the cell
     * (the round face looks the same from both sides, so the front only fixes the frame), with the
     * top north for floor and ceiling panes.
     */
    public WindowPose pose(Direction.Axis axis) {
        if (side == null) {
            return WindowPose.upright(axis == Direction.Axis.X ? Direction.SOUTH : Direction.WEST, WindowDepth.CENTER);
        }
        if (side.getAxis().isHorizontal()) {
            return WindowPose.upright(side.getOpposite(), WindowDepth.flushAgainst(side));
        }
        return new WindowPose(side.getOpposite(), Direction.NORTH, WindowDepth.flushAgainst(side));
    }
}
