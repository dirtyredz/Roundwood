package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchPieceGeometryTest {
    @Test
    void clippingUnionsToWholeMaskWithoutOverlap() {
        for (ArchPieceDesign design : ArchPieceDesign.values()) {
            for (int y = 0; y < design.height(); y++) for (int x = 0; x < design.width(); x++) {
                int px = x, py = y;
                long whole = ArchPieceGeometry.body(design).stream().filter(b -> b.containsStrictly(px + .5, py + .5, 8)).count();
                long cell = ArchPieceGeometry.cell(design, x / 16, y / 16).stream()
                        .filter(b -> b.containsStrictly(px % 16 + .5, py % 16 + .5, 8)).count();
                assertEquals(design.top().contains(x, y) ? 1 : 0, whole);
                assertEquals(whole, cell);
            }
        }
    }

    @Test
    void internalCellBoundariesHaveNoExposedFaces() {
        for (ArchPieceDesign design : ArchPieceDesign.values()) {
            for (int c = 0; c < design.columns(); c++) for (int r = 0; r < design.rows(); r++) {
                for (var face : ArchPieceGeometry.surfaces(design, c, r, 0, 16, "body")) {
                    Box b = face.backing();
                    if (face.face() == Direction.EAST && b.maxX() == 16 && c + 1 < design.columns()) {
                        for (int y = (int) b.minY(); y < b.maxY(); y++) assertFalse(design.top().contains((c + 1) * 16, r * 16 + y));
                    }
                    if (face.face() == Direction.UP && b.maxY() == 16 && r + 1 < design.rows()) {
                        for (int x = (int) b.minX(); x < b.maxX(); x++) assertFalse(design.top().contains(c * 16 + x, (r + 1) * 16));
                    }
                }
                assertTrue(ArchPieceGeometry.collisionCell(design, c, r).isEmpty());
            }
        }
    }

    @Test
    void sixCellFootprintIncludesEmptyLowerCentre() {
        var design = ArchPieceDesign.TOP_2X3;
        assertEquals(6, design.columns() * design.rows());
        assertTrue(ArchPieceGeometry.cell(design, 1, 0).isEmpty());
        assertEquals(6 * 4096, ArchPieceGeometry.footprint(design, Direction.NORTH).volume());
    }
}
