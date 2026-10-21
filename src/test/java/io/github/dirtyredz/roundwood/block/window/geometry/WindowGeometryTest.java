package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The centred round window (the 0.5.0 geometry, kept as the baseline): the thin wooden pane, its
 * round glazing and the glass arms that reach across the plane, and the general pane geometry
 * reproducing it exactly for both axes.
 */
class WindowGeometryTest {
    private static final WindowPose AXIS_X = WindowPosition.CENTER.pose(Direction.Axis.X);
    private static final WindowPose AXIS_Z = WindowPosition.CENTER.pose(Direction.Axis.Z);

    /** The 0.5.0 signature: axis, then north, east, south, west. */
    private static List<Box> boxes(boolean axisZ, boolean north, boolean east, boolean south, boolean west) {
        Set<Direction> connected = EnumSet.noneOf(Direction.class);
        if (north) {
            connected.add(Direction.NORTH);
        }
        if (east) {
            connected.add(Direction.EAST);
        }
        if (south) {
            connected.add(Direction.SOUTH);
        }
        if (west) {
            connected.add(Direction.WEST);
        }
        return WindowPaneGeometry.boxes(WindowProfile.ROUND, axisZ ? AXIS_Z : AXIS_X, connected);
    }

    @Test
    void thePaneGeometryReproducesTheCentredBaselineExactly() {
        assertEquals(WindowGeometry.pieces(), WindowPaneGeometry.panel(WindowProfile.ROUND, AXIS_X));
        assertEquals(WindowGeometry.pieces().stream().map(p -> p.rotateY(1)).toList(),
                WindowPaneGeometry.panel(WindowProfile.ROUND, AXIS_Z));
        assertEquals(WindowGeometry.arm(), WindowPaneGeometry.arm(WindowProfile.ROUND, AXIS_X, Direction.NORTH));
        for (int turns = 0; turns < 4; turns++) {
            Direction side = Direction.from2DDataValue((turns + 2) % 4); // north, east, south, west
            WindowPose pose = side.getAxis() == Direction.Axis.Z ? AXIS_X : AXIS_Z;
            int t = turns;
            assertEquals(new HashSet<>(WindowGeometry.arm().stream().map(p -> p.rotateY(t)).toList()),
                    new HashSet<>(WindowPaneGeometry.arm(WindowProfile.ROUND, pose, side)), side.toString());
        }
        assertTrue(WindowPaneGeometry.hub(WindowProfile.ROUND, AXIS_X).isEmpty(), "a centred window has no hub");
        assertTrue(WindowPaneGeometry.arm(WindowProfile.ROUND, AXIS_X, Direction.EAST).isEmpty(), "in-plane arms are buried");
    }
    private static final double EPS = 1e-9;

    private static List<Box> textured(String texture) {
        return WindowGeometry.pieces().stream().filter(p -> p.texture().equals(texture)).map(Piece::box).toList();
    }

    private static List<Box> boxesOf(List<Piece> pieces) {
        return pieces.stream().map(Piece::box).toList();
    }

    private static boolean covers(List<Box> boxes, double x, double y, double z) {
        return boxes.stream().anyMatch(b -> b.minX() < x && x < b.maxX() && b.minY() < y && y < b.maxY()
                && b.minZ() < z && z < b.maxZ());
    }

    /** Pixel (u, v) of the face is covered, at any depth, by one of the boxes. */
    private static boolean coversPixelAtAnyDepth(List<Box> boxes, int u, int v) {
        return boxes.stream().anyMatch(b -> b.minX() < u + 0.5 && u + 0.5 < b.maxX() && b.minY() < v + 0.5
                && v + 0.5 < b.maxY());
    }

    private static boolean reaches(List<Box> boxes, ToDoubleFunction<Box> side, double value) {
        return boxes.stream().anyMatch(b -> Math.abs(side.applyAsDouble(b) - value) < EPS);
    }

