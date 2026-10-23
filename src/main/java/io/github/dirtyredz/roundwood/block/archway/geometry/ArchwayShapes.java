package io.github.dirtyredz.roundwood.block.archway.geometry;

import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * One archway cell's shapes (0.12.0): the target and outline are the drawn frame inside its own cell
 * ({@link ArchwayGeometry#cell}), turned for the facing, so rays pass through the opening and the
 * jambs and crown stop them. Since 0.13.1 the collision is smaller than that
 * ({@link ArchwayGeometry#collisionCell}: the lower jambs only), so players and mobs walk through the
 * opening comfortably. 32 variants of each (design x facing x part), built once and cached.
 */
public final class ArchwayShapes {
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, VoxelShape> COLLISIONS = new ConcurrentHashMap<>();

    private ArchwayShapes() {
    }

    /** The target and outline: the drawn frame in this cell. */
    public static VoxelShape shape(ArchwayDesign design, Direction facing, ArchwayPart part) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        return SHAPES.computeIfAbsent(key(design, turns, part), k -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(ArchwayGeometry.cell(design, part.column, part.row), turns)));
    }

    /** What collides: the lower jambs in this cell (empty in the upper row). */
    public static VoxelShape collision(ArchwayDesign design, Direction facing, ArchwayPart part) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        return COLLISIONS.computeIfAbsent(key(design, turns, part), k -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(ArchwayGeometry.collisionCell(design, part.column, part.row), turns)));
    }

    private static int key(ArchwayDesign design, int turns, ArchwayPart part) {
        return (design.ordinal() << 4) | (turns << 2) | part.ordinal();
    }
}
