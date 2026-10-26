package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchTopProfileTest {
    @Test
    void everyPixelMatchesIndependentIntegerCircleOracle() {
        int[][] sizes = {{16, 16, 6}, {32, 16, 14}, {48, 32, 22}};
        for (int i = 0; i < sizes.length; i++) {
            ArchPieceDesign design = ArchPieceDesign.values()[i];
            int w = sizes[i][0], h = sizes[i][1], r = sizes[i][2];
            assertEquals(w, design.width());
            assertEquals(h, design.height());
            assertEquals(r, design.top().radius());
            for (int y = -1; y <= h; y++) for (int x = -1; x <= w; x++) {
                int dx = 2 * x + 1 - w, dy = 2 * y + 1;
                boolean solid = x >= 0 && x < w && y >= 0 && y < h && dx * dx + dy * dy > 4 * r * r;
                assertEquals(solid, design.top().contains(x, y));
                assertEquals(design.top().contains(x, y), design.top().contains(w - 1 - x, y));
            }
            assertFalse(design.top().contains(w / 2, r - 1));
            assertTrue(design.top().contains(w / 2, r));
        }
    }

    @Test
    void twoWideTopExactlyMatchesBothOldUpperHalves() {
        for (ArchwayDesign old : ArchwayDesign.values()) {
            for (int y = 0; y < 16; y++) for (int x = 0; x < 32; x++) {
                assertEquals(old.inFrame(x, y + 16), ArchPieceDesign.TOP_1X2.top().contains(x, y));
            }
        }
    }
}
