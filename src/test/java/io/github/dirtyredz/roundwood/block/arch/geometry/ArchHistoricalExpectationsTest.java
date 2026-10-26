package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Independent historical expectations: never redefine retired geometry for new pieces. */
class ArchHistoricalExpectationsTest {
    @Test
    void bothHistoricalCrownsHaveTheApprovedRadiusFourteenSemicircle() {
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (int y = 16; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    double dx = x + 0.5 - 16, dy = y + 0.5 - 16;
                    assertEquals(dx * dx + dy * dy > 196, design.inFrame(x, y));
                }
            }
        }
    }

    @Test
    void historicalTransitionAndRemovedPostsAreExplicit() {
        int[] widths = {6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 5, 4, 4, 3, 3, 2};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 32; x++) {
                assertEquals(x < widths[y] || x >= 32 - widths[y], ArchwayDesign.KEYHOLE.inFrame(x, y));
                assertEquals(x < 2 || x >= 30, ArchwayDesign.ORDINARY.inFrame(x, y));
            }
        }
    }
}
