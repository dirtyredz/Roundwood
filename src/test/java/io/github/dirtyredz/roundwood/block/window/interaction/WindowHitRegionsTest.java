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
 * a trimmed face, an arch sill (0.10.0) inside the bottom two rows of an upper arch's opening only on a
 * silled face; the reflected and single-edge pieces (0.10.0) likewise; the exact outer edge belongs to
 * the last cell and anything outside the face is nothing.
 */
class WindowHitRegionsTest {
    @Test
    void everyHalfPixelOfEveryDesignHasTheExpectedRegion() {
        for (WindowDesign design : WindowDesign.all()) {
            for (int attached = 0; attached < 4; attached++) {
                boolean trimmed = (attached & 1) != 0;
                boolean silled = (attached & 2) != 0;
                for (int hu = 0; hu < 32; hu++) {
                    for (int hv = 0; hv < 32; hv++) {
                        double u = (hu + 0.5) / 2;
                        double v = (hv + 0.5) / 2;
                        Region expected = expected(design, hu, hv, trimmed, silled);
                        Region actual = WindowHitRegions.classify(design, u, v, trimmed, silled);
                        if (expected != actual) {
                            fail(design.id() + " trimmed " + trimmed + " sill " + silled + " at " + u + "," + v + ": " + actual
                                    + ", expected " + expected);
                        }
                    }
                }
            }
        }
    }

    /** The sill covers the bottom two rows inside an upper arch's opening, beam ends included, never the band. */
    @Test
    void theSillLiesInsideTheOpeningAndOverTheBeamEnds() {
        WindowDesign cross = new WindowDesign(WindowProfile.ARCH_LEFT, WindowPattern.CROSS);
        assertEquals(Region.SILL, WindowHitRegions.classify(cross, 8, 0.5, true, true), "a beam end under the sill is sill");
        assertEquals(Region.BEAM, WindowHitRegions.classify(cross, 8, 2.5, true, true), "above the sill the beam is frame");
        assertEquals(Region.TRIM, WindowHitRegions.classify(cross, 0.5, 0.5, true, true), "the band beside the sill stays trim");
        assertEquals(Region.GLASS, WindowHitRegions.classify(cross, 5, 0.5, true, false), "no sill on this face: glass");
        assertEquals(Region.GLASS, WindowHitRegions.classify(WindowDesign.plain(WindowProfile.ARCH_BOTTOM_LEFT), 5, 15.5, true, true),
                "a lower arch takes no sill");
    }

    @Test
    void beamBoundariesAreHalfPixels() {
        WindowDesign cross = new WindowDesign(WindowProfile.ROUND, WindowPattern.CROSS);
        assertEquals(Region.GLASS, WindowHitRegions.classify(cross, 7.49, 5, false));
        assertEquals(Region.BEAM, WindowHitRegions.classify(cross, 7.5, 5, false));
        assertEquals(Region.BEAM, WindowHitRegions.classify(cross, 8.49, 5, false));
        assertEquals(Region.GLASS, WindowHitRegions.classify(cross, 8.5, 5, false));
        assertEquals(Region.BEAM, WindowHitRegions.classify(cross, 5, 7.5, true), "a beam is never trim");
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

    private static Region expected(WindowDesign design, int hu, int hv, boolean trimmed, boolean silled) {
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
            case ARCH_LEFT, ARCH_RIGHT, ARCH_BOTTOM_LEFT, ARCH_BOTTOM_RIGHT -> {
                boolean right = design.profile() == WindowProfile.ARCH_RIGHT || design.profile() == WindowProfile.ARCH_BOTTOM_RIGHT;
                boolean lower = design.profile() == WindowProfile.ARCH_BOTTOM_LEFT || design.profile() == WindowProfile.ARCH_BOTTOM_RIGHT;
                double x = cu + (right ? 16 : 0) - 16;
                double y = lower ? 16 - cv : cv;
                double d = x * x + y * y;
                opening = d <= 196;
                band = d <= 256 && d > 196;
            }
            case BOTTOM_LEFT -> {
                opening = v >= 2 && u >= 2;
                band = !opening;
            }
            case BOTTOM_RIGHT -> {
                opening = v >= 2 && u < 14;
                band = !opening;
            }
            case TOP_LEFT -> {
                opening = v < 14 && u >= 2;
                band = !opening;
            }
            case TOP_RIGHT -> {
                opening = v < 14 && u < 14;
                band = !opening;
            }
            case EDGE_TOP -> {
                opening = v < 14;
                band = !opening;
            }
            case EDGE_LEFT -> {
                opening = u >= 2;
                band = !opening;
            }
            case EDGE_BOTTOM -> {
                opening = v >= 2;
                band = !opening;
            }
            default -> {
                opening = u < 14;
                band = !opening;
            }
        }
        boolean takesSill = design.profile() == WindowProfile.ARCH_LEFT || design.profile() == WindowProfile.ARCH_RIGHT;
        if (trimmed && band) {
            return Region.TRIM;
        }
        if (silled && takesSill && opening && v < 2) {
            return Region.SILL;
        }
        boolean beam = design.pattern() == WindowPattern.CROSS && opening && (hu == 15 || hu == 16 || hv == 15 || hv == 16);
        if (beam) {
            return Region.BEAM;
        }
        return opening ? Region.GLASS : Region.FRAME;
    }
}
