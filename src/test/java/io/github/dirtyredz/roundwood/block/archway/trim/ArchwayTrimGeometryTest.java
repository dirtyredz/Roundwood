package io.github.dirtyredz.roundwood.block.archway.trim;

import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayGeometry;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.geometry.Box;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The archway casings (0.12.0): the whole frame face, one pixel proud of the front or back, never inside the frame. */
class ArchwayTrimGeometryTest {
    @Test
    void casingsCoverTheFrameFaceOnePixelProud() {
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (int u = 0; u < 32; u++) {
                for (int v = 0; v < 32; v++) {
                    assertEquals(design.inFrame(u, v), ArchwayTrimGeometry.inCasing(design, u, v), design + " " + u + "," + v);
                }
            }
            assertEquals(ArchwayGeometry.rects(design).size(), ArchwayTrimGeometry.casing(design, AttachmentFace.FRONT).size());
            for (Box box : ArchwayTrimGeometry.casing(design, AttachmentFace.FRONT)) {
                assertEquals(16, box.minZ(), 1e-9);
                assertEquals(17, box.maxZ(), 1e-9);
            }
            for (Box box : ArchwayTrimGeometry.casing(design, AttachmentFace.BACK)) {
                assertEquals(-1, box.minZ(), 1e-9);
                assertEquals(0, box.maxZ(), 1e-9);
            }
        }
    }

    @Test
    void theNearerFaceIsChosenByDepth() {
        assertEquals(AttachmentFace.FRONT, AttachmentFace.at(15.9999, ArchwayTrimGeometry.MIDDLE));
        assertEquals(AttachmentFace.FRONT, AttachmentFace.at(8, ArchwayTrimGeometry.MIDDLE));
        assertEquals(AttachmentFace.BACK, AttachmentFace.at(7.9, ArchwayTrimGeometry.MIDDLE));
        assertEquals(AttachmentFace.BACK, AttachmentFace.at(0.0001, ArchwayTrimGeometry.MIDDLE));
        assertTrue(AttachmentFace.PROUD > 0);
    }
}
