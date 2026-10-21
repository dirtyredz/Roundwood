package io.github.dirtyredz.roundwood.window;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The square wooden housing and its round glazing. */
class WindowGeometryTest {
    private static List<Box> textured(String texture) {
        return WindowGeometry.pieces().stream().filter(p -> p.texture().equals(texture)).map(Piece::box).toList();
    }

    private static boolean covers(List<Box> boxes, double x, double y, double z) {
        return boxes.stream().anyMatch(b -> b.minX() < x && x < b.maxX() && b.minY() < y && y < b.maxY()
                && b.minZ() < z && z < b.maxZ());
    }

    @Test
    void woodFillsEveryPixelOutsideTheOpeningThroughTheWholeDepth() {
        List<Box> frame = textured(WindowGeometry.FRAME);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean opening = PixelCircle.inDisc(16, WindowGeometry.OPENING, u, v);
                for (double z : new double[] {0.5, 8, 15.5}) {
                    assertEquals(!opening, covers(frame, u + 0.5, v + 0.5, z), "frame at " + u + "," + v + " z " + z);
                }
            }
        }
        for (int[] c : new int[][] {{0, 0}, {15, 0}, {0, 15}, {15, 15}}) {
            assertTrue(covers(frame, c[0] + 0.5, c[1] + 0.5, 8), "corner " + c[0] + "," + c[1] + " is not wood");
        }
    }

    @Test
    void glassFillsExactlyTheOpeningAtMidDepth() {
        List<Box> glass = textured(WindowGeometry.GLASS);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean opening = PixelCircle.inDisc(16, WindowGeometry.OPENING, u, v);
                assertEquals(opening, covers(glass, u + 0.5, v + 0.5, 8), "glass at " + u + "," + v);
                assertFalse(covers(glass, u + 0.5, v + 0.5, 2), "glass should be recessed at " + u + "," + v);
            }
        }
    }

    @Test
    void onlyHousingAndGlassAndBothAxesAreTheSameBlock() {
        double volume = 0;
        double turned = 0;
        for (Piece piece : WindowGeometry.pieces()) {
            assertTrue(piece.texture().equals(WindowGeometry.FRAME) || piece.texture().equals(WindowGeometry.GLASS));
            assertTrue(piece.box().within(Box.cell()));
            Box quarter = piece.box().rotateY(1);
            assertTrue(quarter.within(Box.cell()));
            volume += piece.box().volume();
            turned += quarter.volume();
        }
        assertEquals(volume, turned, 1e-9);
    }
}
