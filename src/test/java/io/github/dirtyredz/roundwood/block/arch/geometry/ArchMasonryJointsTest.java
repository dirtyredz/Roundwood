package io.github.dirtyredz.roundwood.block.arch.geometry;

import org.junit.jupiter.api.Test;
import static io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchMasonryJointsTest {
    @Test
    void oddEqualAngleCountsLeaveACentredKeystone() {
        for (var design : ArchPieceDesign.values()) {
            if (design.shoulder()) continue;
            int n = 4 * design.columns() + 1;
            assertEquals(n, ArchMasonryJoints.voussoirs(design));
            var angles = ArchMasonryJoints.radialAngles(design);
            assertEquals(n - 1, angles.size());
            for (int k = 1; k < n; k++) assertEquals(k * Math.PI / n, angles.get(k - 1), 1e-12);
            assertEquals(Math.PI, angles.get(n / 2 - 1) + angles.get(n / 2), 1e-12);
            double r = design.top().radius();
            assertFalse(ArchMasonryJoints.contains(design, LEFT, design.width() / 2.0, design.top().spring() + r + 1));
            for (double angle : angles) {
                double x = design.width() / 2.0 + (r + 1) * Math.cos(angle), y = design.top().spring() + (r + 1) * Math.sin(angle);
                assertEquals(design.contains(LEFT, (int) Math.floor(x), (int) Math.floor(y)),
                        ArchMasonryJoints.contains(design, LEFT, x, y));
            }
            assertFalse(ArchMasonryJoints.contains(design, LEFT, 1, .0625));
            assertFalse(ArchMasonryJoints.contains(design, LEFT, 1, .25));
            // A block seam alone never adds mortar high in the upper fill.
            for (int x = 16; x < design.width(); x += 16)
                assertFalse(ArchMasonryJoints.contains(design, LEFT, x, design.height() - .25));
        }
    }

    @Test
    void allJointsAreSymmetricClippedAndQuarterPixelWide() {
        assertEquals(.25, ArchMasonryJoints.WIDTH);
        for (var design : ArchPieceDesign.values()) {
            for (double y = -.0625; y < design.height() + 1; y += .125)
                for (double x = -.0625; x < design.width() + 1; x += .125) {
                    boolean joint = ArchMasonryJoints.contains(design, LEFT, x, y);
                    assertEquals(joint, ArchMasonryJoints.contains(design, RIGHT, design.width() - x, y));
                    if (joint) assertTrue(design.contains(LEFT, (int) Math.floor(x), (int) Math.floor(y)));
                }
        }
        for (double y : ArchMasonryJoints.SHOULDER_LEVELS) {
            assertTrue(ArchMasonryJoints.contains(ArchPieceDesign.KEYHOLE_SHOULDER, LEFT, 1, y == 16 ? y - .0625 : y + .0625));
            assertFalse(ArchMasonryJoints.contains(ArchPieceDesign.KEYHOLE_SHOULDER, LEFT, 1, y + .25));
        }
    }
}
