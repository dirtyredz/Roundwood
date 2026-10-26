package io.github.dirtyredz.roundwood.block.arch.geometry;

import org.junit.jupiter.api.Test;
import static io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.*;
import static org.junit.jupiter.api.Assertions.*;

class KeyholeShoulderProfileTest {
    @Test
    void exactRowsAndReflectedFullDepthBodies() {
        int[] widths = {6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 5, 4, 4, 3, 3, 2};
        for (int y = -1; y <= 16; y++) for (int x = -1; x <= 16; x++) {
            boolean expected = y >= 0 && y < 16 && x >= 0 && x < widths[y];
            assertEquals(expected, KeyholeShoulderProfile.contains(LEFT, x, y));
            assertEquals(expected, KeyholeShoulderProfile.contains(RIGHT, 15 - x, y));
        }
        for (var side : values()) {
            var boxes = ArchPieceGeometry.body(ArchPieceDesign.KEYHOLE_SHOULDER, side);
            assertEquals(80 * 16, boxes.stream().mapToDouble(b -> b.volume()).sum());
            assertEquals(boxes, ArchPieceGeometry.collisionCell(ArchPieceDesign.KEYHOLE_SHOULDER, side, 0, 0));
            boxes.forEach(b -> { assertEquals(0, b.minZ()); assertEquals(16, b.maxZ()); assertTrue(b.minY() >= 0); });
        }
    }
}
