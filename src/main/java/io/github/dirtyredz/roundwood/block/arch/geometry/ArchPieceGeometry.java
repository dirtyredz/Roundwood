package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import io.github.dirtyredz.roundwood.geometry.PixelSlab;
import io.github.dirtyredz.roundwood.geometry.Surface;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;

import java.util.List;

/** One mask authority for body, selection, surfaces and placement silhouette. */
public final class ArchPieceGeometry {
    private ArchPieceGeometry() {}

    public static List<Box> body(ArchPieceDesign design, KeyholeShoulderProfile.Side side) {
        return body(design, side, ArchPieceThickness.FULL, ArchPieceDepth.FRONT);
    }
    public static List<Box> body(ArchPieceDesign design, KeyholeShoulderProfile.Side side, ArchPieceThickness thickness, ArchPieceDepth depth) {
        return PixelCircle.rasterize(design.width(), design.height(), (u, v) -> design.contains(side, u, v))
                .stream().map(rect -> rect.extrude(thickness.back(depth), thickness.front(depth))).toList();
    }

    public static List<Box> cell(ArchPieceDesign design, KeyholeShoulderProfile.Side side, int column, int row) {
        return cell(design, side, column, row, ArchPieceThickness.FULL, ArchPieceDepth.FRONT);
    }
    public static List<Box> cell(ArchPieceDesign design, KeyholeShoulderProfile.Side side, int column, int row,
                                 ArchPieceThickness thickness, ArchPieceDepth depth) {
        checkCell(design, column, row);
        return PixelCircle.rasterize(16, 16, (u, v) -> design.contains(side, u + 16 * column, v + 16 * row))
                .stream().map(rect -> rect.extrude(thickness.back(depth), thickness.front(depth))).toList();
    }

    public static List<Surface> surfaces(ArchPieceDesign design, KeyholeShoulderProfile.Side side, int column, int row, double back, double front, String texture) {
        checkCell(design, column, row);
        return List.copyOf(PixelSlab.cellFaces((u, v) -> design.contains(side, u, v), 16 * column, 16 * row, back, front, texture));
    }

    public static List<Box> collisionCell(ArchPieceDesign design, KeyholeShoulderProfile.Side side, int column, int row) {
        checkCell(design, column, row);
        return design.shoulder() ? cell(design, side, column, row) : List.of();
    }

    public static List<Box> placed(ArchPieceDesign design, KeyholeShoulderProfile.Side side, Direction facing) {
        return placed(design, side, facing, ArchPieceThickness.FULL, ArchPieceDepth.FRONT);
    }
    public static List<Box> placed(ArchPieceDesign design, KeyholeShoulderProfile.Side side, Direction facing,
                                   ArchPieceThickness thickness, ArchPieceDepth depth) {
        return VoxelShapeFactory.rotate(body(design, side, thickness, depth), VoxelShapeFactory.quarterTurns(facing));
    }

    public static Box footprint(ArchPieceDesign design, Direction facing) {
        return new Box(0, 0, 0, design.width(), design.height(), 16).rotateY(VoxelShapeFactory.quarterTurns(facing));
    }

    private static void checkCell(ArchPieceDesign design, int column, int row) {
        if (column < 0 || column >= design.columns() || row < 0 || row >= design.rows()) {
            throw new IllegalArgumentException("Cell outside arch piece");
        }
    }
}
