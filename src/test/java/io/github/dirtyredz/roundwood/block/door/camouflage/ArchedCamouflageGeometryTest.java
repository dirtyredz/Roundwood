package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The arched door's camouflage (0.11.0): only the two upper exterior corners beside the crown,
 * exactly the 16 x 32 rectangle minus the leaf, all in the upper cell; the lower cell has none.
 */
class ArchedCamouflageGeometryTest {
    private static final DoorDesign ARCHED = DoorDesign.ARCHED;

    @Test
    void cornersAreTheRectangleMinusTheLeafAndOnlyBesideTheCrown() {
        List<Box> corners = CamouflageGeometry.corners(ARCHED);
        assertFalse(corners.isEmpty());
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 32; v++) {
                int n = 0;
                for (Box b : corners) {
                    if (b.containsStrictly(u + 0.5, v + 0.5, 14.5)) {
                        n++;
                    }
                }
                assertEquals(ARCHED.inLeaf(u, v) ? 0 : 1, n, "pixel " + u + "," + v);
                if (v < DoorDesign.ARCH_SPRING) {
                    assertEquals(0, n, "a corner below the spring line at " + u + "," + v);
                }
            }
        }
        // The two exterior corners: top-left and top-right pixels are corner, the crown's top centre is leaf.
        assertTrue(CamouflageGeometry.inCorner(ARCHED, 0, 31));
        assertTrue(CamouflageGeometry.inCorner(ARCHED, 15, 31));
        assertFalse(CamouflageGeometry.inCorner(ARCHED, 8, 31));
        assertFalse(CamouflageGeometry.inCorner(ARCHED, 16, 31), "nothing outside the door's column");
    }

    @Test
    void onlyTheUpperCellHasACorner() {
        assertTrue(CamouflageGeometry.quarter(ARCHED, 0, 0).isEmpty(), "the lower cell has no corner");
        assertTrue(CamouflageGeometry.exposedFaces(ARCHED, 0, 0).isEmpty());
        List<Box> upper = CamouflageGeometry.quarter(ARCHED, 0, 1);
        assertFalse(upper.isEmpty());
        double area = 0;
        for (Box box : upper) {
            assertTrue(box.within(Box.cell()), "outside its cell: " + box);
            area += (box.maxX() - box.minX()) * (box.maxY() - box.minY());
        }
        double front = 0;
        for (CamouflageGeometry.Face face : CamouflageGeometry.exposedFaces(ARCHED, 0, 1)) {
            if (face.side() == CamouflageGeometry.Side.SOUTH) {
                front += (face.box().maxX() - face.box().minX()) * (face.box().maxY() - face.box().minY());
            }
        }
        assertEquals(area, front, 1e-9, "front faces cover the corners exactly once");
    }

    @Test
    void cornerHitsNeverClaimTheLeaf() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            assertTrue(CamouflageGeometry.isCornerHit(ARCHED, hinge, false, 0.5, 31.5, 16));
            assertTrue(CamouflageGeometry.isCornerHit(ARCHED, hinge, true, 15.5, 31.5, 13.5), "an open door's far corner stays");
            assertFalse(CamouflageGeometry.isCornerHit(ARCHED, hinge, false, 8, 31.5, 14), "the crown is leaf");
            assertFalse(CamouflageGeometry.isCornerHit(ARCHED, hinge, false, 0.5, 4, 14), "no corner low down");
            // Open, the leaf stands along the hinge side and crosses the hinge-side corner near the crown's shoulder.
            double hingeX = DoorGeometry.pivotX(ARCHED, hinge);
            assertTrue(CamouflageGeometry.inCorner(ARCHED, (int) Math.floor(hingeX), 29));
            assertFalse(CamouflageGeometry.isCornerHit(ARCHED, hinge, true, hingeX, 29.5, 13.5));
            assertTrue(CamouflageGeometry.isCornerHit(ARCHED, hinge, false, hingeX, 29.5, 13.5), "closed, the same point is corner");
        }
    }
}
