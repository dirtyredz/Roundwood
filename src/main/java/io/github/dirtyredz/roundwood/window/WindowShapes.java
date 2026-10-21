package io.github.dirtyredz.roundwood.window;

import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft shapes for windows, from {@link WindowGeometry}: the thin panel plus its active arms,
 * used for both collision and outline. 32 variants at most (axis x four connections), cached.
 */
public final class WindowShapes {
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private WindowShapes() {
    }

    public static VoxelShape shape(BlockState state) {
        boolean axisZ = state.getValue(RoundWindowBlock.AXIS) == Direction.Axis.Z;
        boolean north = state.getValue(CrossCollisionBlock.NORTH);
        boolean east = state.getValue(CrossCollisionBlock.EAST);
        boolean south = state.getValue(CrossCollisionBlock.SOUTH);
        boolean west = state.getValue(CrossCollisionBlock.WEST);
        int key = (axisZ ? 16 : 0) | (north ? 8 : 0) | (east ? 4 : 0) | (south ? 2 : 0) | (west ? 1 : 0);
        return SHAPES.computeIfAbsent(key, k -> VoxelShapeFactory.shape(WindowGeometry.boxes(axisZ, north, east, south, west)));
    }
}
