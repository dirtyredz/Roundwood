package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometryTest;
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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The window trim: the band follows each opening rim and sits on frame wood only (0.7.0); since 0.8.0
 * each face's band stands one pixel proud of its face in every pose (a flush pane's wall-side band one
 * pixel past the cell), never changes collision, is targeted only inside the cell, and its whole-face
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
    static boolean expectedBand(WindowProfile profile, int u, int v) {
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

    /**
     * 0.8.0 regression guard (owner report: trim "missing" on edge windows). A flush or floor/ceiling
     * pane's trim must stand one pixel proud of BOTH faces, like a centred pane's, including the face
     * against the cell boundary (render-only, beyond the cell). 0.7.0 inlaid that side at 0..1 / 15..16,
     * replacing the pane's own outer pixels, so it read as no trim at all.
     */
    @Test
    void flushTrimProjectsBeyondTheCellOnTheWallSide() {
        assertArrayEquals(new double[] {6, 7}, WindowTrimGeometry.depths(WindowDepth.CENTER)[0]);
        assertArrayEquals(new double[] {9, 10}, WindowTrimGeometry.depths(WindowDepth.CENTER)[1]);
        assertArrayEquals(new double[] {-1, 0}, WindowTrimGeometry.depths(WindowDepth.NEGATIVE)[0], "negative flush, wall side");
        assertArrayEquals(new double[] {2, 3}, WindowTrimGeometry.depths(WindowDepth.NEGATIVE)[1], "negative flush, room side");
        assertArrayEquals(new double[] {13, 14}, WindowTrimGeometry.depths(WindowDepth.POSITIVE)[0], "positive flush, room side");
        assertArrayEquals(new double[] {16, 17}, WindowTrimGeometry.depths(WindowDepth.POSITIVE)[1], "positive flush, wall side");
    }

    /**
     * Every band sits one pixel proud of its own face of the slab, in every pose, on both sides: never
     * inside the slab (no frame pixel is replaced) and never further than one pixel past the cell.
     */
    @Test
    void eachBandStandsOnePixelProudOfItsFace() {
        for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
            for (Direction side : sides(pose)) {
                double[] expected = side.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? new double[] {pose.depth().slabMax(), pose.depth().slabMax() + 1}
                        : new double[] {pose.depth().slabMin() - 1, pose.depth().slabMin()};
                for (WindowProfile profile : WindowProfile.values()) {
                    List<Piece> pieces = WindowTrimGeometry.pieces(profile, pose, side);
                    assertFalse(pieces.isEmpty(), profile + " " + pose + " " + side + ": empty band");
                    for (Piece piece : pieces) {
                        double[] n = range(piece.box(), pose.normal());
                        if (n[0] != expected[0] || n[1] != expected[1]) {
                            fail(profile + " " + pose + " " + side + ": band at " + n[0] + ".." + n[1]);
                        }
                        assertTrue(piece.box().within(new Box(-1, -1, -1, 17, 17, 17)), "beyond one pixel: " + piece.box());
                        assertEquals(WindowTrimGeometry.TRIM, piece.texture());
                    }
                }
            }
        }
    }

    /** Collision never includes a trim; the outline adds only the in-cell bands of the trimmed sides. */
    @Test
    void theTrimNeverCollidesAndIsTargetedOnlyInTheCell() {
        for (WindowDesign design : WindowDesign.all()) {
            for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
                List<Box> collision = WindowPaneGeometry.boxes(design, pose, Set.of());
                assertEquals(List.of(pose.slab()), collision, design.id() + " " + pose);
                assertEquals(collision, WindowPaneGeometry.selection(design, pose, Set.of(), Set.of()));
                for (Direction side : sides(pose)) {
                    List<Box> selection = WindowPaneGeometry.selection(design, pose, Set.of(), Set.of(side));
                    boolean inCell = range(WindowTrimGeometry.pieces(design.profile(), pose, side).get(0).box(), pose.normal())[0] >= 0
                            && range(WindowTrimGeometry.pieces(design.profile(), pose, side).get(0).box(), pose.normal())[1] <= 16;
                    if (inCell != (selection.size() > collision.size())) {
                        fail(design.id() + " " + pose + " " + side + ": in-cell " + inCell + " but selection " + selection.size());
                    }
                    for (Box box : selection) {
                        assertTrue(box.within(Box.cell()), "selection leaves the cell: " + box);
                    }
                }
            }
        }
    }

    static List<Direction> sides(WindowPose pose) {
        return List.of(Direction.fromAxisAndDirection(pose.normal(), Direction.AxisDirection.NEGATIVE),
                Direction.fromAxisAndDirection(pose.normal(), Direction.AxisDirection.POSITIVE));
    }

    static double[] range(Box box, Direction.Axis axis) {
        return switch (axis) {
            case X -> new double[] {box.minX(), box.maxX()};
            case Y -> new double[] {box.minY(), box.maxY()};
            case Z -> new double[] {box.minZ(), box.maxZ()};
        };
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
