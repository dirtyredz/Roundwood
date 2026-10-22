package io.github.dirtyredz.roundwood.block.window.geometry.face;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The single-edge pieces (0.10.0): exactly one two-pixel border, on the named side, and glass on the
 * remaining 16 x 14 face; their whole-face artwork spans their own 16 pixels (not a 32-pixel assembly
 * just because they are oriented); a crossed piece's beams reach right across its glazing up to the
 * border. Left and right are mirror images, top and bottom reflections.
 */
class WindowEdgeGeometryTest {
    private static final Map<WindowProfile, String> SIDES = Map.of(WindowProfile.EDGE_TOP, "top", WindowProfile.EDGE_LEFT, "left",
            WindowProfile.EDGE_BOTTOM, "bottom", WindowProfile.EDGE_RIGHT, "right");

    /** Written out independently: which pixels the one border covers. */
    private static boolean border(String side, int u, int v) {
        return switch (side) {
            case "top" -> v >= 14;
            case "left" -> u < 2;
            case "bottom" -> v < 2;
            default -> u >= 14;
        };
    }

    @Test
    void eachEdgeHasExactlyOneTwoPixelBorder() {
        SIDES.forEach((profile, side) -> {
            int wood = 0;
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    assertEquals(!border(side, u, v), profile.inOpening(u, v), profile + " at " + u + "," + v);
                    wood += profile.inOpening(u, v) ? 0 : 1;
                }
            }
            assertEquals(2 * 16, wood, profile + ": one 2 x 16 border");
            assertEquals(WindowProfile.SIZE, profile.assemblySize(), profile + ": artwork over its own face");
            assertEquals(0, profile.assemblyU());
            assertEquals(0, profile.assemblyV());
            assertEquals(true, profile.oriented());
        });
    }

    @Test
    void leftAndRightMirrorTopAndBottomReflect() {
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                assertEquals(WindowProfile.EDGE_LEFT.inOpening(u, v), WindowProfile.EDGE_RIGHT.inOpening(15 - u, v));
                assertEquals(WindowProfile.EDGE_TOP.inOpening(u, v), WindowProfile.EDGE_BOTTOM.inOpening(u, 15 - v));
            }
        }
        for (WindowProfile profile : WindowProfile.values()) {
            if (!SIDES.containsKey(profile)) {
                assertNull(profile.edge(), profile + " is no edge piece");
            }
        }
    }

    @Test
    void aCrossedEdgeRunsItsBeamsAcrossItsGlazing() {
        for (WindowProfile profile : List.of(WindowProfile.EDGE_TOP, WindowProfile.EDGE_LEFT, WindowProfile.EDGE_BOTTOM,
                WindowProfile.EDGE_RIGHT)) {
            WindowDesign cross = new WindowDesign(profile, WindowPattern.CROSS);
            for (int hu = 0; hu < 32; hu++) {
                for (int hv = 0; hv < 32; hv++) {
                    boolean onCross = hu == 15 || hu == 16 || hv == 15 || hv == 16;
                    boolean glazed = !border(SIDES.get(profile), hu / 2, hv / 2);
                    assertEquals(onCross && glazed, WindowMuntinGeometry.isBeam(cross, hu, hv), profile + " beam at " + hu + "," + hv);
                    assertEquals(glazed && !onCross, cross.glazed(hu, hv), profile + " glass at " + hu + "," + hv);
                }
            }
        }
    }
}