    @Test
    void woodFillsEveryPixelOutsideTheOpeningAtPaneDepthOnly() {
        List<Box> frame = textured(WindowGeometry.FRAME);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean opening = PixelCircle.inDisc(16, WindowGeometry.OPENING, u, v);
                assertEquals(!opening, covers(frame, u + 0.5, v + 0.5, 8), "frame at " + u + "," + v);
                for (double z : new double[] {3, 12}) {
                    assertFalse(covers(frame, u + 0.5, v + 0.5, z), "wood outside the pane at " + u + "," + v + " z " + z);
                }
            }
        }
        for (int[] c : new int[][] {{0, 0}, {15, 0}, {0, 15}, {15, 15}}) {
            assertTrue(covers(frame, c[0] + 0.5, c[1] + 0.5, 8), "corner " + c[0] + "," + c[1] + " is not wood");
        }
    }

    @Test
    void glassFillsExactlyTheOpeningWithinTheGlassDepth() {
        List<Box> glass = textured(WindowGeometry.GLASS);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean opening = PixelCircle.inDisc(16, WindowGeometry.OPENING, u, v);
                assertEquals(opening, covers(glass, u + 0.5, v + 0.5, 8), "glass at " + u + "," + v);
                for (double z : new double[] {3, 7.25, 8.75, 12}) {
                    assertFalse(covers(glass, u + 0.5, v + 0.5, z), "glass outside 7.5..8.5 at " + u + "," + v + " z " + z);
                }
            }
        }
        for (Box box : glass) {
            assertTrue(box.minZ() >= WindowGeometry.GLASS_BACK - EPS && box.maxZ() <= WindowGeometry.GLASS_FRONT + EPS,
                    "glass piece outside the glass depth " + box);
        }
    }

    @Test
    void onlyWoodAndGlassInsideThePaneSlab() {
        for (Piece piece : WindowGeometry.pieces()) {
            assertTrue(piece.texture().equals(WindowGeometry.FRAME) || piece.texture().equals(WindowGeometry.GLASS),
                    "unexpected texture " + piece.texture());
            Box box = piece.box();
            assertTrue(box.within(Box.cell()), "outside the cell " + box);
            assertTrue(box.minZ() >= WindowGeometry.PANEL_BACK - EPS && box.maxZ() <= WindowGeometry.PANEL_FRONT + EPS,
                    "outside the z 7..9 slab " + box);
        }
    }

    @Test
    void armIsGlassFromTheEdgeToThePanelAndNeverIntoTheWood() {
        List<Box> arm = boxesOf(WindowGeometry.arm());
        for (Piece piece : WindowGeometry.arm()) {
            assertEquals(WindowGeometry.GLASS, piece.texture());
            Box box = piece.box();
            assertTrue(box.minX() >= 7 - EPS && box.maxX() <= 9 + EPS, "arm outside x 7..9 " + box);
            assertTrue(box.minZ() >= -EPS && box.maxZ() <= WindowGeometry.GLASS_BACK + EPS, "arm outside z 0..7.5 " + box);
            if (box.maxZ() > WindowGeometry.PANEL_BACK + EPS) {
                // Past the wood's back face the arm may only sit where the opening is.
                for (int v = (int) Math.round(box.minY()); v < (int) Math.round(box.maxY()); v++) {
                    assertTrue(WindowGeometry.inOpening(7, v) && WindowGeometry.inOpening(8, v),
                            "glass reaches into wood at row " + v);
                }
            }
        }
        for (int v = 0; v < 16; v++) {
            assertTrue(covers(arm, 8, v + 0.5, 0.25), "arm does not reach the edge at row " + v);
        }
        assertTrue(reaches(arm, Box::minZ, 0), "no arm piece starts at z=0");
    }

    @Test
    void noGlassFillsACornerPixel() {
        List<Box> glass = textured(WindowGeometry.GLASS);
        List<Box> arm = boxesOf(WindowGeometry.arm());
        for (int[] c : new int[][] {{0, 0}, {15, 0}, {0, 15}, {15, 15}}) {
            assertFalse(coversPixelAtAnyDepth(glass, c[0], c[1]), "glass in the pane at corner " + c[0] + "," + c[1]);
            assertFalse(coversPixelAtAnyDepth(arm, c[0], c[1]), "glass in the arm at corner " + c[0] + "," + c[1]);
        }
    }

    @Test
    void withoutConnectionsTheBoxesAreExactlyThePanel() {
        List<Box> x = boxes(false, false, false, false, false);
        assertEquals(1, x.size());
        assertBox(x.get(0), 0, 0, 7, 16, 16, 9);
        List<Box> z = boxes(true, false, false, false, false);
        assertEquals(1, z.size());
        assertBox(z.get(0), 7, 0, 0, 9, 16, 16);
    }

    @Test
    void inPlaneConnectionsAddNothingAndAcrossPlaneConnectionsReachTheEdge() {
        // axis x: east and west lie in the plane
        assertEquals(1, boxes(false, false, true, false, true).size());
        List<Box> north = boxes(false, true, false, false, false);
        assertTrue(north.size() > 1);
        assertTrue(reaches(north, Box::minZ, 0));
        assertFalse(reaches(north, Box::maxZ, 16));
        List<Box> south = boxes(false, false, false, true, false);
        assertTrue(south.size() > 1);
        assertTrue(reaches(south, Box::maxZ, 16));
        assertFalse(reaches(south, Box::minZ, 0));

        // axis z: north and south lie in the plane
        assertEquals(1, boxes(true, true, false, true, false).size());
        List<Box> east = boxes(true, false, true, false, false);
        assertTrue(east.size() > 1);
        assertTrue(reaches(east, Box::maxX, 16));
        assertFalse(reaches(east, Box::minX, 0));
        List<Box> west = boxes(true, false, false, false, true);
        assertTrue(west.size() > 1);
        assertTrue(reaches(west, Box::minX, 0));
        assertFalse(reaches(west, Box::maxX, 16));
    }

    @Test
    void everyBoxStaysInsideTheCell() {
        for (int axis = 0; axis < 2; axis++) {
            for (int mask = 0; mask < 16; mask++) {
                List<Box> boxes = boxes(axis == 1, (mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0,
                        (mask & 8) != 0);
                for (Box box : boxes) {
                    assertTrue(box.within(Box.cell()), "axis " + axis + " mask " + mask + ": " + box);
                }
            }
        }
    }

    private static void assertBox(Box box, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        assertEquals(minX, box.minX(), EPS);
        assertEquals(minY, box.minY(), EPS);
        assertEquals(minZ, box.minZ(), EPS);
        assertEquals(maxX, box.maxX(), EPS);
        assertEquals(maxY, box.maxY(), EPS);
        assertEquals(maxZ, box.maxZ(), EPS);
    }
}
