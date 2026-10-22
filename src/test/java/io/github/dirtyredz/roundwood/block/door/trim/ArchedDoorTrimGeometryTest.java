package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.PixelSlab;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The Universal Trim on the arched door (0.11.0): a static two-pixel jamb-and-crown casing following
 * the leaf's own outline (written out here independently), with no threshold across the floor, one
 * pixel proud of each face like the round ring, its targets inside the door's column, and its
 * per-cell surfaces covering it exactly once.
 */
class ArchedDoorTrimGeometryTest {
    private static final DoorDesign ARCHED = DoorDesign.ARCHED;

    /** Inside the 16 x 32 arch and outside the same arch two pixels in (sides and crown, not the floor). */
    private static boolean expectedCasing(int u, int v) {
        return inArch(u, v, 0, 16, 8) && !inArch(u, v, 2, 14, 6);
    }

    private static boolean inArch(int u, int v, double left, double right, double radius) {
        double x = u + 0.5;
        double y = v + 0.5;
        if (y < 0 || x <= left || x >= right) {
            return false;
        }
        if (y < 24) {
            return true;
        }
        double dx = x - 8;
        double dy = y - 24;
        return dx * dx + dy * dy <= radius * radius;
    }

    @Test
    void theCasingIsTheJambsAndCrownTwoPixelsWideWithoutAThreshold() {
        for (AttachmentFace face : AttachmentFace.values()) {
            Set<String> covered = new HashSet<>();
            for (Box box : DoorTrimGeometry.ring(ARCHED, face)) {
                for (int x = (int) box.minX(); x < box.maxX(); x++) {
                    for (int y = (int) box.minY(); y < box.maxY(); y++) {
                        if (!covered.add(x + "," + y)) {
                            fail(face + ": pixel covered twice " + x + "," + y);
                        }
                    }
                }
            }
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 32; y++) {
                    boolean expected = expectedCasing(x, y);
                    assertEquals(expected, covered.contains(x + "," + y), face + ": casing at " + x + "," + y);
                    assertEquals(expected, DoorTrimGeometry.onRing(ARCHED, x + 0.5, y + 0.5), "onRing " + x + "," + y);
                }
            }
        }
        // The floor row: only the two jambs, the doorway between them open.
        for (int x = 0; x < 16; x++) {
            assertEquals(x < 2 || x >= 14, DoorTrimGeometry.inCasing(ARCHED, x, 0), "floor row at " + x);
        }
        assertTrue(DoorTrimGeometry.inCasing(ARCHED, 8, 31), "the crown's apex");
        assertTrue(DoorTrimGeometry.inCasing(ARCHED, 8, 30));
        assertFalse(DoorTrimGeometry.inCasing(ARCHED, 8, 29), "the crown is two pixels thick at the apex");
    }

    @Test
    void casingsSitOnePixelProudAndTargetInsideTheColumn() {
        assertDepth(DoorTrimGeometry.ring(ARCHED, AttachmentFace.FRONT), 16, 17);
        assertDepth(DoorTrimGeometry.ring(ARCHED, AttachmentFace.BACK), 12, 13);
        assertDepth(DoorTrimGeometry.target(ARCHED, AttachmentFace.FRONT), 15, 16);
        for (AttachmentFace face : AttachmentFace.values()) {
            for (Box box : DoorTrimGeometry.target(ARCHED, face)) {
                assertTrue(box.minX() >= 0 && box.maxX() <= 16 && box.minZ() >= 0 && box.maxZ() <= 16, face + ": target " + box);
            }
        }
        assertTrue(DoorTrimGeometry.withinRing(ARCHED, 8, 16), "the leaf is inside the casing's outer edge");
        assertFalse(DoorTrimGeometry.withinRing(ARCHED, 0.5, 31.5), "the corner beside the crown is outside");
        assertFalse(DoorTrimGeometry.withinRing(ARCHED, 16.5, 16), "nothing past the door's column");
    }

    @Test
    void perCellSurfacesCoverTheCasingFrontExactlyOnce() {
        for (int row = 0; row < 2; row++) {
            int r = row;
            double area = 0;
            for (Surface surface : PixelSlab.cellFaces((u, v) -> DoorTrimGeometry.inCasing(ARCHED, u, v), 0, 16 * row, 16, 17, "")) {
                Box b = surface.backing();
                assertTrue(b.minX() >= 0 && b.maxX() <= 16 && b.minY() >= 0 && b.maxY() <= 16, "surface outside its cell " + b);
                if (surface.face() == Direction.SOUTH) {
                    area += (b.maxX() - b.minX()) * (b.maxY() - b.minY());
                }
            }
            long pixels = 0;
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    if (expectedCasing(u, v + 16 * r)) {
                        pixels++;
                    }
                }
            }
            assertEquals(pixels, area, 1e-9, "row " + row);
        }
    }

    private static void assertDepth(List<Box> boxes, double z1, double z2) {
        assertFalse(boxes.isEmpty());
        for (Box box : boxes) {
            assertEquals(z1, box.minZ(), 1e-9, box.toString());
            assertEquals(z2, box.maxZ(), 1e-9, box.toString());
        }
    }
}
