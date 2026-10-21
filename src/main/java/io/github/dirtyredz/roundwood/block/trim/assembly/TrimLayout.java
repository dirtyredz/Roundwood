package io.github.dirtyredz.roundwood.block.trim.assembly;

import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
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

    /** One frame: its anchor (lower-left) cell and the direction the placing player faced. */
    public record Assembly(BlockPos anchor, Direction facing) {
        public BlockPos pos(TrimPart part) {
            return anchor.relative(facing.getClockWise(), part.column).above(part.row);
        }

        public List<BlockPos> positions() {
            List<BlockPos> out = new ArrayList<>(4);
            for (TrimPart part : TrimPart.values()) {
                out.add(pos(part));
            }
            return out;
        }

        public BlockState stateFor(TrimPart part, Block block) {
            return block.defaultBlockState().setValue(RoundTrimBlock.FACING, facing).setValue(RoundTrimBlock.PART, part);
        }

        public boolean isPart(BlockState state, Block block, TrimPart part) {
            return state.is(block) && state.getValue(RoundTrimBlock.FACING) == facing
                    && state.getValue(RoundTrimBlock.PART) == part;
        }

        public boolean loaded(Level level) {
            for (BlockPos pos : positions()) {
                if (!level.isLoaded(pos)) {
                    return false;
                }
            }
            return true;
        }

        /** The quarters currently present and matching; never reads an unloaded cell. */
        public List<TrimPart> presentParts(Level level, Block block) {
            List<TrimPart> present = new ArrayList<>(4);
            for (TrimPart part : TrimPart.values()) {
                BlockPos pos = pos(part);
                if (level.isLoaded(pos) && isPart(level.getBlockState(pos), block, part)) {
                    present.add(part);
                }
            }
            return present;
        }

        public boolean complete(Level level, Block block) {
            return presentParts(level, block).size() == TrimPart.values().length;
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
