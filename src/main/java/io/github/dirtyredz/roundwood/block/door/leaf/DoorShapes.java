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
 * selection outline is the whole door, in {@code door/selection}). Keyed by design, facing, hinge,
 * column, half and open (128 variants at most), built once and cached.
 */
public final class DoorShapes {
    private static final ConcurrentHashMap<Key, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private record Key(DoorDesign design, Direction facing, DoorHingeSide hinge, DoorLayout.Part part, boolean open, double depth) {}

    private DoorShapes() {
    }

    public static VoxelShape shape(BlockState state) {
        return shape(DoorLayout.designOf(state), state.getValue(DoorBlock.FACING), state.getValue(DoorBlock.HINGE),
                DoorLayout.partOf(state), state.getValue(DoorBlock.OPEN), DoorLayout.depthOf(state).offset());
    }

    public static VoxelShape shape(DoorDesign design, Direction facing, DoorHingeSide hinge, DoorLayout.Part part, boolean open) {
        return shape(design, facing, hinge, part, open, 0);
    }

    public static VoxelShape shape(DoorDesign design, Direction facing, DoorHingeSide hinge, DoorLayout.Part part, boolean open, double depth) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        int column = part.columnIndex();
        int row = part.row();
        return SHAPES.computeIfAbsent(new Key(design, facing, hinge, part, open, depth), k -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(DoorGeometry.cellEnvelope(design, hinge, column, row, open, depth), turns)));
    }
}
