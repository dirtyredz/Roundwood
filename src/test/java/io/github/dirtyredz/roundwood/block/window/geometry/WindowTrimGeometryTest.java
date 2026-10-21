package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The window trim (0.7.0): the band follows each opening rim, sits on frame wood only, stays inside
 * the cell at the specified depths in every pose, never changes collision, and its whole-face
 * artwork coordinates run on across the four pieces of the arched window.
 */
class WindowTrimGeometryTest {
    @Test
    void theBandFollowsEachRimOnFrameWoodOnly() {
        for (WindowProfile profile : WindowProfile.values()) {
            int count = 0;
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    boolean band = WindowTrimGeometry.inBand(profile, u, v);
                    if (band) {
                        count++;
                        assertFalse(profile.inOpening(u, v), profile + ": trim over the glazing at " + u + "," + v);
                    }
                    assertEquals(expectedBand(profile, u, v), band, profile + " band at " + u + "," + v);
                }
            }
            assertTrue(count > 0, profile + ": empty band");
        }
    }

    /** Written out independently: the 16/12 ring, the 32/28 ring around (16, 0) of the arches, the corners' jamb and sill. */
    private static boolean expectedBand(WindowProfile profile, int u, int v) {
        double cu = u + 0.5;
        double cv = v + 0.5;
        return switch (profile) {
            case ROUND -> {
                double d = (cu - 8) * (cu - 8) + (cv - 8) * (cv - 8);
                yield d <= 64 && d > 36;
            }
            case ARCH_LEFT, ARCH_RIGHT -> {
                double x = cu + (profile == WindowProfile.ARCH_RIGHT ? 16 : 0) - 16;
                double d = x * x + cv * cv;
                yield d <= 256 && d > 196;
            }
            case BOTTOM_LEFT -> u < 2 || v < 2;
            case BOTTOM_RIGHT -> u >= 14 || v < 2;
        };
    }

    @Test
    void trimDepthsStayInTheCellAndCoverBothFaces() {
        for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
            double[][] depths = WindowTrimGeometry.depths(pose.depth());
            double[][] expected = switch (pose.depth()) {
                case CENTER -> new double[][] {{6, 7}, {9, 10}};
                case NEGATIVE -> new double[][] {{0, 1}, {2, 3}};
                case POSITIVE -> new double[][] {{13, 14}, {15, 16}};
            };
            assertArrayEquals(expected[0], depths[0]);
            assertArrayEquals(expected[1], depths[1]);
            for (WindowProfile profile : WindowProfile.values()) {
                for (Piece piece : WindowTrimGeometry.pieces(profile, pose)) {
                    assertTrue(piece.box().within(Box.cell()), profile + " " + pose + ": trim leaves the cell " + piece.box());
                    assertEquals(WindowTrimGeometry.TRIM, piece.texture());
                }
            }
        }
    }

    @Test
    void theTrimNeverCollidesButIsTargeted() {
        for (WindowDesign design : WindowDesign.all()) {
            for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
                List<Box> collision = WindowPaneGeometry.boxes(design, pose, Set.of());
                List<Box> selection = WindowPaneGeometry.selection(design, pose, Set.of(), true);
                assertEquals(List.of(pose.slab()), collision, design.id() + " " + pose);
                assertTrue(selection.size() > collision.size(), design.id() + " " + pose + ": trim not targetable");
                assertEquals(collision, WindowPaneGeometry.selection(design, pose, Set.of(), false));
            }
        }
    }

    /**
     * Whole-face artwork coordinates: one image over the round face, or over the 2 x 2 arched window,
     * so the two pieces meeting at a seam sample the same image column there; the back reads mirrored.
     */
    @Test
    void wholeFaceCoordinatesRunOnAcrossThePieces() {
        WindowPose front = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        // Facing south, u runs east (x) and v up (y). The left arch's right edge meets the right arch's left edge.
        float[] leftEdge = WindowTrimUv.whole(WindowProfile.ARCH_LEFT, front, 16, 8, 10, true);
        float[] rightEdge = WindowTrimUv.whole(WindowProfile.ARCH_RIGHT, front, 0, 8, 10, true);
        assertArrayEquals(leftEdge, rightEdge, 1e-6F);
        assertArrayEquals(new float[] {8, 4}, leftEdge, 1e-6F);
        // The corner's top edge meets the arch's bottom edge.
        assertArrayEquals(WindowTrimUv.whole(WindowProfile.ARCH_LEFT, front, 4, 0, 10, true),
                WindowTrimUv.whole(WindowProfile.BOTTOM_LEFT, front, 4, 16, 10, true), 1e-6F);
        assertArrayEquals(new float[] {2, 16}, WindowTrimUv.whole(WindowProfile.BOTTOM_LEFT, front, 4, 0, 10, true), 1e-6F);
        // The round window covers its own 16 pixels; the back is the mirror image.
        assertArrayEquals(new float[] {4, 12}, WindowTrimUv.whole(WindowProfile.ROUND, front, 4, 4, 10, true), 1e-6F);
        assertArrayEquals(new float[] {12, 12}, WindowTrimUv.whole(WindowProfile.ROUND, front, 4, 4, 6, false), 1e-6F);
        // A turned pose maps the same face point to the same artwork.
        WindowPose west = front.rotateY(1);
        double[] p = {16 - 10, 8, 4}; // (x, z) -> (16 - z, x) of the point (4, 8, 10)
        assertArrayEquals(WindowTrimUv.whole(WindowProfile.ARCH_LEFT, front, 4, 8, 10, true),
                WindowTrimUv.whole(WindowProfile.ARCH_LEFT, west, p[0], p[1], p[2], true), 1e-6F);
    }
}
