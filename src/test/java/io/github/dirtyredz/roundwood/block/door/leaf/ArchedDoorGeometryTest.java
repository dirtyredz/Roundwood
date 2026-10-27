package io.github.dirtyredz.roundwood.block.door.leaf;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The arched door's leaf (0.11.0): 16 x 32 pixels, straight sides and a flat bottom, a radius-eight
 * crown centred at (8, 24) (written out here independently), the round door's depth and swing axis
 * inset, hardware on both faces, and an open leaf that stays inside its own column, leaving 13/16
 * of it clear.
 */
class ArchedDoorGeometryTest {
    private static final DoorDesign ARCHED = DoorDesign.ARCHED;
    private static final double EPS = 1e-9;

    private static boolean expectedLeaf(int u, int v) {
        if (u < 0 || u >= 16 || v < 0 || v >= 32) {
            return false;
        }
        if (v < 24) {
            return true;
        }
        double dx = u + 0.5 - 8;
        double dy = v + 0.5 - 24;
        return dx * dx + dy * dy <= 64;
    }

    private static int covering(List<Box> boxes, double x, double y, double z) {
        int n = 0;
        for (Box b : boxes) {
            if (b.containsStrictly(x, y, z)) {
                n++;
            }
        }
        return n;
    }

    @Test
    void theDesignIsOneColumnSixteenByThirtyTwo() {
        assertEquals(1, ARCHED.columns());
        assertEquals(16, ARCHED.width());
        assertEquals(32, DoorDesign.HEIGHT);
        assertEquals("arched_door", ARCHED.id());
        assertEquals(2, DoorDesign.ROUND.columns(), "the round door is unchanged");
        assertEquals(32, DoorDesign.ROUND.width());
    }

    @Test
    void theLeafFillsExactlyTheArchedOutlineAtTheLeafDepth() {
        List<Box> envelope = DoorGeometry.envelope(ARCHED);
        for (Box box : envelope) {
            assertEquals(DoorGeometry.BACK, box.minZ(), EPS);
            assertEquals(DoorGeometry.FRONT, box.maxZ(), EPS);
            assertTrue(DoorGeometry.insideCells(ARCHED, box), "outside the column: " + box);
        }
        for (int u = -1; u < 17; u++) {
            for (int v = -1; v < 33; v++) {
                assertEquals(expectedLeaf(u, v) ? 1 : 0, covering(envelope, u + 0.5, v + 0.5, 14.5), "pixel " + u + "," + v);
                assertEquals(expectedLeaf(u, v), ARCHED.inLeaf(u, v), "inLeaf " + u + "," + v);
            }
        }
    }

    @Test
    void pivotsSitInTheHingeSideThicknessOfTheNarrowLeaf() {
        assertEquals(1.5, DoorGeometry.pivotX(ARCHED, DoorHingeSide.LEFT), EPS);
        assertEquals(14.5, DoorGeometry.pivotX(ARCHED, DoorHingeSide.RIGHT), EPS);
        assertEquals(14.5, DoorGeometry.PIVOT_Z, EPS);
        assertEquals(30.5, DoorGeometry.pivotX(DoorDesign.ROUND, DoorHingeSide.RIGHT), EPS, "the round door is unchanged");
    }

    @Test
    void theOpenLeafStaysInItsColumnAndLeavesThirteenSixteenthsClear() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            List<Box> open = DoorGeometry.placedEnvelope(ARCHED, hinge, true);
            double minX = open.stream().mapToDouble(Box::minX).min().orElseThrow();
            double maxX = open.stream().mapToDouble(Box::maxX).max().orElseThrow();
            for (Box box : open) {
                assertTrue(DoorGeometry.insideCells(ARCHED, box), hinge + ": open leaf leaves its cells " + box);
                assertTrue(DoorGeometry.projection(box).isEmpty(), hinge + ": open leaf projects " + box);
            }
            assertEquals(3, maxX - minX, EPS, hinge + ": the open leaf is 3 pixels thick");
            double clear = hinge == DoorHingeSide.LEFT ? 16 - maxX : minX;
            assertEquals(13, clear, EPS, hinge + ": clear width");
            // Collision is the same open leaf, clipped to the two cells: nothing in the free 13 pixels.
            for (int row = 0; row < 2; row++) {
                for (Box box : DoorGeometry.cellEnvelope(ARCHED, hinge, 0, row, true)) {
                    assertTrue(hinge == DoorHingeSide.LEFT ? box.maxX() <= 3 + EPS : box.minX() >= 13 - EPS,
                            hinge + " row " + row + ": collision in the doorway " + box);
                }
            }
        }
    }

    @Test
    void closedCollisionIsTheLeafSlabClippedPerCell() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            double area = 0;
            for (int row = 0; row < 2; row++) {
                for (Box box : DoorGeometry.cellEnvelope(ARCHED, hinge, 0, row, false)) {
                    assertTrue(box.within(Box.cell()), "outside its cell: " + box);
                    assertTrue(box.minZ() >= DoorGeometry.BACK - EPS && box.maxZ() <= DoorGeometry.FRONT + EPS, "hardware collides: " + box);
                    area += (box.maxX() - box.minX()) * (box.maxY() - box.minY());
                }
            }
            double leafArea = DoorGeometry.envelope(ARCHED).stream().mapToDouble(b -> (b.maxX() - b.minX()) * (b.maxY() - b.minY())).sum();
            assertEquals(leafArea, area, EPS, hinge + ": the cells' collision covers the leaf exactly");
        }
    }

    @Test
    void hardwareIsOnTheHingeSideOfBothFacesAndMirrors() {
        List<Piece> left = DoorGeometry.leaf(ARCHED, DoorHingeSide.LEFT);
        List<Piece> right = DoorGeometry.leaf(ARCHED, DoorHingeSide.RIGHT);
        List<Box> brass = left.stream().filter(p -> p.texture().equals(DoorGeometry.BRASS)).map(Piece::box).toList();
        assertTrue(brass.size() > 6, "spear straps have stepped silhouettes");
        long front = brass.stream().filter(b -> b.minZ() >= DoorGeometry.FRONT - EPS).count();
        long back = brass.stream().filter(b -> b.maxZ() <= DoorGeometry.BACK + EPS).count();
        assertEquals(brass.size()/2, front);
        assertEquals(front, back);
        for (Box strap : brass.stream().filter(b -> b.maxZ() - b.minZ() == .5).toList()) {
            assertTrue(strap.minX() < 4, "a left-hinge strap does not start at the hinge edge: " + strap);
        }
        for (Box box : brass) {
            assertTrue(box.minX() >= 0 && box.maxX() <= 16 && box.minY() >= 0 && box.maxY() <= 32, "hardware off the leaf: " + box);
        }
        assertEquals(left.size(), right.size());
        for (int i = 0; i < left.size(); i++) {
            Box l = left.get(i).box();
            Box r = right.get(i).box();
            assertEquals(16 - l.maxX(), r.minX(), EPS);
            assertEquals(16 - l.minX(), r.maxX(), EPS);
            assertEquals(l.minY(), r.minY(), EPS);
            assertEquals(l.minZ(), r.minZ(), EPS);
        }
    }

    @Test
    void theRoundDiscIsUnchanged() {
        for (int u = 0; u < 32; u++) {
            for (int v = 0; v < 32; v++) {
                double dx = u + 0.5 - 16;
                double dy = v + 0.5 - 16;
                assertEquals(dx * dx + dy * dy <= 256, DoorDesign.ROUND.inLeaf(u, v), "round pixel " + u + "," + v);
            }
        }
        assertFalse(DoorDesign.ROUND.inLeaf(0, 0));
    }
}
