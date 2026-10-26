package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.LEFT;
import static org.junit.jupiter.api.Assertions.*;

class ArchPieceGeometryTest {
    @Test
    void clippingUnionsToWholeMaskWithoutOverlap() {
        for (ArchPieceDesign design : ArchPieceDesign.values()) {
            for (int y = 0; y < design.height(); y++) for (int x = 0; x < design.width(); x++) {
                int px = x, py = y;
                long whole = ArchPieceGeometry.body(design, LEFT).stream().filter(b -> b.containsStrictly(px + .5, py + .5, 8)).count();
                long cell = ArchPieceGeometry.cell(design, LEFT, x / 16, y / 16).stream()
                        .filter(b -> b.containsStrictly(px % 16 + .5, py % 16 + .5, 8)).count();
                assertEquals(design.contains(LEFT, x, y) ? 1 : 0, whole);
                assertEquals(whole, cell);
            }
        }
    }

    @Test
    void internalCellBoundariesHaveNoExposedFaces() {
        for (ArchPieceDesign design : ArchPieceDesign.values()) {
            for (int c = 0; c < design.columns(); c++) for (int r = 0; r < design.rows(); r++) {
                for (var face : ArchPieceGeometry.surfaces(design, LEFT, c, r, 0, 16, "body")) {
                    Box b = face.backing();
                    if (face.face() == Direction.EAST && b.maxX() == 16 && c + 1 < design.columns()) {
                        for (int y = (int) b.minY(); y < b.maxY(); y++) assertFalse(design.contains(LEFT, (c + 1) * 16, r * 16 + y));
                    }
                    if (face.face() == Direction.UP && b.maxY() == 16 && r + 1 < design.rows()) {
                        for (int x = (int) b.minX(); x < b.maxX(); x++) assertFalse(design.contains(LEFT, c * 16 + x, (r + 1) * 16));
                    }
                }
                assertEquals(!design.shoulder(), ArchPieceGeometry.collisionCell(design, LEFT, c, r).isEmpty());
            }
        }
    }

    @Test
    void rotatedSelectionAndCollisionFollowTheChangedPixels() {
        for (var design : ArchPieceDesign.values()) for (var side : KeyholeShoulderProfile.Side.values())
            for (var cell : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell.of(design))
                for (var facing : Direction.Plane.HORIZONTAL) {
                    var selection = ArchPieceShapes.shape(design, side, facing, cell).toAabbs();
                    var collision = ArchPieceShapes.collision(design, side, facing, cell).toAabbs();
                    for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                        double px = (x + .5) / 16, pz = .5, py = (y + .5) / 16;
                        for (int turn = 0; turn < io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory.quarterTurns(facing); turn++) {
                            double nextX = 1 - pz; pz = px; px = nextX;
                        }
                        final double sx = px, sz = pz;
                        boolean expected = design.contains(side, x + 16 * cell.column, y + 16 * cell.row);
                        assertEquals(expected, selection.stream().anyMatch(b -> b.contains(sx, py, sz)));
                        assertEquals(design.shoulder() && expected, collision.stream().anyMatch(b -> b.contains(sx, py, sz)));
                    }
                }
    }

    @Test
    void sixCellFootprintIncludesEmptyLowerCentre() {
        var design = ArchPieceDesign.TOP_2X3;
        assertEquals(6, design.columns() * design.rows());
        assertTrue(ArchPieceGeometry.cell(design, LEFT, 1, 0).isEmpty());
        assertEquals(6 * 4096, ArchPieceGeometry.footprint(design, Direction.NORTH).volume());
    }
}
