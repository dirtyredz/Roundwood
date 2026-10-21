package io.github.dirtyredz.roundwood.block.door.leaf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The client swing timeline: no spurious animation, eased motion, partial ticks and mid-swing reversal. */
class DoorSwingTest {
    private static final double EPS = 1e-9;
    private static final double FULL = DoorSwing.FULL_SWING_TICKS;

    @Test
    void aNewLeafRestsAtItsStateWithoutAnimating() {
        DoorSwing open = new DoorSwing(true);
        DoorSwing closed = new DoorSwing(false);
        for (double t : new double[] {0, 0.5, 100, 1e7}) {
            assertEquals(1, open.progress(t), EPS);
            assertEquals(0, closed.progress(t), EPS);
            assertFalse(open.moving(t) || closed.moving(t));
        }
    }

    @Test
    void openingIsEasedAndTakesTheFullSwingTime() {
        DoorSwing swing = new DoorSwing(false);
        swing.retarget(true, 100);
        assertEquals(0, swing.progress(100), EPS);
        assertEquals(0.5, swing.progress(100 + FULL / 2), EPS);
        assertEquals(1, swing.progress(100 + FULL), EPS);
        assertEquals(1, swing.progress(100 + FULL * 3), EPS);
        assertTrue(swing.moving(100 + FULL - 0.01) && !swing.moving(100 + FULL));
        // Slow at both ends, fastest in the middle.
        double start = swing.progress(100 + 0.5);
        double middle = swing.progress(100 + FULL / 2 + 0.25) - swing.progress(100 + FULL / 2 - 0.25);
        assertTrue(start < middle / 2, "not eased in: " + start + " vs " + middle);
        // Monotonic, and partial ticks land between whole ticks.
        double last = -1;
        for (double t = 100; t <= 100 + FULL; t += 0.125) {
            double p = swing.progress(t);
            assertTrue(p >= last, "went backwards at " + t);
            last = p;
        }
        assertTrue(swing.progress(102.5) > swing.progress(102) && swing.progress(102.5) < swing.progress(103));
    }

    @Test
    void repeatingTheTargetDoesNotRestartTheSwing() {
        DoorSwing swing = new DoorSwing(false);
        swing.retarget(true, 0);
        double before = swing.progress(3);
        swing.retarget(true, 3);
        assertEquals(before, swing.progress(3), EPS);
        assertEquals(1, swing.progress(FULL), EPS);
    }

    @Test
    void reversingMidSwingTurnsBackFromTheCurrentAngle() {
        DoorSwing swing = new DoorSwing(false);
        swing.retarget(true, 0);
        double at = FULL / 2;
        double reached = swing.progress(at);
        swing.retarget(false, at);
        assertEquals(reached, swing.progress(at), EPS, "a reversal must not jump");
        // The way back is proportionally shorter.
        assertEquals(0, swing.progress(at + FULL * reached), EPS);
        assertTrue(swing.progress(at + 1) < reached);
        // Flipping again before it settles keeps continuity too.
        double t = at + 1;
        double p = swing.progress(t);
        swing.retarget(true, t);
        assertEquals(p, swing.progress(t), EPS);
        assertEquals(1, swing.progress(t + FULL * (1 - p)), EPS);
    }

    @Test
    void easeIsSymmetricWithFixedEnds() {
        assertEquals(0, DoorSwing.ease(0), EPS);
        assertEquals(1, DoorSwing.ease(1), EPS);
        for (double t = 0; t <= 1; t += 0.05) {
            assertEquals(1 - DoorSwing.ease(1 - t), DoorSwing.ease(t), 1e-12);
        }
    }
}
