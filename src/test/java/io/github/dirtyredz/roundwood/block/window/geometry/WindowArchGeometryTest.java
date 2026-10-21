package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowArchGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two arch pieces side by side are the upper half of one round window two blocks wide. */
class WindowArchGeometryTest {
    /** Written independently: glass where a pixel centre lies within 14 pixels of (16, 0). */
    private static boolean expectedGlass(int x, int y) {
        double dx = x + 0.5 - 16;
        double dy = y + 0.5;
        return dx * dx + dy * dy <= 14 * 14;
    }

    private static boolean covers(List<Piece> pieces, String texture, double x, double y, double z) {
        return pieces.stream().filter(p -> p.texture().equals(texture)).map(Piece::box)
                .anyMatch(b -> b.containsStrictly(x, y, z));
    }

    @Test
    void theTwoPiecesJoinIntoTheUpperSemicircle() {
        // Upright facing south, seen from the front: the left piece is to the west (x 0..16), the right one east of it.
        WindowPose pose = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        assertEquals(Direction.EAST, pose.right());
        List<Piece> left = WindowPaneGeometry.panel(WindowDesign.plain(WindowProfile.ARCH_LEFT), pose);
        List<Piece> right = WindowPaneGeometry.panel(WindowDesign.plain(WindowProfile.ARCH_RIGHT), pose);
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 16; y++) {
                List<Piece> piece = x < 16 ? left : right;
                int local = x % 16;
                boolean glass = expectedGlass(x, y);
                assertEquals(glass, covers(piece, WindowPaneGeometry.GLASS, local + 0.5, y + 0.5, 8), "glass at " + x + "," + y);
                assertEquals(!glass, covers(piece, WindowPaneGeometry.FRAME, local + 0.5, y + 0.5, 8), "wood at " + x + "," + y);
            }
        }
    }

    @Test
    void noMullionAndNoBottomBorder() {
        // The two columns either side of the join are glass from the bottom row up to the opening's top.
        for (int y = 0; y < 13; y++) {
            assertTrue(WindowArchGeometry.inCombinedOpening(15, y) && WindowArchGeometry.inCombinedOpening(16, y), "row " + y);
        }
        // The bottom row is glass right out to the two-pixel border.
        for (int x = 2; x < 30; x++) {
            assertTrue(WindowArchGeometry.inCombinedOpening(x, 0), "bottom row at " + x);
        }
        assertFalse(WindowArchGeometry.inCombinedOpening(1, 0));
        assertFalse(WindowArchGeometry.inCombinedOpening(30, 0));
    }

    @Test
    void squareWoodenCornersAndAThinBorder() {
        assertFalse(WindowProfile.ARCH_LEFT.inOpening(0, 15));
        assertFalse(WindowProfile.ARCH_RIGHT.inOpening(15, 15));
        assertFalse(WindowProfile.ARCH_LEFT.inOpening(15, 15), "the top border above the join is wood");
        assertTrue(WindowProfile.ARCH_LEFT.inOpening(15, 13));
    }

    @Test
    void piecesAreMirrorImages() {
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                assertEquals(WindowProfile.ARCH_LEFT.inOpening(u, v), WindowProfile.ARCH_RIGHT.inOpening(15 - u, v), u + "," + v);
            }
        }
    }

    @Test
    void aLeftPieceSeenFromBehindShowsTheRightSilhouette() {
        WindowPose front = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        WindowPose back = WindowPose.upright(Direction.NORTH, WindowDepth.CENTER);
        List<Box> leftFromSouth = WindowPaneGeometry.panel(WindowDesign.plain(WindowProfile.ARCH_LEFT), front).stream()
                .filter(p -> p.texture().equals(WindowPaneGeometry.GLASS)).map(Piece::box).toList();
        List<Box> rightFromNorth = WindowPaneGeometry.panel(WindowDesign.plain(WindowProfile.ARCH_RIGHT), back).stream()
                .filter(p -> p.texture().equals(WindowPaneGeometry.GLASS)).map(Piece::box).toList();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                double cx = x + 0.5;
                double cy = y + 0.5;
                assertEquals(leftFromSouth.stream().anyMatch(b -> b.containsStrictly(cx, cy, 8)),
                        rightFromNorth.stream().anyMatch(b -> b.containsStrictly(cx, cy, 8)), x + "," + y);
            }
        }
    }
}
