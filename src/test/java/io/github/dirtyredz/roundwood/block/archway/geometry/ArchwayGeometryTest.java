package io.github.dirtyredz.roundwood.block.archway.geometry;

import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The archway profiles (0.12.0) against the proportions written out here independently: a 32 x 32
 * outer contour with straight jambs to y 16 under a radius-16 crown; the ordinary opening x 2..30 under
 * a radius-14 crown; the keyhole's shoulders stepping each side in by four pixels over eight, then a
 * twenty-pixel passage x 6..26 to the floor. Pixel-centre convention throughout.
 */
class ArchwayGeometryTest {
    private static boolean circle(double cx, double cy, double r, int u, int v) {
        double dx = u + 0.5 - cx;
        double dy = v + 0.5 - cy;
        return dx * dx + dy * dy <= r * r;
    }

    private static boolean outer(int u, int v) {
        return u >= 0 && u < 32 && v >= 0 && (v < 16 || circle(16, 16, 16, u, v));
    }

    private static boolean ordinaryOpening(int u, int v) {
        return u >= 2 && u < 30 && v >= 0 && (v < 16 || circle(16, 16, 14, u, v));
    }

    private static boolean keyholeOpening(int u, int v) {
        if (v >= 16) {
            return ordinaryOpening(u, v);
        }
        double x = u + 0.5;
        double y = v + 0.5;
        double left = y <= 8 ? 6 : 2 + (16 - y) / 2;
        return v >= 0 && x > left && x < 32 - left;
    }

    @Test
    void framesAreTheOuterContourMinusTheOpening() {
        for (int u = -2; u < 34; u++) {
            for (int v = -2; v < 34; v++) {
                boolean in = u >= 0 && u < 32 && v >= 0 && v < 32;
                assertEquals(in && outer(u, v) && !ordinaryOpening(u, v), ArchwayDesign.ORDINARY.inFrame(u, v), "ordinary " + u + "," + v);
                assertEquals(in && outer(u, v) && !keyholeOpening(u, v), ArchwayDesign.KEYHOLE.inFrame(u, v), "keyhole " + u + "," + v);
                assertEquals(ordinaryOpening(u, v), ArchwayDesign.ORDINARY.inOpening(u, v), "ordinary opening " + u + "," + v);
                assertEquals(keyholeOpening(u, v), ArchwayDesign.KEYHOLE.inOpening(u, v), "keyhole opening " + u + "," + v);
            }
        }
    }

    @Test
    void theOrdinaryArchwayHasTwoPixelJambsAndATwoPixelCrown() {
        for (int v = 0; v < 16; v++) {
            for (int u = 0; u < 32; u++) {
                assertEquals(u < 2 || u >= 30, ArchwayDesign.ORDINARY.inFrame(u, v), "jamb row " + v + " pixel " + u);
            }
        }
        // The apex: the frame is rows 30 and 31 over the centre, the opening reaches row 29.
        assertTrue(ArchwayDesign.ORDINARY.inFrame(15, 31) && ArchwayDesign.ORDINARY.inFrame(15, 30));
        assertTrue(ArchwayDesign.ORDINARY.inOpening(15, 29));
        // No threshold: the floor row is open across the passage.
        for (int u = 2; u < 30; u++) {
            assertTrue(ArchwayDesign.ORDINARY.inOpening(u, 0), "floor pixel " + u);
        }
    }

    @Test
    void theKeyholeStepsInFourPixelsOverEightThenLeavesATwentyPixelPassage() {
        int[] firstOpen = new int[16];
        for (int v = 0; v < 16; v++) {
            int u = 0;
            while (!ArchwayDesign.KEYHOLE.inOpening(u, v)) {
                u++;
            }
            firstOpen[v] = u;
            int width = 0;
            for (int x = 0; x < 32; x++) {
                width += ArchwayDesign.KEYHOLE.inOpening(x, v) ? 1 : 0;
            }
            assertEquals(32 - 2 * u, width, "row " + v + " is symmetric");
        }
        for (int v = 0; v < 8; v++) {
            assertEquals(6, firstOpen[v], "lower passage row " + v);
        }
        assertEquals(KeyholeGeometry.LOWER_WIDTH, 20);
        // Rows 8..15 step from 6 back out to 2, never moving out by more than one pixel per row.
        assertEquals(6, firstOpen[8]);
        assertEquals(2, firstOpen[15]);
        for (int v = 9; v < 16; v++) {
            assertTrue(firstOpen[v] <= firstOpen[v - 1] && firstOpen[v - 1] - firstOpen[v] <= 1, "shoulder row " + v);
        }
        // Above the spring line both designs are the same.
        for (int v = 16; v < 32; v++) {
            for (int u = 0; u < 32; u++) {
                assertEquals(ArchwayDesign.ORDINARY.inFrame(u, v), ArchwayDesign.KEYHOLE.inFrame(u, v), u + "," + v);
            }
        }
    }

