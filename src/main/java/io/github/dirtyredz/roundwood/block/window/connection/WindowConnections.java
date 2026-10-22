package io.github.dirtyredz.roundwood.block.window.connection;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
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
 * a centred upright window follow vanilla (as in 0.5.0; those connections add no geometry). A side
 * across a centred window's plane, which grows a glass arm to the cell edge, connects only where
 * that arm meets something at the middle of the edge: a pane or bars, a wall, or another centred
 * upright window crossing this one (its slab reaches the edge there). Never a window face to face
 * with this one, never a positioned window (its slab is not at the middle of the edge), never a
 * solid block. A positioned window (flush, floor or ceiling) grows no glass, so it keeps no
 * connection at all: where two flush windows meet, their frames simply abut (0.6.1).
 */
public final class WindowConnections {
    private WindowConnections() {
    }

    /** True when vanilla's rule decides {@code side}: the in-plane sides of a centred upright pane. */
    public static boolean vanillaSide(WindowPose pose, Direction side) {
        return WindowPaneGeometry.bridges(pose) && side.getAxis() != pose.normal();
    }

    /** Whether a window posed {@code pose} may connect towards {@code side} to {@code neighbour} (outside the vanilla sides). */
    public static boolean joins(WindowPose pose, Direction side, BlockState neighbour) {
        if (!WindowPaneGeometry.neededSides(pose).contains(side)) {
            return false;
        }
        if (neighbour.getBlock() instanceof WindowPaneBlock other) {
            WindowPose theirs = other.pose(neighbour);
            return WindowPaneGeometry.bridges(theirs) && theirs.normal() != pose.normal();
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
