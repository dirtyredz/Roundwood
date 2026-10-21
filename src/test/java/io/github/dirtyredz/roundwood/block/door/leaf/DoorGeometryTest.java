package io.github.dirtyredz.roundwood.block.door.leaf;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The single rigid leaf: pivots, the quarter turn, and collision clipped after turning. */
class DoorGeometryTest {
    private static final double EPS = 1e-9;

    private static double volume(List<Box> boxes) {
        return boxes.stream().mapToDouble(Box::volume).sum();
    }

    @Test
    void pivotsSitInTheMiddleOfTheHingeSideThickness() {
        assertEquals(1.5, DoorGeometry.pivotX(DoorHingeSide.LEFT), EPS);
        assertEquals(30.5, DoorGeometry.pivotX(DoorHingeSide.RIGHT), EPS);
        assertEquals(14.5, DoorGeometry.PIVOT_Z, EPS);
        // The pivot itself never moves.
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            double[] p = DoorGeometry.turn(DoorGeometry.pivotX(hinge), DoorGeometry.PIVOT_Z, 37, hinge);
            assertArrayEquals(new double[] {DoorGeometry.pivotX(hinge), DoorGeometry.PIVOT_Z}, p, EPS);
        }
    }

    @Test
    void openingSwingsTheFreeEdgeAwayFromTheFront() {
        float left = DoorGeometry.openDegrees(DoorHingeSide.LEFT);
        float right = DoorGeometry.openDegrees(DoorHingeSide.RIGHT);
        // Facing north the front is +z; the free edge must end up at z = -16 (one block past the cells).
        assertArrayEquals(new double[] {3, -16}, DoorGeometry.turn(32, 16, left, DoorHingeSide.LEFT), EPS);
        assertArrayEquals(new double[] {0, -16}, DoorGeometry.turn(32, 13, left, DoorHingeSide.LEFT), EPS);
        assertArrayEquals(new double[] {29, -16}, DoorGeometry.turn(0, 16, right, DoorHingeSide.RIGHT), EPS);
        assertArrayEquals(new double[] {32, -16}, DoorGeometry.turn(0, 13, right, DoorHingeSide.RIGHT), EPS);
        // Same convention as Axis.YP.rotationDegrees: +90 degrees takes +x to -z.
        assertArrayEquals(new double[] {1.5, 4.5}, DoorGeometry.turn(11.5, 14.5, 90, DoorHingeSide.LEFT), EPS);
    }

    @Test
    void theQuarterTurnIsRigidAndMatchesThePointTransform() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (Piece piece : DoorGeometry.leaf(hinge)) {
                Box closed = piece.box();
                Box open = DoorGeometry.swing(closed, hinge);
                assertEquals(closed.volume(), open.volume(), EPS, "volume changed: " + piece);
                assertEquals(closed.maxY() - closed.minY(), open.maxY() - open.minY(), EPS);
                // Widths swap between x and z under a quarter turn.
                assertEquals(closed.maxX() - closed.minX(), open.maxZ() - open.minZ(), EPS);
                assertEquals(closed.maxZ() - closed.minZ(), open.maxX() - open.minX(), EPS);
                double[] corner = DoorGeometry.turn(closed.minX(), closed.minZ(), DoorGeometry.openDegrees(hinge), hinge);
                assertTrue(near(corner[0], open.minX(), open.maxX()) && near(corner[1], open.minZ(), open.maxZ()),
                        "turned corner not on the swung box: " + piece);
            }
        }
    }

    private static boolean near(double value, double a, double b) {
        return Math.abs(value - a) < 1e-6 || Math.abs(value - b) < 1e-6;
    }

    @Test
    void openLeafLiesAlongTheHingeWallAndProjectsOneBlock() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (Box box : DoorGeometry.placedEnvelope(hinge, true)) {
                if (hinge == DoorHingeSide.LEFT) {
                    assertTrue(box.minX() >= 0 && box.maxX() <= 3, "left open leaf off its wall: " + box);
                } else {
                    assertTrue(box.minX() >= 29 && box.maxX() <= 32, "right open leaf off its wall: " + box);
                }
                assertTrue(box.minZ() >= -16 && box.maxZ() <= 16, "leaf beyond one block of projection: " + box);
            }
            assertEquals(volume(DoorGeometry.envelope()), volume(DoorGeometry.placedEnvelope(hinge, true)), EPS);
        }
    }

    @Test
    void closedCellsPartitionTheDiscAndStayInsideTheirCell() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            double total = 0;
            for (int column = 0; column < 2; column++) {
                for (int row = 0; row < 2; row++) {
                    for (Box box : DoorGeometry.cellEnvelope(hinge, column, row, false)) {
                        assertTrue(box.within(Box.cell()), "outside cell: " + box);
                        assertTrue(box.minZ() >= 13, "closed leaf out of its front slab: " + box);
                        total += box.volume();
                    }
                }
            }
            assertEquals(volume(DoorGeometry.envelope()), total, EPS, "four quarters must make the whole disc");
        }
    }

    @Test
    void openCollisionIsClippedAfterTurningAndLeaves29PixelsClear() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            int hingeColumn = hinge == DoorHingeSide.LEFT ? 0 : 1;
            double kept = 0;
            double nearest = hingeColumn == 0 ? 0 : 16;
            for (int row = 0; row < 2; row++) {
                assertTrue(DoorGeometry.cellEnvelope(hinge, 1 - hingeColumn, row, true).isEmpty(),
                        "the free column must be empty when open");
                for (Box box : DoorGeometry.cellEnvelope(hinge, hingeColumn, row, true)) {
                    assertTrue(box.within(Box.cell()), "outside cell: " + box);
                    assertTrue(hingeColumn == 0 ? box.maxX() <= 3 : box.minX() >= 13, "thicker than 3/16: " + box);
                    nearest = hingeColumn == 0 ? Math.max(nearest, box.maxX()) : Math.min(nearest, box.minX());
                    kept += box.volume();
                }
            }
            // Exactly the hinge half of the leaf stays in the cells; the other half is the visual-only projection.
            assertEquals(volume(DoorGeometry.envelope()) / 2, kept, EPS);
            double clear = hingeColumn == 0 ? 32 - nearest : 16 + nearest;
            assertEquals(29, clear, EPS, "clear width");
        }
    }

    @Test
    void closedCornersAreEmptyAndTheCentreIsSolid() {
        List<Box> lowerLeft = DoorGeometry.cellEnvelope(DoorHingeSide.LEFT, 0, 0, false);
        assertTrue(!covers(lowerLeft, 0.5, 0.5), "outer corner should be open");
        assertTrue(covers(lowerLeft, 15.5, 15.5), "centre should be solid");
        assertTrue(covers(lowerLeft, 0.5, 15.5), "disc reaches the outer edge at mid height");
    }

    private static boolean covers(List<Box> boxes, double x, double y) {
        return boxes.stream().anyMatch(b -> b.minX() <= x && x <= b.maxX() && b.minY() <= y && y <= b.maxY());
    }

    @Test
    void bothLeavesAreOneWholeDiscWithHardwareOnTheHingeSide() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            List<Piece> leaf = DoorGeometry.leaf(hinge);
            for (Piece piece : leaf) {
                Box bounds = piece.texture().equals(DoorGeometry.PANEL)
                        ? new Box(0, 0, 13, 32, 32, 16) : new Box(0, 0, 11, 32, 32, 18);
                assertTrue(piece.box().within(bounds), "leaf piece outside its bounds: " + piece);
            }
            double strapCentre = leaf.stream().filter(p -> p.texture().equals(DoorGeometry.BRASS))
                    .filter(p -> p.box().maxX() - p.box().minX() > 3) // straps are long, the handle is 2 wide
                    .mapToDouble(p -> (p.box().minX() + p.box().maxX()) / 2).average().orElseThrow();
            assertTrue(hinge == DoorHingeSide.LEFT ? strapCentre < 16 : strapCentre > 16, "straps not on the hinge side");
            long handles = leaf.stream().filter(p -> p.texture().equals(DoorGeometry.BRASS))
                    .filter(p -> p.box().maxX() - p.box().minX() <= 3).count();
            assertEquals(2, handles, "one handle on each face");
        }
        double left = DoorGeometry.leaf(DoorHingeSide.LEFT).stream().mapToDouble(p -> p.box().volume()).sum();
        double right = DoorGeometry.leaf(DoorHingeSide.RIGHT).stream().mapToDouble(p -> p.box().volume()).sum();
        assertEquals(left, right, EPS);
    }

    @Test
    void theKnobIsCentredOnBothFacesForBothHinges() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            List<Box> knobs = DoorGeometry.leaf(hinge).stream().filter(p -> p.texture().equals(DoorGeometry.BRASS))
                    .map(Piece::box).filter(b -> b.maxX() - b.minX() <= 3).toList();
            assertEquals(2, knobs.size(), hinge + ": one knob per face");
            boolean front = false;
            boolean back = false;
            for (Box knob : knobs) {
                assertEquals(16, (knob.minX() + knob.maxX()) / 2, EPS, hinge + ": knob not centred across: " + knob);
                assertEquals(16, (knob.minY() + knob.maxY()) / 2, EPS, hinge + ": knob not centred vertically: " + knob);
                assertEquals(DoorGeometry.KNOB_SIZE, knob.maxX() - knob.minX(), EPS);
                front |= knob.maxZ() == DoorGeometry.FRONT + DoorGeometry.KNOB_PROUD;
                back |= knob.minZ() == DoorGeometry.BACK - DoorGeometry.KNOB_PROUD;
            }
            assertTrue(front && back, hinge + ": a knob on each face");
        }
    }

    @Test
    void thePanelIsTheFullDiscAndNothingElseIsTextured() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            int[][] grid = new int[32][32];
            for (Piece piece : DoorGeometry.leaf(hinge)) {
                assertTrue(piece.texture().equals(DoorGeometry.PANEL) || piece.texture().equals(DoorGeometry.BRASS),
                        "unexpected texture: " + piece);
                if (!piece.texture().equals(DoorGeometry.PANEL)) {
                    continue;
                }
                Box box = piece.box();
                assertEquals(13, box.minZ(), EPS, "panel back: " + box);
                assertEquals(16, box.maxZ(), EPS, "panel front: " + box);
                for (int u = (int) box.minX(); u < (int) box.maxX(); u++) {
                    for (int v = (int) box.minY(); v < (int) box.maxY(); v++) {
                        grid[u][v]++;
                    }
                }
            }
            for (int u = 0; u < 32; u++) {
                for (int v = 0; v < 32; v++) {
                    double du = u + 0.5 - 16;
                    double dv = v + 0.5 - 16;
                    boolean inDisc = du * du + dv * dv <= 256;
                    assertEquals(inDisc ? 1 : 0, grid[u][v], hinge + ": panel coverage at " + u + "," + v);
                }
            }
        }
    }

    @Test
    void theHardwareBoxesAreExactForBothHinges() {
        List<Box> left = List.of(
                new Box(4, 8, 16, 11, 9, 16.5), new Box(4, 8, 12.5, 11, 9, 13),
                new Box(4, 23, 16, 11, 24, 16.5), new Box(4, 23, 12.5, 11, 24, 13),
                new Box(15, 15, 16, 17, 17, 18), new Box(15, 15, 11, 17, 17, 13));
        List<Box> right = List.of(
                new Box(21, 8, 16, 28, 9, 16.5), new Box(21, 8, 12.5, 28, 9, 13),
                new Box(21, 23, 16, 28, 24, 16.5), new Box(21, 23, 12.5, 28, 24, 13),
                new Box(15, 15, 16, 17, 17, 18), new Box(15, 15, 11, 17, 17, 13));
        assertHardware(left, DoorHingeSide.LEFT);
        assertHardware(right, DoorHingeSide.RIGHT);
    }

    private static void assertHardware(List<Box> expected, DoorHingeSide hinge) {
        List<Box> actual = DoorGeometry.leaf(hinge).stream().filter(p -> p.texture().equals(DoorGeometry.BRASS))
                .map(Piece::box).toList();
        assertEquals(expected.size(), actual.size(), hinge + ": brass piece count");
        for (Box box : expected) {
            assertTrue(actual.contains(box), hinge + ": missing brass box " + box + " in " + actual);
        }
    }

    @Test
    void theCollisionEnvelopeIsExactlyTheDiscSlab() {
        double volume = 0;
        for (Box box : DoorGeometry.envelope()) {
            assertEquals(13, box.minZ(), EPS, "envelope back: " + box);
            assertEquals(16, box.maxZ(), EPS, "envelope front: " + box);
            volume += box.volume();
        }
        int pixels = 0;
        for (int u = 0; u < 32; u++) {
            for (int v = 0; v < 32; v++) {
                double du = u + 0.5 - 16;
                double dv = v + 0.5 - 16;
                if (du * du + dv * dv <= 256) {
                    pixels++;
                }
            }
        }
        assertEquals(pixels * 3.0, volume, EPS, "envelope volume must be the disc, 3 deep");
    }

    @Test
    void theHardwareStaysInsideTheRenderSweepAtEveryAngle() {
        double margin = DoorGeometry.SWEEP_MARGIN;
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (Piece piece : DoorGeometry.leaf(hinge)) {
                Box box = piece.box();
                for (int degrees = 0; degrees <= 90; degrees++) {
                    double angle = DoorGeometry.openDegrees(hinge) * (degrees / 90.0);
                    for (double x : new double[] {box.minX(), box.maxX()}) {
                        for (double z : new double[] {box.minZ(), box.maxZ()}) {
                            double[] p = DoorGeometry.turn(x, z, angle, hinge);
                            String where = hinge + " " + piece.texture() + " " + box + " at " + degrees + " deg -> "
                                    + p[0] + "," + p[1];
                            assertTrue(p[0] >= -margin - EPS && p[0] <= 32 + margin + EPS, "x outside the sweep: " + where);
                            assertTrue(p[1] >= -16 - margin - EPS && p[1] <= 16 + margin + EPS, "z outside the sweep: " + where);
                        }
                    }
                }
            }
        }
    }
}
