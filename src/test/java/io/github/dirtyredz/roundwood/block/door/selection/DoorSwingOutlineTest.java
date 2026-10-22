package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorSwing;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The swinging highlight (0.6.1): 0.6.0 outlined the blockstate's final pose the moment the door was
 * used while the leaf was still turning (owner screenshot: outline already open, leaf mid-swing).
 * The outline now comes from the swing timeline itself and must sit on the leaf at every moment.
 */
class DoorSwingOutlineTest {
    private static final double EPS = 1e-6;
    private static final double FULL = DoorSwing.FULL_SWING_TICKS;

    private static List<double[]> endpoints(List<DoorSwingOutline.Edge> edges) {
        return edges.stream().flatMap(e -> List.of(new double[] {e.x1(), e.y1(), e.z1()}, new double[] {e.x2(), e.y2(), e.z2()}).stream())
                .toList();
    }

    /** On the surface of the union of {@code boxes}: inside the closure of one, with a step outside all of them. */
    private static boolean onSurface(double[] p, List<Box> boxes) {
        boolean inClosure = boxes.stream().anyMatch(b -> p[0] >= b.minX() - EPS && p[0] <= b.maxX() + EPS
                && p[1] >= b.minY() - EPS && p[1] <= b.maxY() + EPS && p[2] >= b.minZ() - EPS && p[2] <= b.maxZ() + EPS);
        if (!inClosure) {
            return false;
        }
        double step = 0.01;
        double[][] offsets = {{step, 0, 0}, {-step, 0, 0}, {0, step, 0}, {0, -step, 0}, {0, 0, step}, {0, 0, -step}};
        for (double[] o : offsets) {
            double x = p[0] + o[0];
            double y = p[1] + o[1];
            double z = p[2] + o[2];
            if (boxes.stream().noneMatch(b -> b.containsStrictly(x, y, z))) {
                return true;
            }
        }
        return false;
    }

    @Test
    void theClosedSilhouetteTracesTheLeafDisc() {
        List<DoorSwingOutline.Edge> closed = DoorSwingOutline.closed(DoorDesign.ROUND);
        assertFalse(closed.isEmpty());
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        for (double[] p : endpoints(closed)) {
            assertTrue(onSurface(p, DoorGeometry.envelope(DoorDesign.ROUND)), "off the leaf: " + p[0] + "," + p[1] + "," + p[2]);
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
        }
        assertEquals(0, minX, EPS);
        assertEquals(DoorGeometry.DIAMETER, maxX, EPS);
        for (DoorSwingOutline.Edge e : closed) {
            boolean face = e.z1() == e.z2() && (e.z1() == DoorGeometry.BACK || e.z1() == DoorGeometry.FRONT);
            boolean depth = e.x1() == e.x2() && e.y1() == e.y2();
            assertTrue(face || depth, "a slanted closed edge " + e);
        }
    }

