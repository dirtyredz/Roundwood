package io.github.dirtyredz.roundwood.block.arch.assembly;

import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import net.minecraft.core.Direction;
import java.util.List;

/** Four aimed-face wedges, shared by shoulder placement and its overlay. */
public final class ArchPieceTargeting {
    private ArchPieceTargeting() {}
    public enum Zone { U_MIN, U_MAX, V_MIN, V_MAX }
    public record Point(double u, double v) {}
    public record Target(Direction facing, Side side, Zone zone) {}

    public static Zone classify(double u, double v) {
        if (!Double.isFinite(u) || !Double.isFinite(v)) throw new IllegalArgumentException("Finite hit required");
        double du = u - .5, dv = v - .5;
        return Math.abs(du) >= Math.abs(dv) ? (du <= 0 ? Zone.U_MIN : Zone.U_MAX)
                : (dv < 0 ? Zone.V_MIN : Zone.V_MAX);
    }

    /** On walls the lower wedge selects the touching edge, the upper wedge the far edge. */
    public static Target target(Direction facing, Direction face, double x, double y, double z) {
        if (!facing.getAxis().isHorizontal()) throw new IllegalArgumentException("Horizontal facing required");
        double u = face.getAxis() == Direction.Axis.X ? z : x;
        double v = face.getAxis() == Direction.Axis.Y ? z : y;
        Zone zone = classify(u, v);
        Direction positiveU = face.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        Direction positiveV = face.getAxis() == Direction.Axis.Y ? Direction.SOUTH : face;
        Direction edge = switch (zone) {
            case U_MIN -> positiveU.getOpposite();
            case U_MAX -> positiveU;
            case V_MIN -> positiveV.getOpposite();
            case V_MAX -> positiveV;
        };
        if (edge == facing.getClockWise()) return new Target(facing, Side.RIGHT, zone);
        if (edge == facing.getCounterClockWise()) return new Target(facing, Side.LEFT, zone);
        return new Target(edge.getClockWise(), Side.LEFT, zone);
    }

    public static List<Point> polygon(Zone zone) {
        Point a = new Point(0, 0), b = new Point(1, 0), c = new Point(1, 1), d = new Point(0, 1), m = new Point(.5, .5);
        return switch (zone) {
            case U_MIN -> List.of(d, a, m);
            case U_MAX -> List.of(b, c, m);
            case V_MIN -> List.of(a, b, m);
            case V_MAX -> List.of(c, d, m);
        };
    }
}
