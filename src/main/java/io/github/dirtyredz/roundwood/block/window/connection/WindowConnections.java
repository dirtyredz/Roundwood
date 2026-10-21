package io.github.dirtyredz.roundwood.block.window.connection;

import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Which of the four horizontal pane connections a window keeps, from its pose. The in-plane sides of
 * a centred upright window follow vanilla (as in 0.5.0; those connections add no geometry). Every
 * other side (across a centred window's plane, and all four sides of a flush or floor/ceiling
 * window, which grow glass towards the cell's middle) connects only to a genuine pane junction: a
 * pane or bars, a wall, or a window, but not a window face to face with this one (both upright on
 * the same normal, across it) and not one continuing this pane in the same plane (nothing to bridge).
 * A solid block never makes glass grow.
 */
public final class WindowConnections {
    private WindowConnections() {
    }

    /** True when vanilla's rule decides {@code side}: the in-plane sides of a centred upright pane. */
    public static boolean vanillaSide(WindowPose pose, Direction side) {
        return pose.upright() && pose.depth() == WindowDepth.CENTER && side.getAxis() != pose.normal();
    }

    /** Whether a window posed {@code pose} may connect towards {@code side} to {@code neighbour} (outside the vanilla sides). */
    public static boolean joins(WindowPose pose, Direction side, BlockState neighbour) {
        if (neighbour.getBlock() instanceof WindowPaneBlock other) {
            WindowPose theirs = other.pose(neighbour);
            if (pose.upright() && theirs.upright() && pose.normal() == theirs.normal()) {
                return side.getAxis() != pose.normal() && pose.depth() != theirs.depth();
            }
            return pose.upright() || theirs.upright() || pose.depth() != theirs.depth();
        }
        return neighbour.getBlock() instanceof IronBarsBlock || neighbour.is(BlockTags.WALLS);
    }

    /** The window state with its connection on {@code side} re-judged against {@code neighbour}. */
    public static BlockState filter(BlockState window, Direction side, BlockState neighbour) {
        if (!side.getAxis().isHorizontal() || !(window.getBlock() instanceof WindowPaneBlock block)) {
            return window;
        }
        WindowPose pose = block.pose(window);
        if (vanillaSide(pose, side)) {
            return window;
        }
        BooleanProperty property = PipeBlock.PROPERTY_BY_DIRECTION.get(side);
        return window.setValue(property, window.getValue(property) && joins(pose, side, neighbour));
    }

    /** Re-judges every side of a window about to be placed at {@code pos}. */
    public static BlockState filterAll(BlockState window, BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            window = filter(window, side, level.getBlockState(pos.relative(side)));
        }
        return window;
    }
}
