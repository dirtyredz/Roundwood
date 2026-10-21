package io.github.dirtyredz.roundwood.block.window.placement;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * An arch window's {@code orientation} blockstate value: which way its front faces and which way is
 * the top of its artwork. Four upright fronts (top up), and a front facing up or down with each of
 * the four horizontal tops. With {@code depth} it resolves to a {@link WindowPose}; it is stored so
 * the piece keeps its handedness whatever the camera does later.
 */
public enum WindowOrientation implements StringRepresentable {
    NORTH(Direction.NORTH, Direction.UP),
    EAST(Direction.EAST, Direction.UP),
    SOUTH(Direction.SOUTH, Direction.UP),
    WEST(Direction.WEST, Direction.UP),
    UP_NORTH(Direction.UP, Direction.NORTH),
    UP_EAST(Direction.UP, Direction.EAST),
    UP_SOUTH(Direction.UP, Direction.SOUTH),
    UP_WEST(Direction.UP, Direction.WEST),
    DOWN_NORTH(Direction.DOWN, Direction.NORTH),
    DOWN_EAST(Direction.DOWN, Direction.EAST),
    DOWN_SOUTH(Direction.DOWN, Direction.SOUTH),
    DOWN_WEST(Direction.DOWN, Direction.WEST);

    private final Direction front;
    private final Direction top;

    WindowOrientation(Direction front, Direction top) {
        this.front = front;
        this.top = top;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Direction front() {
        return front;
    }

    public Direction top() {
        return top;
    }

    public WindowPose pose(WindowDepth depth) {
        return new WindowPose(front, top, depth);
    }

    public static WindowOrientation of(Direction front, Direction top) {
        for (WindowOrientation orientation : values()) {
            if (orientation.front == front && orientation.top == top) {
                return orientation;
            }
        }
        throw new IllegalArgumentException("no orientation with front " + front + " and top " + top);
    }

    /** The orientation and depth of {@code pose} (always representable: upright, or front up/down). */
    public static WindowOrientation of(WindowPose pose) {
        return of(pose.front(), pose.top());
    }
}
