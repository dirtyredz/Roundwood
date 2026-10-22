package io.github.dirtyredz.roundwood.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The round-headed opening (0.11.0), checked against a circle written out here independently. */
class ArchedProfileTest {
    private static final ArchedProfile DOOR = new ArchedProfile(0, 16, 24);

    @Test
    void theDoorProfileIsStraightToTheSpringLineThenARadiusEightCrown() {
        assertEquals(8, DOOR.radius(), 1e-9);
        assertEquals(32, DOOR.top(), 1e-9);
        for (int u = -2; u < 18; u++) {
            for (int v = -2; v < 34; v++) {
                boolean expected;
                if (u < 0 || u >= 16 || v < 0) {
                    expected = false;
                } else if (v < 24) {
                    expected = true;
                } else {
                    double dx = u + 0.5 - 8;
                    double dy = v + 0.5 - 24;
                    expected = dx * dx + dy * dy <= 64;
                }
                assertEquals(expected, DOOR.contains(u, v), "pixel " + u + "," + v);
            }
        }
        // The crown's top row is six pixels wide, centred.
        for (int u = 0; u < 16; u++) {
            assertEquals(u >= 5 && u <= 10, DOOR.contains(u, 31), "top row pixel " + u);
        }
    }

    @Test
    void everyProfileIsMirrorSymmetric() {
        for (ArchedProfile profile : new ArchedProfile[] {DOOR, new ArchedProfile(2, 14, 24), new ArchedProfile(2, 30, 16)}) {
            int width = (int) Math.ceil(profile.left() + profile.right());
            for (int u = 0; u < width; u++) {
                for (int v = 0; v < 40; v++) {
                    assertEquals(profile.contains(u, v), profile.contains(width - 1 - u, v), profile + " at " + u + "," + v);
                }
            }
        }
    }

    @Test
    void anInsetProfileStaysInsideItsOuter() {
        ArchedProfile inner = new ArchedProfile(2, 14, 24);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 32; v++) {
                if (inner.contains(u, v)) {
                    assertTrue(DOOR.contains(u, v), "inner pixel outside the outer " + u + "," + v);
                }
            }
        }
        assertFalse(inner.contains(1, 0));
        assertTrue(inner.contains(2, 0), "the inset opening reaches the floor");
        assertThrows(IllegalArgumentException.class, () -> new ArchedProfile(4, 4, 0));
    }
}
