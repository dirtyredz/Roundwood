package io.github.dirtyredz.roundwood.block.door.leaf;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft collision shapes for each part, from {@link DoorGeometry}'s clipped leaf slab (the
 * selection outline is the whole door, in {@code door/selection}). 64 variants at most (facing x
 * hinge x column x half x open), built once and cached.
 */
public final class DoorShapes {
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private DoorShapes() {
    }

    public static VoxelShape shape(BlockState state) {
        return shape(state.getValue(DoorBlock.FACING), state.getValue(DoorBlock.HINGE),
                DoorLayout.partOf(state), state.getValue(DoorBlock.OPEN));
    }

    public static VoxelShape shape(Direction facing, DoorHingeSide hinge, DoorLayout.Part part, boolean open) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        int column = part.column.index();
        int row = part.row();
        int hingeBit = hinge == DoorHingeSide.LEFT ? 0 : 1;
        int key = (turns << 4) | (hingeBit << 3) | (column << 2) | (row << 1) | (open ? 1 : 0);
        return SHAPES.computeIfAbsent(key, k -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(DoorGeometry.cellEnvelope(hinge, column, row, open), turns)));
    }
}
