package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.DoubleArchedDoorBlock;
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
        UPPER_RIGHT(DoorColumn.RIGHT, DoubleBlockHalf.UPPER),
        TOP_LEFT(0, 2), TOP_MIDDLE(1, 2), LOWER_FAR_RIGHT(2, 0), MIDDLE_FAR_RIGHT(2, 1), TOP_FAR_RIGHT(2, 2), CROWN_LEFT(0, 3), CROWN_MIDDLE(1, 3), CROWN_RIGHT(2, 3);

        public final DoorColumn column;
        public final DoubleBlockHalf half;
        private final int x, y;
        Part(int x, int y) {
            this.column = x == 0 ? DoorColumn.LEFT : DoorColumn.RIGHT;
            this.half = y == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER;
            this.x = x; this.y = y;
        }
        public int columnIndex() { return x; }
        public static Part at(int x, int y) {
            return java.util.Arrays.stream(values()).filter(p -> p.x == x && p.y == y).findFirst().orElseThrow();
        }

        Part(DoorColumn column, DoubleBlockHalf half) {
            this.column = column;
            this.half = half;
            this.x = column.index(); this.y = half == DoubleBlockHalf.LOWER ? 0 : 1;
        }

        public static Part of(DoorColumn column, DoubleBlockHalf half) {
            if (column == DoorColumn.LEFT) {
                return half == DoubleBlockHalf.LOWER ? LOWER_LEFT : UPPER_LEFT;
            }
            return half == DoubleBlockHalf.LOWER ? LOWER_RIGHT : UPPER_RIGHT;
        }

        /** 0 for the lower row, 1 for the upper. */
        public int row() {
            return y;
        }

        /** The parts a door of {@code design} owns, in this enum's order. */
        public static List<Part> of(DoorDesign design) {
            return java.util.Arrays.stream(values()).filter(p -> p.x < design.columns() && p.y < design.height() / 16).toList();
        }
    }

    /**
     * One assembly: the anchor cell, the direction the placing player faced, the leaf's hinge and the
     * door's design. The shared traversal (positions, loaded, present parts, completeness, the authored
     * frame) is the {@link io.github.dirtyredz.roundwood.block.assembly.Assembly} contract's.
     */
    public record Assembly(BlockPos anchor, Direction facing, DoorHingeSide hinge, DoorDesign design, DoorDepth depth)
            implements io.github.dirtyredz.roundwood.block.assembly.Assembly<Part> {
        public Assembly(BlockPos anchor, Direction facing, DoorHingeSide hinge, DoorDesign design) {
            this(anchor, facing, hinge, design, DoorDepth.FRONT);
        }
        /** Coordinates of geometry, undoing the saved depth before targeting. */
        @Override public Vec3 toAuthored(Vec3 point) {
            return io.github.dirtyredz.roundwood.block.assembly.Assembly.super.toAuthored(point).add(0, 0, -depth.offset());
        }
        /** The parts this assembly owns (four for the round door, two for the arched door). */
        @Override
        public List<Part> parts() {
            return Part.of(design);
        }

        @Override
        public BlockPos pos(Part part) {
            return anchor.relative(facing.getClockWise(), part.columnIndex()).above(part.row());
        }

        /** The block state a part should have, open/powered as given; keeps any injected properties. */
        public BlockState stateFor(Part part, Block block, boolean open, boolean powered) {
            BlockState state = block.defaultBlockState()
                    .setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.HINGE, hinge)
                    .setValue(DoorBlock.HALF, part.half)
                    .setValue(DoorBlock.OPEN, open)
                    .setValue(DoorBlock.POWERED, powered);
            if (state.hasProperty(DoorDepth.PROPERTY)) state = state.setValue(DoorDepth.PROPERTY, depth);
            if (design.doubleLeaf()) return state.setValue(DoubleArchedDoorBlock.CELL_X, part.columnIndex())
                    .setValue(DoubleArchedDoorBlock.CELL_Y, part.row());
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
                    && partOf(state) == part
                    && depthOf(state) == depth;
        }

        /**
         * World bounds of everything the leaf can cover while swinging: the cells plus the cells in
         * front of them along {@code facing}, where an open round leaf projects, and the hardware margin.
         */
        public AABB sweepBounds() {
            AABB cells = new AABB(anchor).minmax(new AABB(topRight().relative(facing, depth == DoorDepth.FRONT ? 1 : 2)));
            // The knobs and straps stand proud of the leaf; at any swing angle they stay within this margin.
            return cells.inflate(DoorGeometry.SWEEP_MARGIN / 16);
        }

        /** The middle of the doorway: the centre of the cells' bounds (where the door's sounds play). */
        public Vec3 centre() {
            return new AABB(anchor).minmax(new AABB(topRight())).getCenter();
        }

        /** The upper cell of the rightmost column. */
        private BlockPos topRight() {
            return pos(Part.at(design.columns() - 1, design.height() / 16 - 1));
        }
    }

    /** The assembly a part belongs to, derived from its own state. */
    public static Assembly of(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DoorBlock.FACING);
        Part part = partOf(state);
        BlockPos anchor = pos.below(part.row()).relative(facing.getCounterClockWise(), part.columnIndex());
        return new Assembly(anchor.immutable(), facing, state.getValue(DoorBlock.HINGE), designOf(state), depthOf(state));
    }

    public static Part partOf(BlockState state) {
        if (state.hasProperty(DoubleArchedDoorBlock.CELL_X)) return Part.at(state.getValue(DoubleArchedDoorBlock.CELL_X), state.getValue(DoubleArchedDoorBlock.CELL_Y));
        return Part.of(columnOf(state), state.getValue(DoorBlock.HALF));
    }

    public static DoorDepth depthOf(BlockState state) {
        return state.hasProperty(DoorDepth.PROPERTY) ? state.getValue(DoorDepth.PROPERTY) : DoorDepth.FRONT;
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
