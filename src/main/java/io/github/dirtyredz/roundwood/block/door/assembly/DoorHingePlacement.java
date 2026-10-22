package io.github.dirtyredz.roundwood.block.door.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Picks the leaf's hinge the way vanilla {@code DoorBlock.getHinge} does, but judged at the two
 * outside edges of the assembly's actual footprint (two columns for the round door, one for the
 * arched door since 0.11.0):
 * <ol>
 *     <li>a door of the same block standing just outside one edge puts the hinge on the other edge
 *     (a pair of doors then opens from the middle, as vanilla pairs do);</li>
 *     <li>otherwise the edge with more full blocks beside it (lower and upper row) gets the hinge;</li>
 *     <li>on a tie, the half of the clicked cell the player aimed at: left half, left hinge.</li>
 * </ol>
 * The footprint never changes: the clicked cell is always the lower-left part.
 */
public final class DoorHingePlacement {
    private DoorHingePlacement() {
    }

    /** @param columns the footprint's width in cells: the outside-right cell is that far right of the anchor */
    public static DoorHingeSide choose(Block door, BlockPlaceContext context, BlockPos anchor, Direction facing, int columns) {
        BlockGetter level = context.getLevel();
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        BlockPos outsideLeft = anchor.relative(left);
        BlockPos outsideRight = anchor.relative(right, columns);

        int solid = (full(level, outsideLeft) ? -1 : 0) + (full(level, outsideLeft.above()) ? -1 : 0)
                + (full(level, outsideRight) ? 1 : 0) + (full(level, outsideRight.above()) ? 1 : 0);
        boolean doorLeft = lowerDoor(level.getBlockState(outsideLeft), door);
        boolean doorRight = lowerDoor(level.getBlockState(outsideRight), door);

        if (doorLeft && !doorRight || solid > 0) {
            return DoorHingeSide.RIGHT;
        }
        if (doorRight && !doorLeft || solid < 0) {
            return DoorHingeSide.LEFT;
        }
        return aimedHalf(context, anchor, facing);
    }

    /** Vanilla's tie-break, measured in the clicked (anchor) cell: which half of it the player aimed at. */
    static DoorHingeSide aimedHalf(BlockPlaceContext context, BlockPos anchor, Direction facing) {
        Vec3 hit = context.getClickLocation();
        double dx = hit.x - anchor.getX();
        double dz = hit.z - anchor.getZ();
        // Distance along the assembly's left-to-right axis (the facing's clockwise direction), 0..1.
        Direction across = facing.getClockWise();
        double along = across.getStepX() != 0
                ? (across.getStepX() > 0 ? dx : 1 - dx)
                : (across.getStepZ() > 0 ? dz : 1 - dz);
        return along > 0.5 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
    }

    private static boolean full(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    private static boolean lowerDoor(BlockState state, Block door) {
        return state.is(door) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }
}