    @Test
    void atRestTheOutlineSitsOnTheRestingLeaf() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            assertEquals(DoorSwingOutline.closed(DoorDesign.ROUND), DoorSwingOutline.at(DoorDesign.ROUND, hinge, 0));
            for (double[] p : endpoints(DoorSwingOutline.at(DoorDesign.ROUND, hinge, 1))) {
                assertTrue(onSurface(p, DoorGeometry.placedEnvelope(DoorDesign.ROUND, hinge, true)),
                        hinge + ": open outline off the open leaf at " + p[0] + "," + p[1] + "," + p[2]);
            }
        }
    }

    /**
     * The reported defect: mid-swing, the outline must be where the leaf is drawn (the renderer turns
     * the closed leaf by {@link DoorGeometry#swingDegrees} of the swing's progress), not at the final
     * open pose the blockstate already holds.
     */
    @Test
    void midSwingTheOutlineFollowsTheLeafNotTheBlockstate() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            DoorSwing swing = new DoorSwing(false);
            swing.retarget(true, 100); // the blockstate is open from here on
            for (double t = 100; t <= 100 + FULL + 1; t += 0.25) {
                double progress = swing.progress(t);
                List<DoorSwingOutline.Edge> outline = DoorSwingOutline.at(DoorDesign.ROUND, hinge, progress);
                double degrees = DoorGeometry.swingDegrees(hinge, progress);
                // Every vertex is the closed leaf's vertex turned exactly as the renderer turns the model.
                List<double[]> closed = endpoints(DoorSwingOutline.closed(DoorDesign.ROUND));
                List<double[]> turned = endpoints(outline);
                for (int i = 0; i < closed.size(); i++) {
                    double[] expected = DoorGeometry.turn(DoorDesign.ROUND, closed.get(i)[0], closed.get(i)[2], degrees, hinge);
                    assertEquals(expected[0], turned.get(i)[0], EPS);
                    assertEquals(closed.get(i)[1], turned.get(i)[1], EPS);
                    assertEquals(expected[1], turned.get(i)[2], EPS);
                }
                boolean arrived = turned.stream().allMatch(p -> onSurface(p, DoorGeometry.placedEnvelope(DoorDesign.ROUND, hinge, true)));
                assertEquals(!swing.moving(t), arrived, hinge + " at t " + t + " (progress " + progress + ")");
            }
            // Reversed half way: the outline turns back from where the leaf is, never jumping to closed.
            DoorSwing reversed = new DoorSwing(false);
            reversed.retarget(true, 0);
            reversed.retarget(false, FULL / 2);
            double previous = Math.abs(DoorGeometry.swingDegrees(hinge, reversed.progress(FULL / 2)));
            assertTrue(previous > 1 && previous < 89, "reversal started from " + previous);
            for (double t = FULL / 2; t <= FULL * 2; t += 0.25) {
                double now = Math.abs(DoorGeometry.swingDegrees(hinge, reversed.progress(t)));
                assertTrue(now <= previous + EPS, "the outline turned the wrong way at " + t);
                previous = now;
            }
            assertEquals(0, previous, EPS);
        }
    }

    /** Every part's cell-local edges, put back into world space, are the same whole leaf (as the resting outline). */
    @Test
    void everyPartOutlinesTheSameWholeLeaf() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (int turns = 0; turns < 4; turns++) {
                Set<List<Long>> first = null;
                for (int column = 0; column < 2; column++) {
                    for (int row = 0; row < 2; row++) {
                        Set<List<Long>> world = world(DoorSwingOutline.forCell(DoorDesign.ROUND, hinge, 0.37, column, row, turns), turns, column, row);
                        if (first == null) {
                            first = world;
                        } else {
                            assertEquals(first, world, hinge + " turns " + turns + " part " + column + "," + row);
                        }
                    }
                }
            }
        }
        // Edge turning agrees with box turning.
        Box box = new Box(1, 2, 3, 5, 6, 7);
        DoorSwingOutline.Edge diagonal = new DoorSwingOutline.Edge(1, 2, 3, 5, 6, 7);
        for (int turns = 0; turns < 4; turns++) {
            Box b = box.rotateY(turns);
            DoorSwingOutline.Edge e = diagonal.rotateY(turns);
            assertEquals(b.minX(), Math.min(e.x1(), e.x2()), EPS);
            assertEquals(b.maxZ(), Math.max(e.z1(), e.z2()), EPS);
        }
    }

    /** Cell-local edges (already turned) moved to their cell's world offset, rounded, endpoints unordered. */
    private static Set<List<Long>> world(List<DoorSwingOutline.Edge> edges, int turns, int column, int row) {
        double ox = 16 * column;
        double oz = 0;
        for (int i = 0; i < turns; i++) {
            double t = ox;
            ox = -oz;
            oz = t;
        }
        Set<List<Long>> out = new HashSet<>();
        for (DoorSwingOutline.Edge e : edges) {
            List<Long> a = List.of(r(e.x1() + ox), r(e.y1() + 16 * row), r(e.z1() + oz));
            List<Long> b = List.of(r(e.x2() + ox), r(e.y2() + 16 * row), r(e.z2() + oz));
            List<Long> key = new java.util.ArrayList<>();
            if (a.toString().compareTo(b.toString()) <= 0) {
                key.addAll(a);
                key.addAll(b);
            } else {
                key.addAll(b);
                key.addAll(a);
            }
            out.add(key);
        }
        return out;
    }

    private static long r(double v) {
        return Math.round(v * 1024);
    }
}
