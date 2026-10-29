package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchMasonryJoints;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;
import io.github.dirtyredz.roundwood.client.arch.PolygonPartition.Patch;
import io.github.dirtyredz.roundwood.client.arch.PolygonPartition.Point;

/** Bake-time partition of donor faces into disjoint donor/mortar polygons, never overlay faces. */
public final class ArchMasonryModels {
    static final int MORTAR = 0xFF999999;
    private ArchMasonryModels() {}

    /** Each curved end is four chords across a quarter pixel (radial error below 0.0001 pixel). */
    static List<List<Point>> joints(ArchPieceDesign design) {
        List<List<Point>> joints = new ArrayList<>();
        double half = ArchMasonryJoints.WIDTH / 2;
        if (design.shoulder()) {
            for (double y : ArchMasonryJoints.SHOULDER_LEVELS) joints.add(rectangle(0, y - half, 16, y + half));
        } else {
            double radius = design.top().radius(), centre = design.width() / 2.0, spring = design.top().spring();
            for (double angle : ArchMasonryJoints.radialAngles(design)) {
                for (int slice = 0; slice < 4; slice++) {
                    double a = -half + slice * ArchMasonryJoints.WIDTH / 4;
                    double b = -half + (slice + 1) * ArchMasonryJoints.WIDTH / 4;
                    joints.add(List.of(radial(centre, spring, radius, angle, a), radial(centre, spring, radius + 2, angle, a),
                            radial(centre, spring, radius + 2, angle, b), radial(centre, spring, radius, angle, b)));
                }
            }
        }
        return List.copyOf(joints);
    }

    private static Point radial(double centre, double spring, double radius, double angle, double across) {
        double along = Math.sqrt(radius * radius - across * across);
        return new Point(centre + along * Math.cos(angle) - across * Math.sin(angle),
                spring + along * Math.sin(angle) + across * Math.cos(angle));
    }

    private static List<Point> rectangle(double x0, double y0, double x1, double y1) {
        return List.of(new Point(x0, y0), new Point(x1, y0), new Point(x1, y1), new Point(x0, y1));
    }

    /** Triangles are degenerate quads; source winding, normal, donor UV projection and AO survive. */
    static List<BakedQuad> bake(Surface surface, BakedQuad source, int originX, int originY, int turns,
                               List<List<Point>> joints) {
        List<BakedQuad> quads = new ArrayList<>();
        double z = surface.face() == Direction.SOUTH ? surface.backing().maxZ() : surface.backing().minZ();
        var box = surface.backing();
        for (Patch patch : PolygonPartition.partition(box.minX() + originX, box.minY() + originY,
                box.maxX() + originX, box.maxY() + originY, joints)) {
            var polygon = patch.polygon();
            for (int i = 1; i < polygon.size() - 1; i++) {
                Point a = polygon.get(0), b = polygon.get(i), c = polygon.get(i + 1);
                if (PolygonPartition.area(List.of(a, b, c)) <= 1e-9) continue;
                // CCW in authored XY faces south; north faces require the opposite winding.
                Point[] triangle = surface.face() == Direction.SOUTH ? new Point[]{a, b, c, c} : new Point[]{a, c, b, b};
                int[] data = source.getVertices().clone();
                int stride = data.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    double x = triangle[vertex].x() - originX, y = triangle[vertex].y() - originY, depth = z;
                    for (int turn = 0; turn < turns; turn++) { double nx = 16 - depth; depth = x; x = nx; }
                    int offset = vertex * stride;
                    data[offset] = Float.floatToRawIntBits((float) (x / 16));
                    data[offset + 1] = Float.floatToRawIntBits((float) (y / 16));
                    data[offset + 2] = Float.floatToRawIntBits((float) (depth / 16));
                    if (patch.masked()) data[offset + 3] = MORTAR;
                    double[] uv = projected(source.getDirection(), x / 16, y / 16, depth / 16);
                    for (int axis = 0; axis < 2; axis++) data[offset + 4 + axis] = Float.floatToRawIntBits(interpolate(source, axis, uv[axis]));
                }
                quads.add(new BakedQuad(data, source.getTintIndex(), source.getDirection(), source.getSprite(), source.isShade(), source.hasAmbientOcclusion()));
            }
        }
        return quads;
    }

    private static double[] projected(Direction face, double x, double y, double z) {
        return switch (face) {
            case NORTH -> new double[]{-x, -y};
            case SOUTH -> new double[]{x, -y};
            case WEST -> new double[]{z, -y};
            case EAST -> new double[]{-z, -y};
            default -> throw new IllegalArgumentException("Mortar only partitions front/back faces");
        };
    }

    private static float interpolate(BakedQuad source, int axis, double value) {
        int[] data = source.getVertices(); int stride = data.length / 4;
        double start = projected(source.getDirection(), Float.intBitsToFloat(data[0]), Float.intBitsToFloat(data[1]), Float.intBitsToFloat(data[2]))[axis];
        float uv = Float.intBitsToFloat(data[4 + axis]);
        for (int i = stride; i < data.length; i += stride) {
            double end = projected(source.getDirection(), Float.intBitsToFloat(data[i]), Float.intBitsToFloat(data[i + 1]), Float.intBitsToFloat(data[i + 2]))[axis];
            if (Math.abs(end - start) > 1e-9) return (float) (uv + (value - start) / (end - start) * (Float.intBitsToFloat(data[i + 4 + axis]) - uv));
        }
        throw new IllegalArgumentException("Degenerate source face");
    }
}
