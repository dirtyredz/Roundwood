package io.github.dirtyredz.roundwood.block.archway.geometry;

import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * One archway cell's shape (0.12.0): only the frame inside its own cell
 * ({@link ArchwayGeometry#cell}), turned for the facing. It is the collision, the target and the
 * outline at once, so rays and players pass through the opening and the jambs and crown stop both.
 * 32 variants (design x facing x part), built once and cached.
 */
public final class ArchwayShapes {
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private ArchwayShapes() {
    }

    public static VoxelShape shape(ArchwayDesign design, Direction facing, ArchwayPart part) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        int key = (design.ordinal() << 4) | (turns << 2) | part.ordinal();
        return SHAPES.computeIfAbsent(key, k -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(ArchwayGeometry.cell(design, part.column, part.row), turns)));
    }
}
