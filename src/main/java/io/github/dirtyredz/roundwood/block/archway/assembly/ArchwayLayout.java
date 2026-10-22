package io.github.dirtyredz.roundwood.block.archway.assembly;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Where the four cells of an archway are (0.12.0). Like the round door: the anchor is the lower-left
 * cell seen from the front (the placing player's side, looking along {@code facing}), the right
 * column one block to the right ({@code facing}'s clockwise neighbour), the upper row one block up.
 * A cell's partners are only the cells this names holding the same block (so the same design), facing
 * and the matching part, so two archways side by side never merge.
 */
public final class ArchwayLayout {
    private ArchwayLayout() {
    }

    /** The four cells in the shared part order. */
    private static final List<ArchwayPart> PARTS = List.of(ArchwayPart.values());

    /**
     * One archway: its anchor (lower-left) cell, the direction the placing player faced, its design. The
     * shared traversal and the authored frame are the {@link io.github.dirtyredz.roundwood.block.assembly.Assembly}
     * contract's.
     */
    public record Assembly(BlockPos anchor, Direction facing, ArchwayDesign design)
            implements io.github.dirtyredz.roundwood.block.assembly.Assembly<ArchwayPart> {
        @Override
        public List<ArchwayPart> parts() {
            return PARTS;
        }

        @Override
        public BlockPos pos(ArchwayPart part) {
            return anchor.relative(facing.getClockWise(), part.column).above(part.row);
        }

        public BlockState stateFor(ArchwayPart part, Block block) {
            return block.defaultBlockState().setValue(ArchwayBlock.FACING, facing).setValue(ArchwayBlock.PART, part);
        }

        /** Same block (so the same design), facing and part. */
        @Override
        public boolean isPart(BlockState state, Block block, ArchwayPart part) {
            return state.is(block) && state.getValue(ArchwayBlock.FACING) == facing && state.getValue(ArchwayBlock.PART) == part;
        }

        /** The four cells, plus the one pixel each face casing stands proud of them (drawn whenever any of it shows). */
        public AABB renderBounds() {
            AABB box = new AABB(anchor);
            for (BlockPos pos : positions()) {
                box = box.minmax(new AABB(pos));
            }
            double proud = AttachmentFace.PROUD / 16.0;
            return box.inflate(Math.abs(facing.getStepX()) * proud, 0, Math.abs(facing.getStepZ()) * proud);
        }
    }

    /** The archway a cell belongs to, from its own state. */
    public static Assembly of(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(ArchwayBlock.FACING);
        ArchwayPart part = state.getValue(ArchwayBlock.PART);
        BlockPos anchor = pos.below(part.row).relative(facing.getCounterClockWise(), part.column);
        return new Assembly(anchor.immutable(), facing, ((ArchwayBlock) state.getBlock()).design());
    }
}
