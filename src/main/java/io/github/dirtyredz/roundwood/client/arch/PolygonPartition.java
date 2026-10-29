package io.github.dirtyredz.roundwood.client.arch;

import java.util.ArrayList;
import java.util.List;

/** Pure two-dimensional clipping of a rectangle into disjoint polygon patches. */
final class PolygonPartition {
    private static final double EPSILON = 1e-9;

    record Point(double x, double y) {}
    record Patch(List<Point> polygon, boolean masked) {}

    private PolygonPartition() {}

    static List<Patch> partition(double minX, double minY, double maxX, double maxY,
                                 List<List<Point>> masks) {
        List<List<Point>> unmasked = new ArrayList<>();
        unmasked.add(rectangle(minX, minY, maxX, maxY));
        List<Patch> result = new ArrayList<>();
        for (var mask : masks) {
            List<List<Point>> next = new ArrayList<>();
            for (var polygon : unmasked) {
                List<Point> overlap = polygon;
                for (int edge = 0; edge < mask.size() && !overlap.isEmpty(); edge++)
                    overlap = clip(overlap, mask.get(edge), mask.get((edge + 1) % mask.size()), true);
                if (area(overlap) <= EPSILON) { next.add(polygon); continue; }
                List<Point> inside = polygon;
                for (int edge = 0; edge < mask.size() && !inside.isEmpty(); edge++) {
                    Point a = mask.get(edge), b = mask.get((edge + 1) % mask.size());
                    var outside = clip(inside, a, b, false);
                    if (area(outside) > EPSILON) next.add(outside);
                    inside = clip(inside, a, b, true);
                }
                if (area(inside) > EPSILON) result.add(new Patch(inside, true));
            }
            unmasked = next;
        }
        for (var polygon : unmasked) result.add(new Patch(polygon, false));
        return List.copyOf(result);
    }

    private static List<Point> rectangle(double x0, double y0, double x1, double y1) {
        return List.of(new Point(x0, y0), new Point(x1, y0), new Point(x1, y1), new Point(x0, y1));
    }

    private static List<Point> clip(List<Point> polygon, Point a, Point b, boolean inside) {
        if (polygon.isEmpty()) return List.of();
        List<Point> result = new ArrayList<>();
        Point previous = polygon.get(polygon.size() - 1);
        double pd = distance(previous, a, b) * (inside ? 1 : -1);
        for (Point current : polygon) {
            double cd = distance(current, a, b) * (inside ? 1 : -1);
            if ((pd >= 0) != (cd >= 0)) {
                double t = pd / (pd - cd);
                result.add(new Point(previous.x + t * (current.x - previous.x), previous.y + t * (current.y - previous.y)));
            }
            if (cd >= 0) result.add(current);
            previous = current;
            pd = cd;
        }
        return result;
    }

    private static double distance(Point point, Point a, Point b) {
        return (b.x - a.x) * (point.y - a.y) - (b.y - a.y) * (point.x - a.x);
    }

    static double area(List<Point> polygon) {
        double area = 0;
        for (int i = 0; i < polygon.size(); i++) {
            Point a = polygon.get(i), b = polygon.get((i + 1) % polygon.size());
            area += a.x * b.y - a.y * b.x;
        }
        return Math.abs(area) / 2;
    }
}
