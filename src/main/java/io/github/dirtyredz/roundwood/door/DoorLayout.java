package io.github.dirtyredz.roundwood.door;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the four parts of an assembly are. Each part is an ordinary door half; its facing, hinge
 * and half are enough to find the anchor (the lower, left-hinged part), so no extra state is stored.
 *
 * <p>Seen from the front (the side the placing player stood on, looking along {@code facing}), the
 * left column is at the anchor and the right column one block to the right ({@code facing}'s
 * clockwise neighbour).
 */
public final class DoorLayout {
    private DoorLayout() {
    }

    public enum Part {
        LOWER_LEFT(DoorHingeSide.LEFT, DoubleBlockHalf.LOWER),
        UPPER_LEFT(DoorHingeSide.LEFT, DoubleBlockHalf.UPPER),
        LOWER_RIGHT(DoorHingeSide.RIGHT, DoubleBlockHalf.LOWER),
        UPPER_RIGHT(DoorHingeSide.RIGHT, DoubleBlockHalf.UPPER);

        public final DoorHingeSide hinge;
        public final DoubleBlockHalf half;

        Part(DoorHingeSide hinge, DoubleBlockHalf half) {
            this.hinge = hinge;
            this.half = half;
        }

        public static Part of(DoorHingeSide hinge, DoubleBlockHalf half) {
            if (hinge == DoorHingeSide.LEFT) {
                return half == DoubleBlockHalf.LOWER ? LOWER_LEFT : UPPER_LEFT;
            }
            return half == DoubleBlockHalf.LOWER ? LOWER_RIGHT : UPPER_RIGHT;
        }
    }

    /** One assembly: the anchor cell and the direction the placing player faced. */
    public record Assembly(BlockPos anchor, Direction facing) {
        public BlockPos pos(Part part) {
            BlockPos pos = part.hinge == DoorHingeSide.LEFT ? anchor : anchor.relative(facing.getClockWise());
            return part.half == DoubleBlockHalf.LOWER ? pos : pos.above();
        }

        public List<BlockPos> positions() {
            List<BlockPos> out = new ArrayList<>(4);
            for (Part part : Part.values()) {
                out.add(pos(part));
            }
            return out;
        }

        /** The block state a part should have, open/powered as given. */
        public BlockState stateFor(Part part, Block block, boolean open, boolean powered) {
            return block.defaultBlockState()
                    .setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.HINGE, part.hinge)
                    .setValue(DoorBlock.HALF, part.half)
                    .setValue(DoorBlock.OPEN, open)
                    .setValue(DoorBlock.POWERED, powered);
        }

        /** True when {@code state} is exactly this assembly's {@code part} of {@code block}. */
        public boolean isPart(BlockState state, Block block, Part part) {
            return state.is(block)
                    && state.getValue(DoorBlock.FACING) == facing
                    && state.getValue(DoorBlock.HINGE) == part.hinge
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
    }

    /** The assembly a part belongs to, derived from its own state. */
    public static Assembly of(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DoorBlock.FACING);
        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockPos anchor = state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT
                ? lower : lower.relative(facing.getCounterClockWise());
        return new Assembly(anchor, facing);
    }

    public static Part partOf(BlockState state) {
        return Part.of(state.getValue(DoorBlock.HINGE), state.getValue(DoorBlock.HALF));
    }
}
