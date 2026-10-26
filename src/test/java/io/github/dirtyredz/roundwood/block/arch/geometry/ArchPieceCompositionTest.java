package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import org.junit.jupiter.api.Test;
import static io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchPieceCompositionTest {
    @Test
    void retainedCrownAndShouldersMatchHistoryAndOnlySpecifiedPostsAreRemoved() {
        for (var old : ArchwayDesign.values()) for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            boolean composed = y >= 16 ? ArchPieceDesign.TOP_1X2.top().contains(x, y - 16)
                    : old == ArchwayDesign.KEYHOLE && (KeyholeShoulderProfile.contains(LEFT, x, y)
                    || KeyholeShoulderProfile.contains(RIGHT, x - 16, y));
            if (y >= 16 || (old == ArchwayDesign.KEYHOLE && y >= 8)) assertEquals(old.inFrame(x, y), composed);
            else assertFalse(composed);
            assertFalse(composed && !old.inFrame(x, y), "no added pixels");
        }
        // Narrowest paired shoulder opening: 20 pixels in two cells, 36 in three.
        assertTrue(KeyholeShoulderProfile.contains(LEFT, 5, 8));
        assertFalse(KeyholeShoulderProfile.contains(LEFT, 6, 8));
        assertEquals(20, 32 - 2 * 6);
        assertEquals(36, 48 - 2 * 6);
    }
}
