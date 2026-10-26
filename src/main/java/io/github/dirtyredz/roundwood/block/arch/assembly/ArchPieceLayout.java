package io.github.dirtyredz.roundwood.block.arch.assembly;

import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Objects;

/** Variable one-, two- and six-cell membership; no neighbour discovery or support policy. */
public final class ArchPieceLayout {
    private ArchPieceLayout() {}

    public record Assembly(BlockPos anchor, Direction facing, ArchPieceDesign design, Side side)
            implements io.github.dirtyredz.roundwood.block.assembly.Assembly<ArchPieceCell> {
        public Assembly {
            anchor = anchor.immutable();
            Objects.requireNonNull(design);
            Objects.requireNonNull(side);
            if (!facing.getAxis().isHorizontal()) throw new IllegalArgumentException("Arch pieces face horizontally");
        }
        @Override public List<ArchPieceCell> parts() { return ArchPieceCell.of(design); }
        @Override public BlockPos pos(ArchPieceCell cell) {
            if (!cell.in(design)) throw new IllegalArgumentException("Cell outside piece");
            return anchor.relative(facing.getClockWise(), cell.column).above(cell.row);
        }
        public BlockState stateFor(ArchPieceCell cell, Block block) {
            if (!cell.in(design) || !(block instanceof ArchPieceBlock piece) || piece.design() != design)
                throw new IllegalArgumentException("State does not belong to this product");
            return block.defaultBlockState().setValue(ArchPieceBlock.FACING, facing)
                    .setValue(ArchPieceBlock.CELL, cell).setValue(ArchPieceBlock.SIDE, side);
        }
        @Override public boolean isPart(BlockState state, Block block, ArchPieceCell cell) {
            return cell.in(design) && state.is(block) && block instanceof ArchPieceBlock piece && piece.design() == design
                    && state.getValue(ArchPieceBlock.FACING) == facing && state.getValue(ArchPieceBlock.CELL) == cell
                    && state.getValue(ArchPieceBlock.SIDE) == side;
        }
        public AABB renderBounds() {
            AABB bounds = new AABB(anchor);
            for (BlockPos pos : positions()) bounds = bounds.minmax(new AABB(pos));
            return bounds.inflate(Math.abs(facing.getStepX()) / 16.0, 0, Math.abs(facing.getStepZ()) / 16.0);
        }
    }

    public static BlockPos anchorFor(BlockPos pos, Direction facing, ArchPieceCell cell) {
        return pos.below(cell.row).relative(facing.getCounterClockWise(), cell.column).immutable();
    }

    public static Assembly of(BlockPos pos, BlockState state) {
        ArchPieceBlock block = (ArchPieceBlock) state.getBlock();
        var cell = state.getValue(ArchPieceBlock.CELL);
        if (!cell.in(block.design())) throw new IllegalArgumentException("Invalid cell for arch product");
        Direction facing = state.getValue(ArchPieceBlock.FACING);
        return new Assembly(anchorFor(pos, facing, cell), facing, block.design(), state.getValue(ArchPieceBlock.SIDE));
    }
}
