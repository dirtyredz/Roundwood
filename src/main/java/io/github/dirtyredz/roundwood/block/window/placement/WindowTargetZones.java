package io.github.dirtyredz.roundwood.block.window.placement;

import net.minecraft.core.Direction;

import java.util.List;
import java.util.Optional;

/**
 * The targeting grid on a clicked face, Hammer style: a centre square and four edge trapezoids.
 * One authority for the classifier (placement) and the polygons (the client overlay), so the
 * highlight always shows the zone the click will use. Plain Java apart from {@link Direction}.
 *
 * <p>Face coordinates {@code (u, v)} are the hit point's position on the clicked cell's face, each
 * 0..1, fixed per face in world terms:
 * <table>
 * <tr><th>Face</th><th>u grows</th><th>v grows</th></tr>
 * <tr><td>up, down</td><td>east</td><td>south</td></tr>
 * <tr><td>north, south</td><td>east</td><td>up</td></tr>
 * <tr><td>east, west</td><td>south</td><td>up</td></tr>
 * </table>
 * The centre is {@code [1/4, 3/4]^2}, boundary included. Outside it the larger distance from the
 * face's middle picks the edge; an exact diagonal tie picks the u edge. An edge zone names the world
 * direction its edge lies in, so the top and bottom zones of a wall face name up and down.
 */
public final class WindowTargetZones {
    public static final double INNER_MIN = 0.25;
    public static final double INNER_MAX = 0.75;

    private WindowTargetZones() {
    }

    /** The five zones; the four edges are named in face coordinates. */
    public enum Zone {
        CENTER, U_MIN, U_MAX, V_MIN, V_MAX
    }

    /** A face coordinate pair. */
    public record FacePoint(double u, double v) {
    }

    /**
     * The face coordinates of a hit {@code (x, y, z)} given relative to the clicked cell's lower
     * corner (block units; not reduced modulo 1, so exact boundary hits stay on their face). Values
     * are clamped to 0..1: real hits are inside up to rounding, and shapes reaching past the cell
     * count as its nearest edge.
     */
    public static FacePoint facePoint(Direction face, double x, double y, double z) {
        return switch (face.getAxis()) {
            case Y -> new FacePoint(clamp(x), clamp(z));
            case Z -> new FacePoint(clamp(x), clamp(y));
            case X -> new FacePoint(clamp(z), clamp(y));
        };
    }

    public static Zone classify(FacePoint point) {
        double du = point.u() - 0.5;
        double dv = point.v() - 0.5;
        if (Math.abs(du) <= INNER_MAX - 0.5 && Math.abs(dv) <= INNER_MAX - 0.5) {
            return Zone.CENTER;
        }
        if (Math.abs(du) >= Math.abs(dv)) {
            return du < 0 ? Zone.U_MIN : Zone.U_MAX;
        }
        return dv < 0 ? Zone.V_MIN : Zone.V_MAX;
    }

    /** The world direction an edge zone of {@code face} points to; empty for the centre. */
    public static Optional<Direction> edge(Direction face, Zone zone) {
        if (zone == Zone.CENTER) {
            return Optional.empty();
        }
        Direction u = switch (face.getAxis()) {
            case X -> Direction.SOUTH;
            default -> Direction.EAST;
        };
        Direction v = switch (face.getAxis()) {
            case Y -> Direction.SOUTH;
            default -> Direction.UP;
        };
        return Optional.of(switch (zone) {
            case U_MIN -> u.getOpposite();
            case U_MAX -> u;
            case V_MIN -> v.getOpposite();
            case V_MAX -> v;
            default -> throw new IllegalStateException();
        });
    }

    /** The zone's outline on the face, counter-clockwise in (u, v): the centre square or an edge trapezoid. */
    public static List<FacePoint> polygon(Zone zone) {
        FacePoint a = new FacePoint(0, 0);
        FacePoint b = new FacePoint(1, 0);
        FacePoint c = new FacePoint(1, 1);
        FacePoint d = new FacePoint(0, 1);
        FacePoint ia = new FacePoint(INNER_MIN, INNER_MIN);
        FacePoint ib = new FacePoint(INNER_MAX, INNER_MIN);
        FacePoint ic = new FacePoint(INNER_MAX, INNER_MAX);
        FacePoint id = new FacePoint(INNER_MIN, INNER_MAX);
        return switch (zone) {
            case CENTER -> List.of(ia, ib, ic, id);
            case V_MIN -> List.of(a, b, ib, ia);
            case U_MAX -> List.of(b, c, ic, ib);
            case V_MAX -> List.of(c, d, id, ic);
            case U_MIN -> List.of(d, a, ia, id);
        };
    }

    /** The point on the cell's {@code face} at face coordinates (u, v), relative to the cell's lower corner. */
    public static double[] toCell(Direction face, double u, double v) {
        double plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
        return switch (face.getAxis()) {
            case Y -> new double[] {u, plane, v};
            case Z -> new double[] {u, v, plane};
            case X -> new double[] {plane, v, u};
        };
    }

    private static double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 1);
    }
}
