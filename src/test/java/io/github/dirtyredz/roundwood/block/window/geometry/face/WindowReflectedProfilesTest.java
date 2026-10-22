package io.github.dirtyredz.roundwood.block.window.geometry.face;

import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reflected pieces (0.10.0): the lower arches are the upper arches reflected top to bottom, so the
 * four arches glaze exactly the 28-pixel disc of a 32 x 32 window ({@link PixelCircle#inDisc}, an
 * independent authority), with no divider at either seam; the top corners are the bottom corners
 * reflected, an outer jamb and a two-pixel head. Each crossed piece clips its own cross to its own
 * opening.
 */
class WindowReflectedProfilesTest {
    /** The piece of a 2 x 2 window covering assembly pixel (x, y), and that pixel in the piece's own face. */
    private static boolean circle(int x, int y) {
        WindowProfile piece = y < 16 ? (x < 16 ? WindowProfile.ARCH_BOTTOM_LEFT : WindowProfile.ARCH_BOTTOM_RIGHT)
                : (x < 16 ? WindowProfile.ARCH_LEFT : WindowProfile.ARCH_RIGHT);
        assertEquals(x < 16 ? 0 : 16, piece.assemblyU(), piece + " column");
        assertEquals(y < 16 ? 0 : 16, piece.assemblyV(), piece + " row");
        return piece.inOpening(x - piece.assemblyU(), y - piece.assemblyV());
    }

    @Test
    void fourArchesGlazeTheFullCircle() {
        int glazed = 0;
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                boolean expected = PixelCircle.inDisc(32, 28, x, y);
                assertEquals(expected, circle(x, y), "assembly pixel " + x + "," + y);
                glazed += expected ? 1 : 0;
            }
        }
        assertTrue(glazed > 600, "a 28-pixel disc");
    }

    @Test
    void theLowerArchesAreTheUpperOnesReflected() {
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                assertEquals(WindowProfile.ARCH_LEFT.inOpening(u, v), WindowProfile.ARCH_BOTTOM_LEFT.inOpening(u, 15 - v), u + "," + v);
                assertEquals(WindowProfile.ARCH_RIGHT.inOpening(u, v), WindowProfile.ARCH_BOTTOM_RIGHT.inOpening(u, 15 - v), u + "," + v);
                assertEquals(WindowProfile.ARCH_BOTTOM_LEFT.inOpening(u, v), WindowProfile.ARCH_BOTTOM_RIGHT.inOpening(15 - u, v),
                        "lower arches mirror each other at " + u + "," + v);
            }
            // the seam rows match: no wooden divider along the diameter or between the lower pair
            assertEquals(WindowProfile.ARCH_LEFT.inOpening(u, 0), WindowProfile.ARCH_BOTTOM_LEFT.inOpening(u, 15), "left seam " + u);
            assertEquals(WindowProfile.ARCH_RIGHT.inOpening(u, 0), WindowProfile.ARCH_BOTTOM_RIGHT.inOpening(u, 15), "right seam " + u);
        }
        for (int v = 0; v < 16; v++) {
            assertEquals(WindowProfile.ARCH_BOTTOM_LEFT.inOpening(15, v), WindowProfile.ARCH_BOTTOM_RIGHT.inOpening(0, v), "middle seam " + v);
        }
    }

    @Test
    void topCornersHaveAnOuterJambAndATwoPixelHead() {
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean leftWood = u < 2 || v >= 14;
                boolean rightWood = u >= 14 || v >= 14;
                assertEquals(!leftWood, WindowProfile.TOP_LEFT.inOpening(u, v), "left " + u + "," + v);
                assertEquals(!rightWood, WindowProfile.TOP_RIGHT.inOpening(u, v), "right " + u + "," + v);
                assertEquals(WindowProfile.BOTTOM_LEFT.inOpening(u, v), WindowProfile.TOP_LEFT.inOpening(u, 15 - v), "reflection");
            }
        }
        assertEquals(16, WindowProfile.TOP_LEFT.assemblyV(), "top corners sit in the upper row of the 2 x 2 window");
        assertEquals(WindowProfile.ASSEMBLY, WindowProfile.TOP_RIGHT.assemblySize());
    }

    /** Every crossed reflected piece carries its own cross, clipped to its own opening: four crosses, never one. */
    @Test
    void eachCrossedPieceClipsItsOwnCross() {
        for (WindowProfile profile : List.of(WindowProfile.ARCH_BOTTOM_LEFT, WindowProfile.ARCH_BOTTOM_RIGHT,
                WindowProfile.TOP_LEFT, WindowProfile.TOP_RIGHT)) {
            WindowDesign cross = new WindowDesign(profile, WindowPattern.CROSS);
            int beams = 0;
            for (int hu = 0; hu < 32; hu++) {
                for (int hv = 0; hv < 32; hv++) {
                    boolean onCross = hu == 15 || hu == 16 || hv == 15 || hv == 16;
                    boolean expected = onCross && profile.inOpening(hu / 2, hv / 2);
                    assertEquals(expected, WindowMuntinGeometry.isBeam(cross, hu, hv), profile + " beam at " + hu + "," + hv);
                    beams += expected ? 1 : 0;
                    assertFalse(cross.glazed(hu, hv) && expected, "glass under a beam");
                }
            }
            assertTrue(beams > 0, profile + ": no cross");
        }
    }
}
