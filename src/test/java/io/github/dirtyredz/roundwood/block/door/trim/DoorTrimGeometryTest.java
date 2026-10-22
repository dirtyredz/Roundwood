package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.geometry.Box;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The door's trim rings (0.8.0) in the door's authored frame: the 32/28 ring (written out here
 * independently) one pixel proud of the closed leaf ({@code z 13..16}) on each face, FRONT at
 * {@code 16..17} and BACK at {@code 12..13}; the rays' targets stay inside the door's own cells; a
 * point's depth picks its face at the leaf's middle.
 */
class DoorTrimGeometryTest {
    @Test
    void ringsSitOnePixelProudOfEachFace() {
        assertDepth(DoorTrimGeometry.ring(DoorDesign.ROUND, AttachmentFace.FRONT), 16, 17);
        assertDepth(DoorTrimGeometry.ring(DoorDesign.ROUND, AttachmentFace.BACK), 12, 13);
        assertDepth(DoorTrimGeometry.target(DoorDesign.ROUND, AttachmentFace.FRONT), 15, 16);
        assertDepth(DoorTrimGeometry.target(DoorDesign.ROUND, AttachmentFace.BACK), 12, 13);
    }

    @Test
    void theRingIsTheThirtyTwoTwentyEightProfile() {
        for (AttachmentFace face : AttachmentFace.values()) {
            Set<String> covered = new HashSet<>();
            for (Box box : DoorTrimGeometry.ring(DoorDesign.ROUND, face)) {
                for (int x = (int) box.minX(); x < box.maxX(); x++) {
                    for (int y = (int) box.minY(); y < box.maxY(); y++) {
                        if (!covered.add(x + "," + y)) {
                            fail(face + ": pixel covered twice " + x + "," + y);
                        }
                    }
                }
            }
            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 32; y++) {
                    boolean expected = expectedRing(x, y);
                    if (expected != covered.contains(x + "," + y) || expected != DoorTrimGeometry.onRing(DoorDesign.ROUND, x + 0.5, y + 0.5)) {
                        fail(face + ": ring mismatch at " + x + "," + y);
                    }
                }
            }
        }
    }

    @Test
    void depthPicksTheFaceAndTargetsStayInTheCells() {
        assertEquals(AttachmentFace.FRONT, DoorTrimGeometry.face(15.9));
        assertEquals(AttachmentFace.FRONT, DoorTrimGeometry.face(14.5));
        assertEquals(AttachmentFace.BACK, DoorTrimGeometry.face(14.49));
        assertEquals(AttachmentFace.BACK, DoorTrimGeometry.face(12.1));
        for (AttachmentFace face : AttachmentFace.values()) {
            for (Box box : DoorTrimGeometry.target(DoorDesign.ROUND, face)) {
                assertTrue(box.minZ() >= 0 && box.maxZ() <= 16, face + ": target leaves the door's cells " + box);
            }
        }
        assertTrue(DoorTrimGeometry.inTargetDepth(AttachmentFace.FRONT, 15.5));
        assertFalse(DoorTrimGeometry.inTargetDepth(AttachmentFace.FRONT, 14));
        assertTrue(DoorTrimGeometry.inTargetDepth(AttachmentFace.BACK, 12.5));
        assertTrue(DoorTrimGeometry.withinRing(DoorDesign.ROUND, 16, 16), "the disc centre is inside the ring");
        assertFalse(DoorTrimGeometry.onRing(DoorDesign.ROUND, 16, 16));
        assertFalse(DoorTrimGeometry.withinRing(DoorDesign.ROUND, 0.5, 0.5), "a corner is outside");
        assertFalse(DoorTrimGeometry.withinRing(DoorDesign.ROUND, -0.5, 16));
    }

    private static boolean expectedRing(int x, int y) {
        double dx = x + 0.5 - 16;
        double dy = y + 0.5 - 16;
        double d = dx * dx + dy * dy;
        return d <= 256 && d > 196;
    }

    private static void assertDepth(List<Box> boxes, double z1, double z2) {
        assertFalse(boxes.isEmpty());
        for (Box box : boxes) {
            assertEquals(z1, box.minZ(), 1e-9, box.toString());
            assertEquals(z2, box.maxZ(), 1e-9, box.toString());
        }
    }
}
