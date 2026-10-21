package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.interaction.WindowHitRegions.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The hit-region classifier every window click is routed by (0.8.0), against masks written out
 * independently here: the round window's 12-pixel opening inside its 16/12 band, the arches' radius-14
 * opening around (16, 0) inside the 32/28 band, the corners' two-pixel jamb and sill; cross beams on
 * the half-pixel columns and rows 7.5..8.5 inside the opening count as frame; a trim band wins only on
 * a trimmed face; the exact outer edge belongs to the last cell and anything outside the face is nothing.
 */
class WindowHitRegionsTest {
    @Test
    void everyHalfPixelOfAllTenDesignsHasTheExpectedRegion() {
        for (WindowDesign design : WindowDesign.all()) {
            for (boolean trimmed : new boolean[] {false, true}) {
                for (int hu = 0; hu < 32; hu++) {
                    for (int hv = 0; hv < 32; hv++) {
                        double u = (hu + 0.5) / 2;
                        double v = (hv + 0.5) / 2;
                        Region expected = expected(design, hu, hv, trimmed);
                        Region actual = WindowHitRegions.classify(design, u, v, trimmed);
                        if (expected != actual) {
                            fail(design.id() + " trimmed " + trimmed + " at " + u + "," + v + ": " + actual + ", expected " + expected);
                        }
                    }
                }
            }
        }
    }

    @Test
    void beamBoundariesAreHalfPixels() {
        WindowDesign cross = new WindowDesign(WindowProfile.ROUND, WindowPattern.CROSS);
        assertEquals(Region.GLASS, WindowHitRegions.classify(cross, 7.49, 5, false));
        assertEquals(Region.FRAME, WindowHitRegions.classify(cross, 7.5, 5, false));
        assertEquals(Region.FRAME, WindowHitRegions.classify(cross, 8.49, 5, false));
        assertEquals(Region.GLASS, WindowHitRegions.classify(cross, 8.5, 5, false));
        assertEquals(Region.FRAME, WindowHitRegions.classify(cross, 5, 7.5, true), "a beam is never trim");
    }

    @Test
    void theOuterEdgeIsTheLastCellAndOutsideIsNothing() {
        WindowDesign corner = WindowDesign.plain(WindowProfile.BOTTOM_RIGHT);
        assertEquals(Region.TRIM, WindowHitRegions.classify(corner, 16, 8, true), "u = 16 is the jamb's last column");
        assertEquals(Region.FRAME, WindowHitRegions.classify(corner, 16, 8, false));
        assertEquals(Region.GLASS, WindowHitRegions.classify(corner, 8, 16, false), "v = 16 is the top row, glazed");
        assertEquals(Region.NONE, WindowHitRegions.classify(corner, 16.01, 8, true));
        assertEquals(Region.NONE, WindowHitRegions.classify(corner, -0.01, 8, true));
        assertEquals(Region.NONE, WindowHitRegions.classify(corner, 8, Double.NaN, true));
    }

    private static Region expected(WindowDesign design, int hu, int hv, boolean trimmed) {
        double cu = hu / 2 + 0.5;
        double cv = hv / 2 + 0.5;
        int u = hu / 2;
        int v = hv / 2;
        boolean opening;
        boolean band;
        switch (design.profile()) {
            case ROUND -> {
                double d = (cu - 8) * (cu - 8) + (cv - 8) * (cv - 8);
                opening = d <= 36;
                band = d <= 64 && d > 36;
            }
            case ARCH_LEFT, ARCH_RIGHT -> {
                double x = cu + (design.profile() == WindowProfile.ARCH_RIGHT ? 16 : 0) - 16;
                double d = x * x + cv * cv;
                opening = d <= 196;
                band = d <= 256 && d > 196;
            }
            case BOTTOM_LEFT -> {
                opening = v >= 2 && u >= 2;
                band = !opening;
            }
            default -> {
                opening = v >= 2 && u < 14;
                band = !opening;
            }
        }
        if (trimmed && band) {
            return Region.TRIM;
        }
        boolean beam = design.pattern() == WindowPattern.CROSS && opening && (hu == 15 || hu == 16 || hv == 15 || hv == 16);
        if (beam) {
            return Region.FRAME;
        }
        return opening ? Region.GLASS : Region.FRAME;
    }
}
