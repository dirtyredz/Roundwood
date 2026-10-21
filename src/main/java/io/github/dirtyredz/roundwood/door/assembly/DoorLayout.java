package io.github.dirtyredz.roundwood.door.assembly;

import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the four parts of an assembly are. A part's identity is its {@code column} and {@code half};
 * the anchor (lower-left part) is found from those alone. The physical {@code hinge} is shared by all
 * four parts and only says which outer edge the single leaf turns on.
 *
 * <p>Seen from the front (the side the placing player stood on, looking along {@code facing}), the
 * left column is at the anchor and the right column one block to the right ({@code facing}'s
 * clockwise neighbour).
 */
public final class DoorLayout {
    private DoorLayout() {
    }

    public enum Part {
        LOWER_LEFT(DoorColumn.LEFT, DoubleBlockHalf.LOWER),
        UPPER_LEFT(DoorColumn.LEFT, DoubleBlockHalf.UPPER),
        LOWER_RIGHT(DoorColumn.RIGHT, DoubleBlockHalf.LOWER),
        UPPER_RIGHT(DoorColumn.RIGHT, DoubleBlockHalf.UPPER);

        public final DoorColumn column;
        public final DoubleBlockHalf half;

        Part(DoorColumn column, DoubleBlockHalf half) {
            this.column = column;
            this.half = half;
        }

        public static Part of(DoorColumn column, DoubleBlockHalf half) {
            if (column == DoorColumn.LEFT) {
                return half == DoubleBlockHalf.LOWER ? LOWER_LEFT : UPPER_LEFT;
            }
            return half == DoubleBlockHalf.LOWER ? LOWER_RIGHT : UPPER_RIGHT;
        }

        /** 0 for the lower row, 1 for the upper. */
        public int row() {
            return half == DoubleBlockHalf.LOWER ? 0 : 1;
        }
    }

    /** One assembly: the anchor cell, the direction the placing player faced, and the leaf's hinge. */
    public record Assembly(BlockPos anchor, Direction facing, DoorHingeSide hinge) {
        public BlockPos pos(Part part) {
            BlockPos pos = part.column == DoorColumn.LEFT ? anchor : anchor.relative(facing.getClockWise());
            return part.half == DoubleBlockHalf.LOWER ? pos : pos.above();
        }

        public List<BlockPos> positions() {
            List<BlockPos> out = new ArrayList<>(4);
            for (Part part : Part.values()) {
                out.add(pos(part));
            }
            return out;
        }

        /** The block state a part should have, open/powered as given; keeps any injected properties. */
        public BlockState stateFor(Part part, Block block, boolean open, boolean powered) {
            return block.defaultBlockState()
                    .setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.HINGE, hinge)
                    .setValue(RoundDoorBlock.COLUMN, part.column)
                    .setValue(DoorBlock.HALF, part.half)
                    .setValue(DoorBlock.OPEN, open)
                    .setValue(DoorBlock.POWERED, powered);
        }

        /** True when {@code state} is exactly this assembly's {@code part} of {@code block}. */
        public boolean isPart(BlockState state, Block block, Part part) {
            return state.is(block)
                    && state.getValue(DoorBlock.FACING) == facing
                    && state.getValue(DoorBlock.HINGE) == hinge
                    && state.getValue(RoundDoorBlock.COLUMN) == part.column
                    && state.getValue(DoorBlock.HALF) == part.half;
        }

        /** True when every cell of the assembly is in a loaded chunk. */
        public boolean loaded(Level level) {
            for (Part part : Part.values()) {
                if (!level.isLoaded(pos(part))) {
                    return false;
                }
            }
            return true;
        }

        /** The parts currently present and matching; never reads an unloaded cell. */
        public List<Part> presentParts(Level level, Block block) {
            List<Part> present = new ArrayList<>(4);
            for (Part part : Part.values()) {
                BlockPos pos = pos(part);
                if (level.isLoaded(pos) && isPart(level.getBlockState(pos), block, part)) {
                    present.add(part);
                }
            }
            return present;
        }

        /** True when all four cells are loaded and hold this assembly's parts. */
        public boolean complete(Level level, Block block) {
            return presentParts(level, block).size() == Part.values().length;
        }

        /**
         * World bounds of everything the leaf can cover while swinging: the four cells plus the two
         * cells in front of them along {@code facing}, where the open leaf projects.
         */
        public AABB sweepBounds() {
            BlockPos far = pos(Part.UPPER_RIGHT).relative(facing);
            AABB cells = new AABB(anchor).minmax(new AABB(far));
            // A turning corner of the leaf's thickness pokes a fraction of a pixel past the cells.
            return cells.inflate(1.0 / 16);
        }
    }

    /** The assembly a part belongs to, derived from its own state. */
    public static Assembly of(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DoorBlock.FACING);
        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockPos anchor = state.getValue(RoundDoorBlock.COLUMN) == DoorColumn.LEFT
                ? lower : lower.relative(facing.getCounterClockWise());
        return new Assembly(anchor.immutable(), facing, state.getValue(DoorBlock.HINGE));
    }

    public static Part partOf(BlockState state) {
        return Part.of(state.getValue(RoundDoorBlock.COLUMN), state.getValue(DoorBlock.HALF));
    }
}
