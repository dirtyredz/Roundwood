package io.github.dirtyredz.roundwood.block.trim.geometry;

import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The selection shape of one frame quarter: only the ring piece in its own cell, turned for the
 * facing, so the open centre (and the corners outside the ring) let rays through to whatever is
 * behind. The frame never collides. 16 variants (facing x part), built once and cached.
 */
public final class TrimShapes {
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private TrimShapes() {
    }

    public static VoxelShape shape(Direction facing, TrimPart part) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        return SHAPES.computeIfAbsent((turns << 2) | part.ordinal(), k -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(TrimGeometry.quarter(part.column, part.row), turns)));
    }
}
