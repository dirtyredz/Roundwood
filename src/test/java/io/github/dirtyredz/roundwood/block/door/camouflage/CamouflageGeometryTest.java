package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The four static corners: exactly the square minus the disc, at the closed leaf's depth. */
class CamouflageGeometryTest {
    private static final double EPS = 1e-9;

    private static int covering(List<Box> boxes, double x, double y, double z) {
        int n = 0;
        for (Box b : boxes) {
            if (b.minX() < x && x < b.maxX() && b.minY() < y && y < b.maxY() && b.minZ() < z && z < b.maxZ()) {
                n++;
            }
        }
        return n;
    }

    @Test
    void cornersAreExactlyTheComplementOfTheDiscWithoutGapsOrOverlaps() {
        List<Box> corners = CamouflageGeometry.corners(DoorDesign.ROUND);
        for (int u = 0; u < 32; u++) {
            for (int v = 0; v < 32; v++) {
                boolean disc = PixelCircle.inDisc(32, 32, u, v);
                assertEquals(disc ? 0 : 1, covering(corners, u + 0.5, v + 0.5, 14.5), "pixel " + u + "," + v);
                // The leaf's disc covers exactly the rest: together they fill the square once.
                assertEquals(1, covering(corners, u + 0.5, v + 0.5, 14.5) + covering(DoorGeometry.envelope(DoorDesign.ROUND), u + 0.5, v + 0.5, 14.5));
            }
        }
    }

    @Test
    void cornersHaveTheClosedLeafDepth() {
        for (Box box : CamouflageGeometry.corners(DoorDesign.ROUND)) {
            assertEquals(DoorGeometry.BACK, box.minZ(), EPS);
            assertEquals(DoorGeometry.FRONT, box.maxZ(), EPS);
            assertTrue(box.within(new Box(0, 0, 0, 32, 32, 16)), "outside the assembly: " + box);
        }
    }

    @Test
    void quartersPartitionTheCornersAndStayInTheirCell() {
        double total = CamouflageGeometry.corners(DoorDesign.ROUND).stream().mapToDouble(Box::volume).sum();
        double sum = 0;
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                List<Box> quarter = CamouflageGeometry.quarter(DoorDesign.ROUND, column, row);
                assertFalse(quarter.isEmpty(), "every cell has a corner");
                for (Box box : quarter) {
                    assertTrue(box.within(Box.cell()), "outside its cell: " + box);
                    sum += box.volume();
                }
                // Each cell's corner is the outer corner of the square.
                double cx = column == 0 ? 0.5 : 15.5;
                double cy = row == 0 ? 0.5 : 15.5;
                assertEquals(1, covering(quarter, cx, cy, 14.5), "outer corner of cell " + column + "," + row);
            }
        }
        assertEquals(total, sum, EPS);
    }

    @Test
    void exposedFacesCoverTheFrontAndBackAndOnlyBoundarySides() {
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                double front = 0;
                for (CamouflageGeometry.Face face : CamouflageGeometry.exposedFaces(DoorDesign.ROUND, column, row)) {
                    Box b = face.box();
                    assertTrue(b.within(Box.cell()), "face outside its cell: " + face);
                    if (face.side() == CamouflageGeometry.Side.SOUTH) {
                        front += (b.maxX() - b.minX()) * (b.maxY() - b.minY());
                    }
                    // A side face is exposed: the pixel beyond it is not corner.
                    int u = (int) b.minX() + 16 * column;
                    int v = (int) b.minY() + 16 * row;
                    switch (face.side()) {
                        case WEST -> assertFalse(CamouflageGeometry.inCorner(DoorDesign.ROUND, u - 1, v), "buried west face " + face);
                        case EAST -> assertFalse(CamouflageGeometry.inCorner(DoorDesign.ROUND, (int) b.maxX() + 16 * column, v), "buried east face " + face);
                        case DOWN -> assertFalse(CamouflageGeometry.inCorner(DoorDesign.ROUND, u, v - 1), "buried down face " + face);
                        case UP -> assertFalse(CamouflageGeometry.inCorner(DoorDesign.ROUND, u, (int) b.maxY() + 16 * row), "buried up face " + face);
                        default -> {
                        }
                    }
                }
                double area = CamouflageGeometry.quarter(DoorDesign.ROUND, column, row).stream()
                        .mapToDouble(b -> (b.maxX() - b.minX()) * (b.maxY() - b.minY())).sum();
                assertEquals(area, front, EPS, "front faces cover the corner exactly once");
            }
        }
    }

    @Test
    void hitsClassifyCornersButNeverTheLeaf() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (boolean open : new boolean[] {false, true}) {
                // The four outer corners at the front face.
                assertTrue(CamouflageGeometry.isCornerHit(DoorDesign.ROUND, hinge, open, 0.5, 0.5, 16));
                assertTrue(CamouflageGeometry.isCornerHit(DoorDesign.ROUND, hinge, open, 31.5, 31.5, 13));
                // Off the corner's depth, outside the square, or on the disc.
                assertFalse(CamouflageGeometry.isCornerHit(DoorDesign.ROUND, hinge, open, 0.5, 0.5, 12));
                assertFalse(CamouflageGeometry.isCornerHit(DoorDesign.ROUND, hinge, open, -0.5, 0.5, 14));
                assertFalse(CamouflageGeometry.isCornerHit(DoorDesign.ROUND, hinge, open, 16, 16, 15.9));
            }
            // The open leaf near its hinge is the leaf, even where it crosses the corner region.
            double inLeafX = hinge == DoorHingeSide.LEFT ? 1.5 : 30.5;
            assertFalse(CamouflageGeometry.isCornerHit(DoorDesign.ROUND, hinge, true, inLeafX, 16, 14.5));
        }
    }
}
