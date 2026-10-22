package io.github.dirtyredz.roundwood.block.window.geometry.face;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bottom corners (0.7.0) against an independently written 32 x 32 arched window: arches over
 * corners make one opening, the lower part {@code x 2..30, y 2..16} under the radius-14 upper
 * semicircle centred at (16, 16), with a continuous two-pixel frame and no divider anywhere.
 */
class WindowCornerGeometryTest {
    /** Pixel (x, y) of the assembled 32 x 32 face is glazed, written out from the owner's sketch. */
    private static boolean expected(int x, int y) {
        if (y < 16) {
            return x >= 2 && x < 30 && y >= 2;
        }
        double dx = x + 0.5 - 16;
        double dy = y + 0.5 - 16;
        return dx * dx + dy * dy <= 14 * 14;
    }

    /** The piece covering assembly pixel (x, y), and that pixel in the piece's own face. */
    private static boolean assembled(int x, int y) {
        WindowProfile piece = y < 16 ? (x < 16 ? WindowProfile.BOTTOM_LEFT : WindowProfile.BOTTOM_RIGHT)
                : (x < 16 ? WindowProfile.ARCH_LEFT : WindowProfile.ARCH_RIGHT);
        return piece.inOpening(x - piece.assemblyU(), y - piece.assemblyV());
    }

    @Test
    void fourPiecesFormTheArchedWindowWithoutDividers() {
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                assertEquals(expected(x, y), assembled(x, y), "assembly pixel " + x + "," + y);
            }
        }
    }

    @Test
    void cornersHaveATwoPixelJambAndSillMeetingSquare() {
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean leftWood = u < 2 || v < 2;
                boolean rightWood = u >= 14 || v < 2;
                assertEquals(!leftWood, WindowProfile.BOTTOM_LEFT.inOpening(u, v), "left " + u + "," + v);
                assertEquals(!rightWood, WindowProfile.BOTTOM_RIGHT.inOpening(u, v), "right " + u + "," + v);
            }
        }
    }

    @Test
    void theCornersAreMirrorImagesAndMeetTheArchesEdgeToEdge() {
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                assertEquals(WindowProfile.BOTTOM_LEFT.inOpening(u, v), WindowProfile.BOTTOM_RIGHT.inOpening(15 - u, v), u + "," + v);
            }
            // the top row of a corner continues the bottom row of the arch above it
            assertEquals(WindowProfile.ARCH_LEFT.inOpening(u, 0), WindowProfile.BOTTOM_LEFT.inOpening(u, 15), "left seam " + u);
            assertEquals(WindowProfile.ARCH_RIGHT.inOpening(u, 0), WindowProfile.BOTTOM_RIGHT.inOpening(u, 15), "right seam " + u);
        }
    }
}
