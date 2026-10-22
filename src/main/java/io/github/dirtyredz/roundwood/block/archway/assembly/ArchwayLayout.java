package io.github.dirtyredz.roundwood.block.archway.assembly;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
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

    /** One archway: its anchor (lower-left) cell, the direction the placing player faced, its design. */
    public record Assembly(BlockPos anchor, Direction facing, ArchwayDesign design) {
        public BlockPos pos(ArchwayPart part) {
            return anchor.relative(facing.getClockWise(), part.column).above(part.row);
        }

        public List<BlockPos> positions() {
            List<BlockPos> out = new ArrayList<>(4);
            for (ArchwayPart part : ArchwayPart.values()) {
                out.add(pos(part));
            }
            return out;
        }

        public BlockState stateFor(ArchwayPart part, Block block) {
            return block.defaultBlockState().setValue(ArchwayBlock.FACING, facing).setValue(ArchwayBlock.PART, part);
        }

        public boolean isPart(BlockState state, Block block, ArchwayPart part) {
            return state.is(block) && state.getValue(ArchwayBlock.FACING) == facing && state.getValue(ArchwayBlock.PART) == part;
        }

        public boolean loaded(Level level) {
            for (BlockPos pos : positions()) {
                if (!level.isLoaded(pos)) {
                    return false;
                }
            }
            return true;
        }

        /** The cells currently present and matching; never reads an unloaded cell. */
        public List<ArchwayPart> presentParts(Level level, Block block) {
            List<ArchwayPart> present = new ArrayList<>(4);
            for (ArchwayPart part : ArchwayPart.values()) {
                BlockPos pos = pos(part);
                if (level.isLoaded(pos) && isPart(level.getBlockState(pos), block, part)) {
                    present.add(part);
                }
            }
            return present;
        }

        public boolean complete(Level level, Block block) {
            return presentParts(level, block).size() == ArchwayPart.values().length;
        }

        /**
         * A world point in the archway's authored frame: pixels from the anchor cell's origin, the
         * facing's turn undone (x across from the left, y up, z depth with the front at 16).
         */
        public Vec3 toAuthored(Vec3 world) {
            double x = (world.x - anchor.getX()) * 16;
            double y = (world.y - anchor.getY()) * 16;
            double z = (world.z - anchor.getZ()) * 16;
            // Undo the facing's clockwise quarter turns about the anchor cell's centre: (x, z) <- (z, 16 - x).
            for (int i = 0; i < VoxelShapeFactory.quarterTurns(facing); i++) {
                double turnedX = z;
                z = 16 - x;
                x = turnedX;
            }
            return new Vec3(x, y, z);
        }

        /** The four cells, plus the one pixel each face casing stands proud of them (drawn whenever any of it shows). */
        public AABB renderBounds() {
            AABB box = new AABB(anchor);
            for (BlockPos pos : positions()) {
                box = box.minmax(new AABB(pos));
            }
            double proud = 1 / 16.0;
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
