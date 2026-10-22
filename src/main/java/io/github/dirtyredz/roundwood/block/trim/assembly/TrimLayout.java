package io.github.dirtyredz.roundwood.block.trim.assembly;

import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Where the four quarters of a frame are. Like the door: the anchor is the lower-left cell seen
 * from the front (the placing player's side, looking along {@code facing}), the right column one
 * block to the right ({@code facing}'s clockwise neighbour), the upper row one block up. A quarter's
 * partners are only the cells this names, holding the same block, facing and the matching part, so
 * two frames side by side never merge.
 */
public final class TrimLayout {
    private TrimLayout() {
    }

    /** The four quarters in the shared part order. */
    private static final List<TrimPart> PARTS = List.of(TrimPart.values());

    /**
     * One frame: its anchor (lower-left) cell and the direction the placing player faced. The shared
     * traversal is the {@link io.github.dirtyredz.roundwood.block.assembly.Assembly} contract's.
     */
    public record Assembly(BlockPos anchor, Direction facing) implements io.github.dirtyredz.roundwood.block.assembly.Assembly<TrimPart> {
        @Override
        public List<TrimPart> parts() {
            return PARTS;
        }

        @Override
        public BlockPos pos(TrimPart part) {
            return anchor.relative(facing.getClockWise(), part.column).above(part.row);
        }

        public BlockState stateFor(TrimPart part, Block block) {
            return block.defaultBlockState().setValue(RoundTrimBlock.FACING, facing).setValue(RoundTrimBlock.PART, part);
        }

        /** Same block, facing and quarter. */
        @Override
        public boolean isPart(BlockState state, Block block, TrimPart part) {
            return state.is(block) && state.getValue(RoundTrimBlock.FACING) == facing
                    && state.getValue(RoundTrimBlock.PART) == part;
        }
    }

    /** The frame a quarter belongs to, from its own state. */
    public static Assembly of(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(RoundTrimBlock.FACING);
        TrimPart part = state.getValue(RoundTrimBlock.PART);
        BlockPos anchor = pos.below(part.row).relative(facing.getCounterClockWise(), part.column);
        return new Assembly(anchor.immutable(), facing);
    }
}
