package io.github.dirtyredz.roundwood.block.attachment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The one depth-to-face rule every host's targeting shares ({@link AttachmentFace#at}) and its proud constant. */
class AttachmentFaceTest {
    @Test
    void belowTheMiddleIsTheBackAndTheMiddleItselfIsTheFront() {
        assertEquals(AttachmentFace.BACK, AttachmentFace.at(7.999, 8));
        assertEquals(AttachmentFace.FRONT, AttachmentFace.at(8, 8));
        assertEquals(AttachmentFace.FRONT, AttachmentFace.at(8.001, 8));
        assertEquals(AttachmentFace.BACK, AttachmentFace.at(-1, 8));
        assertEquals(AttachmentFace.FRONT, AttachmentFace.at(17, 8));
    }

    @Test
    void theDoorAndArchwayMiddlesKeepTheirTies() {
        assertEquals(AttachmentFace.FRONT, AttachmentFace.at(14.5, 14.5));
        assertEquals(AttachmentFace.BACK, AttachmentFace.at(14.49, 14.5));
    }

    @Test
    void anAttachmentStandsOnePixelProud() {
        assertEquals(1.0, AttachmentFace.PROUD);
    }
}
