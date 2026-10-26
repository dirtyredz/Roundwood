package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.concurrent.ConcurrentHashMap;

/** Finite shape cache. Selection is the body; only shoulders collide. */
public final class ArchPieceShapes {
    private record Key(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell) {}
    private static final ConcurrentHashMap<Key, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private ArchPieceShapes() {}

    public static VoxelShape shape(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell) {
        if (!cell.in(design)) return Shapes.empty();
        return SHAPES.computeIfAbsent(new Key(design, side, facing, cell), key -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(ArchPieceGeometry.cell(design, side, cell.column, cell.row), VoxelShapeFactory.quarterTurns(facing))));
    }
    public static VoxelShape collision(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell) {
        return design.shoulder() ? shape(design, side, facing, cell) : Shapes.empty();
    }
}
