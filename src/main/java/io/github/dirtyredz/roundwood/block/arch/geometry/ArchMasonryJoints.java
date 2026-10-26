package io.github.dirtyredz.roundwood.block.arch.geometry;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Pure authored-space mortar layout shared by later body, casing and item baking.
 * Distances are pixels. The classifier clips to the actual raster body, then to the
 * two-pixel analytic crown band; it never creates seams at block/cell boundaries.
 * Material selection belongs to the model adapter, not this geometry.
 */
public final class ArchMasonryJoints {
    public static final double WIDTH = 0.25;
    public static final List<Double> SHOULDER_LEVELS = List.of(0.0, 4.0, 8.0, 12.0, 16.0);

    private ArchMasonryJoints() {}

    public static int voussoirs(ArchPieceDesign design) {
        if (design.shoulder()) throw new IllegalArgumentException("Shoulders have horizontal joints");
        return 4 * design.columns() + 1;
    }

    /** Internal radial boundaries only; clipped to the corner fill (there are no spring bearings). */
    public static List<Double> radialAngles(ArchPieceDesign design) {
        int count = voussoirs(design);
        return IntStream.range(1, count).mapToObj(k -> k * Math.PI / count).toList();
    }

    /** Whether a front/back surface point is mortar rather than unmodified donor artwork. */
    public static boolean contains(ArchPieceDesign design, KeyholeShoulderProfile.Side side, double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || !design.contains(side, (int) Math.floor(x), (int) Math.floor(y))) return false;
        if (design.shoulder()) {
            return SHOULDER_LEVELS.stream().anyMatch(level -> Math.abs(y - level) <= WIDTH / 2);
        }
        double dx = x - design.width() / 2.0;
        double radius = design.top().radius();
        double dy = y - design.top().spring();
        double distanceSquared = dx * dx + dy * dy;
        if (distanceSquared < radius * radius || distanceSquared > (radius + 2) * (radius + 2)) return false;
        for (double angle : radialAngles(design)) {
            double along = dx * Math.cos(angle) + dy * Math.sin(angle);
            double across = dx * Math.sin(angle) - dy * Math.cos(angle);
            if (along >= 0 && Math.abs(across) <= WIDTH / 2) return true;
        }
        return false;
    }
}