    @Test
    void framesAreMirrorSymmetric() {
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (int u = 0; u < 32; u++) {
                for (int v = 0; v < 32; v++) {
                    assertEquals(design.inFrame(u, v), design.inFrame(31 - u, v), design + " " + u + "," + v);
                }
            }
        }
    }

    @Test
    void theCellsPartitionTheFrameAndStayInsideTheirCells() {
        for (ArchwayDesign design : ArchwayDesign.values()) {
            double whole = ArchwayGeometry.frame(design).stream().mapToDouble(Box::volume).sum();
            double cells = 0;
            for (int column = 0; column < 2; column++) {
                for (int row = 0; row < 2; row++) {
                    List<Box> cell = ArchwayGeometry.cell(design, column, row);
                    assertFalse(cell.isEmpty(), design + ": every cell holds some frame");
                    for (Box box : cell) {
                        assertTrue(box.within(Box.cell()), design + ": " + box);
                        assertEquals(0, box.minZ(), 1e-9, "full depth");
                        assertEquals(16, box.maxZ(), 1e-9, "full depth");
                    }
                    cells += cell.stream().mapToDouble(Box::volume).sum();
                }
            }
            assertEquals(whole, cells, 1e-6, design + ": cells sum to the frame");
            int pixels = 0;
            for (int u = 0; u < 32; u++) {
                for (int v = 0; v < 32; v++) {
                    pixels += design.inFrame(u, v) ? 1 : 0;
                }
            }
            assertEquals(pixels * 16.0, whole, 1e-6, design + ": the boxes are exactly the frame pixels");
        }
    }

    /** A player (0.6 x 1.8 blocks) walks through the middle of either passage. */
    @Test
    void aPlayerFitsThroughEitherPassage() {
        double half = 0.3 * 16;
        double height = 1.8 * 16;
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (Box box : ArchwayGeometry.frame(design)) {
                boolean overlaps = box.minX() < 16 + half && box.maxX() > 16 - half && box.minY() < height;
                assertFalse(overlaps, design + ": the frame blocks the passage at " + box);
            }
        }
    }

    /**
     * The collision is the lower jambs only (0.13.1, dk-73098f88): a 0.6 x 1.95 mob (a zombie or villager,
     * the tallest common walker) stands anywhere in the middle half of the passage with room to spare,
     * and the crown never collides, while the drawn frame, the target and the outline stay whole.
     */
    @Test
    void theCollisionLeavesThePassageClearForAMobAndKeepsTheJambs() {
        double mobHalf = 0.3 * 16;
        double mobHeight = 1.95 * 16;
        for (ArchwayDesign design : ArchwayDesign.values()) {
            int jambBoxes = 0;
            for (int column = 0; column < 2; column++) {
                for (int row = 0; row < 2; row++) {
                    List<Box> collision = ArchwayGeometry.collisionCell(design, column, row);
                    if (row == 1) {
                        assertTrue(collision.isEmpty(), design + ": the crown cell collides");
                    }
                    List<Box> drawn = ArchwayGeometry.cell(design, column, row);
                    for (Box box : collision) {
                        assertTrue(drawn.contains(box), design + ": collision " + box + " is not drawn frame");
                        assertTrue(box.maxY() <= 16, design + ": collision above the spring line: " + box);
                        double x = 16 * column + (box.minX() + box.maxX()) / 2;
                        boolean overlaps = box.minX() + 16 * column < 16 + mobHalf && box.maxX() + 16 * column > 16 - mobHalf
                                && box.minY() + 16 * row < mobHeight;
                        assertFalse(overlaps, design + ": the collision blocks a mob in the passage at " + box + " x " + x);
                    }
                    jambBoxes += row == 0 ? collision.size() : 0;
                }
            }
            assertTrue(jambBoxes > 0, design + ": the jambs do not collide");
        }
    }

    @Test
    void thePlacedOutlineAndFootprintTurnWithTheFacing() {
        for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            Box footprint = ArchwayGeometry.footprint(facing);
            assertEquals(32 * 32 * 16, footprint.volume(), 1e-9, facing + ": four cells");
            for (Box box : ArchwayGeometry.placed(ArchwayDesign.KEYHOLE, facing)) {
                assertTrue(box.within(footprint), facing + ": " + box + " outside " + footprint);
            }
        }
        // Facing north the assembly runs east (x 0..32) from the anchor cell.
        assertEquals(new Box(0, 0, 0, 32, 32, 16), ArchwayGeometry.footprint(Direction.NORTH));
    }
}
