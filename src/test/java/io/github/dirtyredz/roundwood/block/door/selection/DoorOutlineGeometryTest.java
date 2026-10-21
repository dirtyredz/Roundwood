package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageGeometry;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each part's outline, put back into world space, must be the whole door: the same set of boxes
 * from all four parts, whatever the facing, hinge or pose. World space here is pixels from the
 * anchor cell's origin, the way a block's shape is placed at its own position.
 */
class DoorOutlineGeometryTest {
    private static final double EPS = 1e-9;

    /** A cell-local shape (facing north) turned like a blockstate and moved to its cell's world offset. */
    private static Set<Box> world(List<Box> cellLocal, int turns, int column, int row) {
        // The column offset is the assembly's left-to-right axis, turned with the facing.
        double ox = 16 * column;
        double oz = 0;
        for (int i = 0; i < turns; i++) {
            double t = ox;
            ox = -oz;
            oz = t;
        }
        Set<Box> out = new HashSet<>();
        for (Box box : cellLocal) {
            Box turned = box.rotateY(turns);
            out.add(round(turned.translate(ox, 16 * row, oz)));
        }
        return out;
    }

    private static Box round(Box b) {
        return new Box(r(b.minX()), r(b.minY()), r(b.minZ()), r(b.maxX()), r(b.maxY()), r(b.maxZ()));
    }

    private static double r(double v) {
        return Math.round(v * 1024) / 1024.0;
    }

    @Test
    void cellClippedShapesDifferPerCellWhichWasTheQuarterOutline() {
        // The 0.2.0 outline was each cell's clipped collision piece: four different shapes.
        Set<Set<Box>> distinct = new HashSet<>();
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                distinct.add(world(DoorGeometry.cellEnvelope(DoorHingeSide.LEFT, column, row, false), 0, column, row));
            }
        }
        assertEquals(4, distinct.size(), "the clipped pieces are quarters, not the door");
    }

    @Test
    void everyPartOutlinesTheSameWholeDoor() {
        for (int turns = 0; turns < 4; turns++) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                for (boolean open : new boolean[] {false, true}) {
                    for (boolean corners : new boolean[] {false, true}) {
                        List<Box> outline = DoorOutlineGeometry.outline(hinge, open, corners);
                        Set<Box> expected = null;
                        for (int column = 0; column < 2; column++) {
                            for (int row = 0; row < 2; row++) {
                                Set<Box> shape = world(DoorOutlineGeometry.relativeTo(outline, column, row), turns, column, row);
                                if (expected == null) {
                                    expected = shape;
                                }
                                assertEquals(expected, shape, "turns " + turns + " " + hinge + " open=" + open
                                        + " corners=" + corners + ": cell " + column + "," + row + " outlines something else");
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void closedOutlineIsTheWholeDiscAndOpenIncludesTheProjection() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            Box closed = bounds(DoorOutlineGeometry.outline(hinge, false, false));
            assertEquals(new Box(0, 0, DoorGeometry.BACK, 32, 32, DoorGeometry.FRONT), closed, hinge + " closed bounds");
            Box open = bounds(DoorOutlineGeometry.outline(hinge, true, false));
            // The open leaf lies along the hinge wall and reaches one block past the cells (z -16).
            assertEquals(-16, open.minZ(), EPS, hinge + ": projection missing from the open outline");
            assertEquals(16, open.maxZ(), EPS);
            assertTrue(hinge == DoorHingeSide.LEFT ? open.maxX() <= 3 : open.minX() >= 29, hinge + ": open leaf off its wall");
            double volume = DoorOutlineGeometry.outline(hinge, true, false).stream().mapToDouble(Box::volume).sum();
            double disc = DoorGeometry.envelope().stream().mapToDouble(Box::volume).sum();
            assertEquals(disc, volume, EPS, "the outline is the whole leaf, not a clipped part");
        }
    }

    @Test
    void cornersAddTheStaticSquareInEveryPose() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (boolean open : new boolean[] {false, true}) {
                List<Box> plain = DoorOutlineGeometry.outline(hinge, open, false);
                List<Box> withCorners = DoorOutlineGeometry.outline(hinge, open, true);
                assertEquals(plain.size() + CamouflageGeometry.corners().size(), withCorners.size());
                assertTrue(withCorners.containsAll(CamouflageGeometry.corners()), "corners move with the pose");
            }
        }
        assertNotEquals(DoorOutlineGeometry.outline(DoorHingeSide.LEFT, false, false),
                DoorOutlineGeometry.outline(DoorHingeSide.LEFT, false, true));
    }

    private static Box bounds(List<Box> boxes) {
        double x1 = Double.MAX_VALUE;
        double y1 = Double.MAX_VALUE;
        double z1 = Double.MAX_VALUE;
        double x2 = -Double.MAX_VALUE;
        double y2 = -Double.MAX_VALUE;
        double z2 = -Double.MAX_VALUE;
        for (Box b : boxes) {
            x1 = Math.min(x1, b.minX());
            y1 = Math.min(y1, b.minY());
            z1 = Math.min(z1, b.minZ());
            x2 = Math.max(x2, b.maxX());
            y2 = Math.max(y2, b.maxY());
            z2 = Math.max(z2, b.maxZ());
        }
        return new Box(x1, y1, z1, x2, y2, z2);
    }
}
