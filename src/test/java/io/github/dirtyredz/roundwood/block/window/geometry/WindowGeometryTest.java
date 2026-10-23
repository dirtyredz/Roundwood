package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The centred round window (the 0.5.0 geometry): the thin wooden pane and its round glazing, for both
 * axes (the 0.5.0 glass arms were removed in 0.13.1). Since 0.7.0 the logical volumes come from
 * {@link WindowPaneGeometry} alone (the 0.5.0 baseline class wrote models that are now generated as
 * visible surfaces); these checks pin that the plain round design still has exactly that shape.
 */
class WindowGeometryTest {
    /** The 0.5.0 centred round window, read from the general geometry. */
    private static final class Baseline {
        static final int OPENING = 12;
        static final double PANEL_BACK = 7;
        static final double PANEL_FRONT = 9;
        static final double GLASS_BACK = 7.5;
        static final double GLASS_FRONT = 8.5;
        static final String FRAME = WindowPaneGeometry.FRAME;
        static final String GLASS = WindowPaneGeometry.GLASS;

        static List<Piece> pieces() {
            return WindowPaneGeometry.panel(ROUND, AXIS_X);
        }
    }

    private static final WindowDesign ROUND = WindowDesign.plain(WindowProfile.ROUND);
    private static final WindowPose AXIS_X = WindowPosition.CENTER.pose(Direction.Axis.X);
    private static final WindowPose AXIS_Z = WindowPosition.CENTER.pose(Direction.Axis.Z);

    @Test
    void theZAxisWindowIsTheXAxisWindowTurned() {
        assertEquals(Baseline.pieces().stream().map(p -> p.rotateY(1)).toList(),
                WindowPaneGeometry.panel(ROUND, AXIS_Z));
    }

    private static final double EPS = 1e-9;

    private static List<Box> textured(String texture) {
        return Baseline.pieces().stream().filter(p -> p.texture().equals(texture)).map(Piece::box).toList();
    }

    private static boolean covers(List<Box> boxes, double x, double y, double z) {
        return boxes.stream().anyMatch(b -> b.minX() < x && x < b.maxX() && b.minY() < y && y < b.maxY()
                && b.minZ() < z && z < b.maxZ());
    }

    @Test
    void woodFillsEveryPixelOutsideTheOpeningAtPaneDepthOnly() {
        List<Box> frame = textured(Baseline.FRAME);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean opening = PixelCircle.inDisc(16, Baseline.OPENING, u, v);
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
        List<Box> glass = textured(Baseline.GLASS);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean opening = PixelCircle.inDisc(16, Baseline.OPENING, u, v);
                assertEquals(opening, covers(glass, u + 0.5, v + 0.5, 8), "glass at " + u + "," + v);
                for (double z : new double[] {3, 7.25, 8.75, 12}) {
                    assertFalse(covers(glass, u + 0.5, v + 0.5, z), "glass outside 7.5..8.5 at " + u + "," + v + " z " + z);
                }
            }
        }
        for (Box box : glass) {
            assertTrue(box.minZ() >= Baseline.GLASS_BACK - EPS && box.maxZ() <= Baseline.GLASS_FRONT + EPS,
                    "glass piece outside the glass depth " + box);
        }
    }

    @Test
    void onlyWoodAndGlassInsideThePaneSlab() {
        for (Piece piece : Baseline.pieces()) {
            assertTrue(piece.texture().equals(Baseline.FRAME) || piece.texture().equals(Baseline.GLASS),
                    "unexpected texture " + piece.texture());
            Box box = piece.box();
            assertTrue(box.within(Box.cell()), "outside the cell " + box);
            assertTrue(box.minZ() >= Baseline.PANEL_BACK - EPS && box.maxZ() <= Baseline.PANEL_FRONT + EPS,
                    "outside the z 7..9 slab " + box);
        }
    }

    @Test
    void noGlassFillsACornerPixel() {
        List<Box> glass = textured(Baseline.GLASS);
        for (int[] c : new int[][] {{0, 0}, {15, 0}, {0, 15}, {15, 15}}) {
            assertFalse(glass.stream().anyMatch(b -> b.minX() < c[0] + 0.5 && c[0] + 0.5 < b.maxX()
                    && b.minY() < c[1] + 0.5 && c[1] + 0.5 < b.maxY()), "glass in the pane at corner " + c[0] + "," + c[1]);
        }
    }

    @Test
    void theBoxesAreExactlyThePanelSlab() {
        List<Box> x = WindowPaneGeometry.boxes(AXIS_X);
        assertEquals(1, x.size());
        assertBox(x.get(0), 0, 0, 7, 16, 16, 9);
        List<Box> z = WindowPaneGeometry.boxes(AXIS_Z);
        assertEquals(1, z.size());
        assertBox(z.get(0), 7, 0, 0, 9, 16, 16);
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
