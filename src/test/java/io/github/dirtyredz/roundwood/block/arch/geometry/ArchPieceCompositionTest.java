package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import org.junit.jupiter.api.Test;
import static io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchPieceCompositionTest {
    @Test
    void newCrownWidensHistoryWhileFullShouldersPreserveTheLowerKeyhole() {
        for (var old : ArchwayDesign.values()) for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            boolean composed = y >= 16 ? ArchPieceDesign.TOP_1X2.top().contains(x, y - 16)
                    : old == ArchwayDesign.KEYHOLE && (KeyholeShoulderProfile.contains(LEFT, x, y)
                    || KeyholeShoulderProfile.contains(RIGHT, x - 16, y));
            // Shoulders are the historical lower keyhole thinned by the two pixels the crown gained.
            if (y < 16) assertEquals(old == ArchwayDesign.KEYHOLE && (x < 16 ? old.inFrame(x + 2, y) : old.inFrame(x - 2, y)), composed);
            else {
                int dx = 2 * x + 1 - 32, dy = 2 * (y - 16) + 1;
                assertEquals(dx * dx + dy * dy > 32 * 32, composed);
            }
            assertFalse(composed && !old.inFrame(x, y), "no added pixels");
        }
        // Narrowest paired shoulder opening: 24 pixels in two cells, 40 in three.
        assertTrue(KeyholeShoulderProfile.contains(LEFT, 3, 8));
        assertFalse(KeyholeShoulderProfile.contains(LEFT, 4, 8));
        assertEquals(24, 32 - 2 * 4);
        assertEquals(40, 48 - 2 * 4);
    }
}
