package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.concurrent.ConcurrentHashMap;

/** Finite shape cache. Selection is the body; only shoulders collide. */
public final class ArchPieceShapes {
    private record Key(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell, ArchPieceThickness thickness, ArchPieceDepth depth, int casings) {}
    private static final ConcurrentHashMap<Key, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private ArchPieceShapes() {}

    public static VoxelShape shape(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell) {
        return shape(design, side, facing, cell, ArchPieceThickness.FULL, ArchPieceDepth.FRONT);
    }
    public static VoxelShape shape(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell,
                                   ArchPieceThickness thickness, ArchPieceDepth depth) {
        return shape(design, side, facing, cell, thickness, depth, 0);
    }
    public static VoxelShape shape(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell,
                                   ArchPieceThickness thickness, ArchPieceDepth depth, int casings) {
        if (!cell.in(design)) return Shapes.empty();
        return SHAPES.computeIfAbsent(new Key(design, side, facing, cell, thickness, depth, casings), key -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(ArchPieceGeometry.cell(design, side, cell.column, cell.row, thickness, depth).stream().map(box ->
                        new io.github.dirtyredz.roundwood.geometry.Box(box.minX(), box.minY(), Math.max(0, box.minZ() - ((casings & 2) != 0 ? 1 : 0)),
                                box.maxX(), box.maxY(), Math.min(16, box.maxZ() + ((casings & 1) != 0 ? 1 : 0)))).toList(), VoxelShapeFactory.quarterTurns(facing))));
    }
    public static VoxelShape collision(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell) {
        return collision(design, side, facing, cell, ArchPieceThickness.FULL, ArchPieceDepth.FRONT);
    }
    public static VoxelShape collision(ArchPieceDesign design, Side side, Direction facing, ArchPieceCell cell,
                                       ArchPieceThickness thickness, ArchPieceDepth depth) {
        return design.shoulder() ? shape(design, side, facing, cell, thickness, depth) : Shapes.empty();
    }
}
