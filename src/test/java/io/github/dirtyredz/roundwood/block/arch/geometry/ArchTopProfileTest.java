package io.github.dirtyredz.roundwood.block.arch.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchTopProfileTest {
    @Test
    void everyPixelMatchesIndependentIntegerCircleOracle() {
        int[][] sizes = {{16, 16, 8, 8}, {32, 16, 16, 0}, {48, 32, 24, 8}};
        for (int i = 0; i < sizes.length; i++) {
            ArchPieceDesign design = ArchPieceDesign.values()[i];
            int w = sizes[i][0], h = sizes[i][1], r = sizes[i][2], spring = sizes[i][3];
            assertEquals(w, design.width());
            assertEquals(h, design.height());
            assertEquals(r, design.top().radius());
            for (int y = -1; y <= h; y++) for (int x = -1; x <= w; x++) {
                int dx = 2 * x + 1 - w, dy = 2 * (y - spring) + 1;
                boolean solid = x >= 0 && x < w && y >= spring && y < h && dx * dx + dy * dy > 4 * r * r;
                assertEquals(solid, design.top().contains(x, y));
                assertEquals(design.top().contains(x, y), design.top().contains(w - 1 - x, y));
            }
            assertFalse(design.top().contains(w / 2, spring + r - 1));
            assertEquals(spring + r < h, design.top().contains(w / 2, spring + r));
        }
    }

    @Test void smallTopHasNoLowerFillOrApexBand() {
        var top = ArchPieceDesign.TOP_1X1.top();
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) assertFalse(top.contains(x, y));
        for (int y = 8; y < 16; y++) { assertFalse(top.contains(7, y)); assertFalse(top.contains(8, y)); }
        assertTrue(top.contains(0, 15));
    }
    @Test void widerTopsHaveNoSpringLedge() {
        for (var d : new ArchPieceDesign[]{ArchPieceDesign.TOP_1X2, ArchPieceDesign.TOP_2X3}) {
            assertEquals(d.width(), 2 * d.top().radius());
            for (int x = 0; x < d.width(); x++) assertFalse(d.top().contains(x, 0));
            assertFalse(d.top().contains(0, 1));
            assertFalse(d.top().contains(d.width() - 1, 1));
        }
    }
}
