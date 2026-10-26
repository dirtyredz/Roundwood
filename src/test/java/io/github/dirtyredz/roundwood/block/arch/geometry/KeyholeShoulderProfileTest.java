package io.github.dirtyredz.roundwood.block.arch.geometry;

import org.junit.jupiter.api.Test;
import static io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.*;
import static org.junit.jupiter.api.Assertions.*;

class KeyholeShoulderProfileTest {
    @Test
    void exactRowsAndReflectedFullDepthBodies() {
        int[] widths = {4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1, 0};
        for (int y = -1; y <= 16; y++) for (int x = -1; x <= 16; x++) {
            boolean expected = y >= 0 && y < 16 && x >= 0 && x < widths[y];
            assertEquals(expected, KeyholeShoulderProfile.contains(LEFT, x, y));
            assertEquals(expected, KeyholeShoulderProfile.contains(RIGHT, 15 - x, y));
        }
        for (var side : values()) {
            var boxes = ArchPieceGeometry.body(ArchPieceDesign.KEYHOLE_SHOULDER, side);
            assertEquals(48 * 16, boxes.stream().mapToDouble(b -> b.volume()).sum());
            assertEquals(boxes, ArchPieceGeometry.collisionCell(ArchPieceDesign.KEYHOLE_SHOULDER, side, 0, 0));
            boxes.forEach(b -> { assertEquals(0, b.minZ()); assertEquals(16, b.maxZ()); assertTrue(b.minY() >= 0); });
        }
    }

    /** No ledge and no gap at the seam: the shoulder's top row equals the top's bottom row on that side. */
    @Test
    void shoulderTopRowMatchesTheTopsBottomRowOnThatSide() {
        for (var design : ArchPieceDesign.values()) {
            if (design.shoulder()) continue;
            int topLeft = 0, topRight = 0;
            for (int u = 0; u < design.width(); u++) {
                if (design.contains(LEFT, u, 0) && u < design.width() / 2) topLeft++;
                if (design.contains(RIGHT, u, 0) && u >= design.width() / 2) topRight++;
            }
            int shoulderLeft = 0, shoulderRight = 0;
            for (int u = 0; u < 16; u++) {
                if (KeyholeShoulderProfile.contains(LEFT, u, 15)) shoulderLeft++;
                if (KeyholeShoulderProfile.contains(RIGHT, u, 15)) shoulderRight++;
            }
            assertEquals(topLeft, shoulderLeft, design + " left seam");
            assertEquals(topRight, shoulderRight, design + " right seam");
        }
    }
}
