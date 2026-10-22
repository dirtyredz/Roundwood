package io.github.dirtyredz.roundwood.geometry;

/**
 * A round-headed opening on the pixel grid (0.11.0): straight sides from the floor up to the spring
 * line, then a semicircular crown whose diameter is the opening's width. Pixels follow the
 * {@link PixelCircle} convention (a pixel belongs when its centre lies inside), so every profile is
 * mirror-symmetric about its centre line. Shared by the arched door's leaf and casing and, from the
 * next release, the archways. Plain Java.
 *
 * @param left   x of the left side, pixels
 * @param right  x of the right side, pixels
 * @param spring y where the straight sides end and the crown begins (its centre height), pixels
 */
public record ArchedProfile(double left, double right, double spring) {
    public ArchedProfile {
        if (!(left < right)) {
            throw new IllegalArgumentException("Empty arched profile " + left + ".." + right);
        }
    }

    /** The crown's radius: half the opening's width. */
    public double radius() {
        return (right - left) / 2;
    }

    /** The crown's apex height. */
    public double top() {
        return spring + radius();
    }

    /** True when pixel (u, v)'s centre lies inside the profile (any v at or above the floor, 0). */
    public boolean contains(int u, int v) {
        double x = u + 0.5;
        double y = v + 0.5;
        if (y < 0 || x <= left || x >= right) {
            return false;
        }
        return y < spring || PixelCircle.inCircle((left + right) / 2, spring, right - left, u, v);
    }
}
