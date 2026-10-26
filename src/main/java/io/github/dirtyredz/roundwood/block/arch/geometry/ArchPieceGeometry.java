package io.github.dirtyredz.roundwood.block.arch.geometry;

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

    public static List<Box> body(ArchPieceDesign design) {
        return PixelCircle.rasterize(design.width(), design.height(), design.top()::contains)
                .stream().map(rect -> rect.extrude(0, 16)).toList();
    }

    public static List<Box> cell(ArchPieceDesign design, int column, int row) {
        checkCell(design, column, row);
        return PixelCircle.rasterize(16, 16, (u, v) -> design.top().contains(u + 16 * column, v + 16 * row))
                .stream().map(rect -> rect.extrude(0, 16)).toList();
    }

    public static List<Surface> surfaces(ArchPieceDesign design, int column, int row, double back, double front, String texture) {
        checkCell(design, column, row);
        return List.copyOf(PixelSlab.cellFaces(design.top()::contains, 16 * column, 16 * row, back, front, texture));
    }

    public static List<Box> collisionCell(ArchPieceDesign design, int column, int row) {
        checkCell(design, column, row);
        return List.of();
    }

    public static List<Box> placed(ArchPieceDesign design, Direction facing) {
        return VoxelShapeFactory.rotate(body(design), VoxelShapeFactory.quarterTurns(facing));
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
