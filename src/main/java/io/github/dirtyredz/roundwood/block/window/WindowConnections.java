package io.github.dirtyredz.roundwood.block.window;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Which pane connections a window keeps. In its own plane it connects like any pane (vanilla rules;
 * those connections add no geometry). Across the plane, on its front and back faces, vanilla would
 * also connect to any sturdy full block, growing a glass arm towards a wall standing in front of the
 * window. There only a genuine pane junction counts: a pane or bars, a wall (which reaches towards
 * panes itself), or a window whose plane crosses this one. A window parallel to this one is face to
 * face, not a junction.
 */
public final class WindowConnections {
    private WindowConnections() {
    }

    /** True when {@code side} leaves the window through its front or back face. */
    public static boolean acrossPlane(BlockState window, Direction side) {
        return side.getAxis().isHorizontal() && side.getAxis() != window.getValue(RoundWindowBlock.AXIS);
    }

    /** Whether a window may connect across its plane to {@code neighbour}. */
    public static boolean joinsAcrossPlane(BlockState window, BlockState neighbour) {
        if (neighbour.getBlock() instanceof RoundWindowBlock) {
            return neighbour.getValue(RoundWindowBlock.AXIS) != window.getValue(RoundWindowBlock.AXIS);
        }
        return neighbour.getBlock() instanceof IronBarsBlock || neighbour.is(BlockTags.WALLS);
    }

    /** The window state with its across-plane connection on {@code side} re-judged against {@code neighbour}. */
    public static BlockState filter(BlockState window, Direction side, BlockState neighbour) {
        if (!acrossPlane(window, side)) {
            return window;
        }
        BooleanProperty property = PipeBlock.PROPERTY_BY_DIRECTION.get(side);
        return window.setValue(property, window.getValue(property) && joinsAcrossPlane(window, neighbour));
    }

    /** Re-judges both across-plane connections of a window about to be placed at {@code pos}. */
    public static BlockState filterAll(BlockState window, BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            window = filter(window, side, level.getBlockState(pos.relative(side)));
        }
        return window;
    }
}
