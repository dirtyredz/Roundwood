package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Continuous interval bounds, including all hardware and both hinges, never angle samples. */
public final class DoorArchClearance {
    public static final double GAP = 1.0 / 32;
    private static final int INTERVALS = 32;
    private static final ConcurrentHashMap<DoorDesign, double[][]> REBATES = new ConcurrentHashMap<>();
    private DoorArchClearance() {}

    private static double back(DoorDesign design, int x, int y) { return REBATES.computeIfAbsent(design, DoorArchClearance::rebate)[x][y]; }
    public static boolean accepts(DoorDesign design, ArchPieceThickness thickness, ArchPieceDepth depth) {
        if (thickness == ArchPieceThickness.FULL) return depth == ArchPieceDepth.FRONT;
        if (depth == ArchPieceDepth.BACK) return false;
        var top = DoorArchFitPlan.topFor(design);
        for (int x = 0; x < top.width(); x++) for (int y = 0; y < top.height(); y++)
            if (top.contains(Side.LEFT, x, y) && depth.back() < back(design, x, y)) return false;
        return true;
    }
    private static double[][] rebate(DoorDesign design) {
        var top = DoorArchFitPlan.topFor(design);
        var result = new double[top.width()][top.height()];
        var sweep = sweep(design);
        int offset = 16 * DoorArchFitPlan.topOffset(design);
        for (int x = 0; x < top.width(); x++) for (int y = 0; y < top.height(); y++) {
            if (!top.contains(Side.LEFT, x, y)) continue;
            double max = Double.NEGATIVE_INFINITY;
            for (Box bound : sweep) if (bound.maxX() > x - GAP && bound.minX() < x + 1 + GAP
                    && bound.maxY() > y + offset - GAP && bound.minY() < y + offset + 1 + GAP)
                max = Math.max(max, bound.maxZ());
            // The rear casing occupies [back-1, back]. Normal separation is smaller than GAP.
            result[x][y] = Math.max(0, max + 1 + GAP);
            if (result[x][y] >= 16) throw new IllegalStateException("No material remains behind the fitted sweep");
        }
        return result;
    }
    private static List<Box> sweep(DoorDesign design) {
        var out = new ArrayList<Box>();
        for (var hinge : DoorHingeSide.values()) for (var piece : DoorGeometry.leaf(design, hinge)) {
            Box box = piece.box();
            double pivot = DoorGeometry.pivotX(design, hinge), sign = hinge == DoorHingeSide.LEFT ? 1 : -1;
            for (int i = 0; i < INTERVALS; i++) {
                double lo = i * Math.PI / (2 * INTERVALS), hi = (i + 1) * Math.PI / (2 * INTERVALS);
                double minX = Double.POSITIVE_INFINITY, minZ = minX, maxX = Double.NEGATIVE_INFINITY, maxZ = maxX;
                for (double x : new double[]{box.minX(), box.maxX()}) for (double z : new double[]{box.minZ(), box.maxZ()}) {
                    double dx = x - pivot, dz = z - DoorGeometry.PIVOT_Z;
                    var xr = range(dx, sign * dz, lo, hi);
                    var zr = range(dz, -sign * dx, lo, hi);
                    minX = Math.min(minX, pivot + xr[0]); maxX = Math.max(maxX, pivot + xr[1]);
                    minZ = Math.min(minZ, DoorGeometry.PIVOT_Z + DoorDepth.BACK.offset() + zr[0]);
                    maxZ = Math.max(maxZ, DoorGeometry.PIVOT_Z + DoorDepth.BACK.offset() + zr[1]);
                }
                out.add(new Box(minX, box.minY(), minZ, maxX, box.maxY(), maxZ));
            }
        }
        return List.copyOf(out);
    }

    /** Exact extrema of a*cos(t)+b*sin(t): endpoints plus every stationary point in the interval. */
    static double[] range(double a, double b, double lo, double hi) {
        double start = a * Math.cos(lo) + b * Math.sin(lo), end = a * Math.cos(hi) + b * Math.sin(hi);
        double min = Math.min(start, end), max = Math.max(start, end);
        double phase = Math.atan2(b, a);
        for (int k = -2; k <= 2; k++) {
            double t = phase + k * Math.PI;
            if (t >= lo && t <= hi) {
                double value = a * Math.cos(t) + b * Math.sin(t);
                min = Math.min(min, value); max = Math.max(max, value);
            }
        }
        return new double[]{min - 1e-10, max + 1e-10};
    }
}
