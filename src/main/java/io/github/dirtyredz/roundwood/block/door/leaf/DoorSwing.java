package io.github.dirtyredz.roundwood.block.door.leaf;

/**
 * The swing timeline shared by rendering and server hardware eligibility, in ticks. Plain Java so the
 * maths is unit-tested; blockstate {@code open} and collision still switch
 * instantly.
 *
 * <p>Progress runs from 0 (closed) to 1 (open). A full swing takes {@link #FULL_SWING_TICKS} with
 * a sine ease-in-out. Retargeting mid-swing starts from the current progress, so a reversal turns
 * back from where the leaf is, over the proportionally shorter distance.
 */
public final class DoorSwing {
    /** Ticks for a full 90 degree swing (vanilla doors snap; this keeps the same snappy feel). */
    public static final double FULL_SWING_TICKS = 8.0;

    private double from;
    private double to;
    private double start;
    private double duration;

    /** A leaf resting at its current state: no animation on chunk load or first render. */
    public DoorSwing(boolean open) {
        from = to = open ? 1 : 0;
    }

    /** The state changed to {@code open} at time {@code now} (ticks). Repeating the current target is a no-op. */
    public void retarget(boolean open, double now) {
        double target = open ? 1 : 0;
        if (target == to) {
            return;
        }
        double current = progress(now);
        from = current;
        to = target;
        start = now;
        duration = FULL_SWING_TICKS * Math.abs(to - from);
    }

    /** Progress at time {@code now} (ticks, with partial tick), 0 closed .. 1 open. */
    public double progress(double now) {
        if (duration <= 0) {
            return to;
        }
        double t = (now - start) / duration;
        if (t >= 1) {
            return to;
        }
        if (t <= 0) {
            return from;
        }
        return from + (to - from) * ease(t);
    }

    /** True while the leaf is still moving at {@code now}. */
    public boolean moving(double now) {
        return duration > 0 && now - start < duration;
    }

    /** Sine ease-in-out on [0, 1]: zero speed at both ends. */
    static double ease(double t) {
        return (1 - Math.cos(Math.PI * t)) / 2;
    }
}
