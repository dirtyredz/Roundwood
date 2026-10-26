package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The leaf's outline while it swings. The blockstate's {@code open} (and so the resting outline,
 * {@link DoorSelectionShapes}) switches the moment the door is used, but the leaf is drawn turning
 * over {@code DoorSwing.FULL_SWING_TICKS}; an outline read from the blockstate jumps to the final
 * pose ahead of the leaf (0.6.0). This is the closed leaf's silhouette (the edges of its outline,
 * {@link DoorGeometry#envelope}, front and back plus the depth edges at every corner; per design
 * since 0.11.0) turned about the hinge by {@link DoorGeometry#swingDegrees}, the same angle and axis
 * the renderer turns the leaf by, so a highlight drawn from it follows the leaf exactly. Assembly
 * coordinates (pixels, facing north), then put relative to a cell and turned for the facing like the
 * selection shapes. Plain Java.
 */
public final class DoorSwingOutline {
    /** A straight segment, in pixels. */
    public record Edge(double x1, double y1, double z1, double x2, double y2, double z2) {
        public Edge translate(double dx, double dy, double dz) {
            return new Edge(x1 + dx, y1 + dy, z1 + dz, x2 + dx, y2 + dy, z2 + dz);
        }

        /** Turned clockwise seen from above by quarter turns about the cell centre, as {@link Box#rotateY}. */
        public Edge rotateY(int quarterTurns) {
            Edge edge = this;
            for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
                edge = new Edge(Box.BLOCK - edge.z1, edge.y1, edge.x1, Box.BLOCK - edge.z2, edge.y2, edge.x2);
            }
            return edge;
        }

        /** Swung about the hinge's axis by {@code degrees} ({@link DoorGeometry#turn}). */
        Edge turn(DoorDesign design, double degrees, DoorHingeSide hinge) {
            double[] a = DoorGeometry.turn(design, x1, z1, degrees, hinge);
            double[] b = DoorGeometry.turn(design, x2, z2, degrees, hinge);
            return new Edge(a[0], y1, a[1], b[0], y2, b[1]);
        }
    }

    private static final Map<DoorDesign, List<Edge>> CLOSED = new EnumMap<>(DoorDesign.class);

    static {
        for (DoorDesign design : DoorDesign.values()) {
            CLOSED.put(design, silhouette(DoorGeometry.envelope(design)));
        }
    }

    private DoorSwingOutline() {
    }

    /** The closed leaf's outline edges, assembly coordinates. */
    public static List<Edge> closed(DoorDesign design) {
        return CLOSED.get(design);
    }

    /** The leaf's outline at swing {@code progress} (0 closed .. 1 open), assembly coordinates. */
    public static List<Edge> at(DoorDesign design, DoorHingeSide hinge, double progress) {
        if (design.doubleLeaf()) {
            List<Edge> edges = new ArrayList<>();
            for (DoorHingeSide side : DoorHingeSide.values()) {
                var half = DoorGeometry.envelope(design).stream().flatMap(b -> b.intersect(DoorGeometry.half(design, side)).stream()).toList();
                for (Edge edge : silhouette(half)) edges.add(edge.turn(design, DoorGeometry.swingDegrees(side, progress), side));
            }
            return List.copyOf(edges);
        }
        double degrees = DoorGeometry.swingDegrees(hinge, progress);
        return closed(design).stream().map(edge -> edge.turn(design, degrees, hinge)).toList();
    }

    /**
     * The outline at {@code progress} relative to the cell at ({@code column}, {@code row}) of the
     * assembly, turned by {@code quarterTurns} for the facing: block-local pixels of that cell, the
     * frame {@link DoorSelectionShapes} builds its shapes in.
     */
    public static List<Edge> forCell(DoorDesign design, DoorHingeSide hinge, double progress, int column, int row,
                                     int quarterTurns) {
        return at(design, hinge, progress).stream()
                .map(edge -> edge.translate(-16 * column, -16 * row, 0).rotateY(quarterTurns)).toList();
    }

    /**
     * The silhouette of a flat leaf made of boxes sharing one depth range: its outline in the x-y
     * plane (on the half-pixel grid, collinear runs merged) on the back and front faces, and one depth
     * edge at every outline corner.
     */
    static List<Edge> silhouette(List<Box> leaf) {
        double back = leaf.get(0).minZ();
        double front = leaf.get(0).maxZ();
        int size = 0;
        for (Box box : leaf) {
            size = Math.max(size, (int) Math.ceil(Math.max(box.maxX(), box.maxY()) * 2));
        }
        boolean[][] inside = new boolean[size + 2][size + 2];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double x = i / 2.0 + 0.25;
                double y = j / 2.0 + 0.25;
                for (Box box : leaf) {
                    if (x > box.minX() && x < box.maxX() && y > box.minY() && y < box.maxY()) {
                        inside[i + 1][j + 1] = true;
                        break;
                    }
                }
            }
        }
        List<double[]> outline = new ArrayList<>();
        // Horizontal runs (along x) at each grid line y, split by which side is inside.
        for (int j = 0; j <= size; j++) {
            for (int dir = 0; dir < 2; dir++) {
                int start = -1;
                for (int i = 0; i <= size; i++) {
                    boolean edge = i < size && inside[i + 1][j + 1 - dir] && !inside[i + 1][j + dir];
                    if (edge && start < 0) {
                        start = i;
                    } else if (!edge && start >= 0) {
                        outline.add(new double[] {start / 2.0, j / 2.0, i / 2.0, j / 2.0});
                        start = -1;
                    }
                }
            }
        }
        // Vertical runs (along y) at each grid line x.
        for (int i = 0; i <= size; i++) {
            for (int dir = 0; dir < 2; dir++) {
                int start = -1;
                for (int j = 0; j <= size; j++) {
                    boolean edge = j < size && inside[i + 1 - dir][j + 1] && !inside[i + dir][j + 1];
                    if (edge && start < 0) {
                        start = j;
                    } else if (!edge && start >= 0) {
                        outline.add(new double[] {i / 2.0, start / 2.0, i / 2.0, j / 2.0});
                        start = -1;
                    }
                }
            }
        }
        List<Edge> edges = new ArrayList<>();
        Set<List<Double>> corners = new LinkedHashSet<>();
        for (double[] s : outline) {
            edges.add(new Edge(s[0], s[1], back, s[2], s[3], back));
            edges.add(new Edge(s[0], s[1], front, s[2], s[3], front));
            corners.add(List.of(s[0], s[1]));
            corners.add(List.of(s[2], s[3]));
        }
        for (List<Double> c : corners) {
            edges.add(new Edge(c.get(0), c.get(1), back, c.get(0), c.get(1), front));
        }
        return List.copyOf(edges);
    }
}
