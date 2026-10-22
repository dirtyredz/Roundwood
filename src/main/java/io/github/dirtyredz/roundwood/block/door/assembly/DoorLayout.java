package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Where the parts of an assembly are. The design ({@link DoorDesign}, from the block) says how many
 * columns it owns: the round door four parts in two columns, the arched door (0.11.0) two parts in
 * one. A part's identity is its column and {@code half}: the round door saves the column in its
 * {@code column} property, the arched door has none and is always the left column (the vanilla
 * upper/lower identity only). The anchor (lower-left part) is found from those alone. The physical
 * {@code hinge} is shared by all parts and only says which outer edge the single leaf turns on.
 *
 * <p>Seen from the front (the side the placing player stood on, looking along {@code facing}), the
 * left column is at the anchor and a right column one block to the right ({@code facing}'s
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

        /** The parts a door of {@code design} owns, in this enum's order. */
        public static List<Part> of(DoorDesign design) {
            return design.columns() == 1 ? List.of(LOWER_LEFT, UPPER_LEFT) : List.of(values());
        }
    }

    /**
     * One assembly: the anchor cell, the direction the placing player faced, the leaf's hinge and the
     * door's design. The shared traversal (positions, loaded, present parts, completeness, the authored
     * frame) is the {@link io.github.dirtyredz.roundwood.block.assembly.Assembly} contract's.
     */
    public record Assembly(BlockPos anchor, Direction facing, DoorHingeSide hinge, DoorDesign design)
            implements io.github.dirtyredz.roundwood.block.assembly.Assembly<Part> {
        /** The parts this assembly owns (four for the round door, two for the arched door). */
        @Override
        public List<Part> parts() {
            return Part.of(design);
        }

        @Override
        public BlockPos pos(Part part) {
            BlockPos pos = part.column == DoorColumn.LEFT ? anchor : anchor.relative(facing.getClockWise());
            return part.half == DoubleBlockHalf.LOWER ? pos : pos.above();
        }

        /** The block state a part should have, open/powered as given; keeps any injected properties. */
        public BlockState stateFor(Part part, Block block, boolean open, boolean powered) {
            BlockState state = block.defaultBlockState()
                    .setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.HINGE, hinge)
                    .setValue(DoorBlock.HALF, part.half)
                    .setValue(DoorBlock.OPEN, open)
                    .setValue(DoorBlock.POWERED, powered);
            return state.hasProperty(RoundDoorBlock.COLUMN) ? state.setValue(RoundDoorBlock.COLUMN, part.column) : state;
        }

        /**
         * True when {@code state} is exactly this assembly's {@code part} of {@code block}: same block,
         * design, facing, hinge, column and half; {@code open} and {@code powered} are not identity.
         */
        @Override
        public boolean isPart(BlockState state, Block block, Part part) {
            return state.is(block)
                    && designOf(state) == design
                    && state.getValue(DoorBlock.FACING) == facing
                    && state.getValue(DoorBlock.HINGE) == hinge
                    && columnOf(state) == part.column
                    && state.getValue(DoorBlock.HALF) == part.half;
        }

        /**
         * World bounds of everything the leaf can cover while swinging: the cells plus the cells in
         * front of them along {@code facing}, where an open round leaf projects, and the hardware margin.
         */
        public AABB sweepBounds() {
            AABB cells = new AABB(anchor).minmax(new AABB(topRight().relative(facing)));
            // The knobs and straps stand proud of the leaf; at any swing angle they stay within this margin.
            return cells.inflate(DoorGeometry.SWEEP_MARGIN / 16);
        }

        /** The middle of the doorway: the centre of the cells' bounds (where the door's sounds play). */
        public Vec3 centre() {
            return new AABB(anchor).minmax(new AABB(topRight())).getCenter();
        }

        /** The upper cell of the rightmost column. */
        private BlockPos topRight() {
            return pos(Part.of(DoorColumn.values()[design.columns() - 1], DoubleBlockHalf.UPPER));
        }
    }

    /** The assembly a part belongs to, derived from its own state. */
    public static Assembly of(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DoorBlock.FACING);
        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockPos anchor = columnOf(state) == DoorColumn.LEFT ? lower : lower.relative(facing.getCounterClockWise());
        return new Assembly(anchor.immutable(), facing, state.getValue(DoorBlock.HINGE), designOf(state));
    }

    public static Part partOf(BlockState state) {
        return Part.of(columnOf(state), state.getValue(DoorBlock.HALF));
    }

    /** The design of a door part's block (only ever asked of Roundwood door states). */
    public static DoorDesign designOf(BlockState state) {
        if (state.getBlock() instanceof AssemblyDoorBlock door) {
            return door.design();
        }
        throw new IllegalArgumentException("not a Roundwood door: " + state);
    }

    /** The saved column of a round-door part; a door without the property (the arched door) is one left column. */
    public static DoorColumn columnOf(BlockState state) {
        return state.hasProperty(RoundDoorBlock.COLUMN) ? state.getValue(RoundDoorBlock.COLUMN) : DoorColumn.LEFT;
    }
}
