package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowOrientation;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every pose of every face: panels in place, junctions that bridge exactly what they must, no
 * overlaps, nothing outside the cell, and rotation/mirroring that move the real geometry.
 */
public class WindowPaneGeometryTest {
    private static final double EPS = 1e-9;

    /** Every pose any block can be in: the round window's seven positions and all 36 arch poses. */
    public static List<WindowPose> allPoses() {
        List<WindowPose> poses = new ArrayList<>();
        for (WindowPosition position : WindowPosition.values()) {
            poses.add(position.pose(Direction.Axis.X));
            if (position == WindowPosition.CENTER) {
                poses.add(position.pose(Direction.Axis.Z));
            }
        }
        for (WindowOrientation orientation : WindowOrientation.values()) {
            for (WindowDepth depth : WindowDepth.values()) {
                poses.add(orientation.pose(depth));
            }
        }
        return poses;
    }

    static Set<Direction> mask(int bits) {
        Set<Direction> sides = EnumSet.noneOf(Direction.class);
        int i = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if ((bits & (1 << i++)) != 0) {
                sides.add(side);
            }
        }
        return sides;
    }

    @Test
    void slabDepthsAreTheSpecifiedOnes() {
        assertBox(WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER).slab(), 0, 0, 7, 16, 16, 9);
        assertBox(WindowPose.upright(Direction.SOUTH, WindowDepth.NEGATIVE).slab(), 0, 0, 0, 16, 16, 2);
        assertBox(WindowPose.upright(Direction.NORTH, WindowDepth.POSITIVE).slab(), 0, 0, 14, 16, 16, 16);
        assertBox(WindowPose.upright(Direction.EAST, WindowDepth.POSITIVE).slab(), 14, 0, 0, 16, 16, 16);
        assertBox(new WindowPose(Direction.UP, Direction.NORTH, WindowDepth.NEGATIVE).slab(), 0, 0, 0, 16, 2, 16);
        assertBox(new WindowPose(Direction.DOWN, Direction.EAST, WindowDepth.POSITIVE).slab(), 0, 14, 0, 16, 16, 16);
        for (WindowPose pose : allPoses()) {
            for (Piece piece : WindowPaneGeometry.panel(WindowDesign.plain(WindowProfile.ROUND), pose)) {
                double[] n = range(piece.box(), pose.normal());
                double min = pose.depth().slabMin();
                if (piece.texture().equals(WindowPaneGeometry.FRAME)) {
                    assertEquals(min, n[0], EPS, pose + " wood");
                    assertEquals(min + 2, n[1], EPS, pose + " wood");
                } else {
                    assertEquals(min + 0.5, n[0], EPS, pose + " glass");
                    assertEquals(min + 1.5, n[1], EPS, pose + " glass");
                }
            }
        }
    }

    @Test
    void everyPieceStaysInTheCellAndNothingOverlaps() {
        for (WindowDesign profile : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                for (int bits = 0; bits < 16; bits++) {
                    List<Piece> pieces = WindowPaneGeometry.pieces(profile, pose, mask(bits));
                    byte[] count = new byte[32 * 32 * 32];
                    for (Piece piece : pieces) {
                        Box box = piece.box();
                        assertTrue(box.within(Box.cell()), profile + " " + pose + " " + bits + ": outside " + box);
                        for (int x = (int) Math.round(box.minX() * 2); x < (int) Math.round(box.maxX() * 2); x++) {
                            for (int y = (int) Math.round(box.minY() * 2); y < (int) Math.round(box.maxY() * 2); y++) {
                                for (int z = (int) Math.round(box.minZ() * 2); z < (int) Math.round(box.maxZ() * 2); z++) {
                                    if (count[(x * 32 + y) * 32 + z]++ != 0) {
                                        fail(profile + " " + pose + " " + bits + ": " + box + " overlaps another piece");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void anIsolatedPaneIsOnlyItsSlab() {
        for (WindowDesign profile : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                assertTrue(WindowPaneGeometry.junction(profile, pose, Set.of()).isEmpty(), pose.toString());
                assertEquals(List.of(pose.slab()), WindowPaneGeometry.boxes(profile, pose, Set.of()), pose.toString());
                double thickness = range(pose.slab(), pose.normal())[1] - range(pose.slab(), pose.normal())[0];
                assertEquals(2, thickness, EPS);
            }
        }
    }

    /** True for the 0.5.0 pose family: upright, slab in the middle of the cell. */
    static boolean centredUpright(WindowPose pose) {
        return pose.upright() && pose.depth() == WindowDepth.CENTER;
    }

    /**
     * 0.6.1 owner ruling: a positioned pane (flush, floor or ceiling) never grows glass beyond its own
     * slab, whatever it is connected to. Two flush windows meeting in an L showed a glass post, stems
     * and arms filling the corner (0.6.0); they now simply meet frame to frame.
     */
    @Test
    void positionedPanesNeverGrowGlassBeyondTheirSlab() {
        for (WindowDesign profile : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                if (centredUpright(pose)) {
                    continue;
                }
                for (int bits = 0; bits < 16; bits++) {
                    assertEquals(WindowPaneGeometry.panel(profile, pose), WindowPaneGeometry.pieces(profile, pose, mask(bits)),
                            profile + " " + pose + " " + bits + ": glass outside the panel");
                    assertEquals(List.of(pose.slab()), WindowPaneGeometry.boxes(profile, pose, mask(bits)),
                            profile + " " + pose + " " + bits + ": shape outside the slab");
                }
            }
        }
    }

    /** A centred upright pane only ever adds the 0.5.0 arms across its plane: no post, nothing in its own plane. */
    @Test
    void centredPanesGrowOnlyArmsAcrossTheirPlane() {
        for (WindowDesign profile : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                if (!centredUpright(pose)) {
                    continue;
                }
                List<Piece> junction = WindowPaneGeometry.junction(profile, pose, mask(0b1111));
                assertFalse(junction.isEmpty(), pose.toString());
                for (Piece piece : junction) {
                    boolean inAnArm = false;
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        // The arm's strip from that edge to the cell's middle (its reach on to the recessed glass included).
                        Box half = new Box(7, 0, 0, 9, 16, 8).rotateY(switch (side) {
                            case EAST -> 1;
                            case SOUTH -> 2;
                            case WEST -> 3;
                            default -> 0;
                        });
                        inAnArm |= side.getAxis() == pose.normal() && piece.box().within(half);
                    }
                    assertTrue(inAnArm, profile + " " + pose + ": " + piece.box() + " is not inside an across-plane arm");
                }
            }
        }
    }

    @Test
    void onlyCentredPanesNeedArmsAndOnlyAcrossTheirPlane() {
        assertEquals(List.of(Direction.NORTH, Direction.SOUTH), WindowPaneGeometry.neededSides(WindowPosition.CENTER.pose(Direction.Axis.X)));
        assertEquals(List.of(Direction.EAST, Direction.WEST), WindowPaneGeometry.neededSides(WindowPosition.CENTER.pose(Direction.Axis.Z)));
        for (WindowPosition position : WindowPosition.values()) {
            if (position != WindowPosition.CENTER) {
                assertEquals(List.of(), WindowPaneGeometry.neededSides(position.pose(Direction.Axis.X)), position.toString());
            }
        }
    }

    /**
     * Every connected side that needs a bridge is joined to the panel by one continuous run of glass
     * from the vanilla strip at the cell edge, and wherever that glass ends on the panel over the
     * opening it reaches the recessed glass (no half-pixel gap).
     */
    @Test
    void connectedJunctionsReachFromTheVanillaStripToThePanel() {
        for (WindowDesign profile : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                for (Direction side : WindowPaneGeometry.neededSides(pose)) {
                    Set<Direction> connected = EnumSet.of(side);
                    List<Piece> pieces = WindowPaneGeometry.pieces(profile, pose, connected);
                    Voxels all = new Voxels(pieces);
                    Voxels junction = new Voxels(WindowPaneGeometry.junction(profile, pose, connected));
                    // The strip: x or z 7..9 at the boundary, full height (where the slab crosses it the panel
                    // itself meets the neighbour, recessed glass included).
                    Box slab = pose.slab();
                    for (double y = 0.25; y < 16; y += 0.5) {
                        double[] p = stripPoint(side, y);
                        if (!slab.containsStrictly(p[0], p[1], p[2])) {
                            assertTrue(all.at(p[0], p[1], p[2]), profile + " " + pose + " " + side + ": strip open at y " + y);
                        }
                    }
                    double[] start = null;
                    for (double y = 0.25; y < 16 && start == null; y += 0.5) {
                        double[] p = stripPoint(side, y);
                        start = junction.at(p[0], p[1], p[2]) ? p : null;
                    }
                    assertTrue(start != null, profile + " " + pose + " " + side + ": no junction glass at the strip");
                    assertTrue(junction.connectsToPanel(start, new Voxels(WindowPaneGeometry.panel(profile, pose))),
                            profile + " " + pose + " " + side + ": the bridge does not reach the panel");
                    assertNoRecessGap(profile, pose, pieces);
                }
            }
        }
    }

    /** Where junction glass sits on a slab face, every opening pixel under it continues into the glass. */
    private static void assertNoRecessGap(WindowDesign profile, WindowPose pose, List<Piece> pieces) {
        Box slab = pose.slab();
        double[] n = range(slab, pose.normal());
        Voxels all = new Voxels(pieces);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                if (!profile.profile().inOpening(u, v)) {
                    continue;
                }
                for (double[] face : new double[][] {{n[0] - 0.25, n[0] + 0.25}, {n[1] + 0.25, n[1] - 0.25}}) {
                    Box outside = pose.toWorld(u + 0.1, v + 0.1, u + 0.9, v + 0.9, face[0] - 0.01, face[0] + 0.01);
                    Box recess = pose.toWorld(u + 0.1, v + 0.1, u + 0.9, v + 0.9, face[1] - 0.01, face[1] + 0.01);
                    if (all.covers(outside)) {
                        assertTrue(all.covers(recess), profile + " " + pose + ": gap behind the junction at pixel " + u + "," + v);
                    }
                }
            }
        }
    }

    @Test
    void rotationTurnsTheGeometry() {
        for (WindowDesign profile : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                assertEquals(pose, pose.rotateY(4));
                assertEquals(pose, pose.rotateY(1).rotateY(-1));
                Set<Direction> connected = mask(0b0111);
                Voxels turned = new Voxels(WindowPaneGeometry.pieces(profile, pose, connected)).rotated();
                Set<Direction> turnedSides = EnumSet.noneOf(Direction.class);
                connected.forEach(side -> turnedSides.add(side.getClockWise()));
                assertEquals(turned, new Voxels(WindowPaneGeometry.pieces(profile, pose.rotateY(1), turnedSides)),
                        profile + " " + pose);
            }
        }
    }

    @Test
    void mirroringReflectsTheGeometryAndKeepsThePiece() {
        for (WindowDesign profile : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                    assertEquals(pose, pose.mirror(axis).mirror(axis), pose + " twice " + axis);
                    Voxels reflected = new Voxels(WindowPaneGeometry.panel(profile, pose)).mirrored(axis);
                    assertEquals(reflected, new Voxels(WindowPaneGeometry.panel(profile, pose.mirror(axis))),
                            profile + " " + pose + " mirrored on " + axis);
                }
            }
        }
    }

    @Test
    void boundaryFootprintsSeeOnlyFacesOnThatSide() {
        WindowPose centred = WindowPosition.CENTER.pose(Direction.Axis.X);
        List<Box> boxes = WindowPaneGeometry.panel(WindowDesign.plain(WindowProfile.ROUND), centred).stream().map(Piece::box).toList();
        // Top: the wooden slab's full top row, x 0..16 by z 7..9 (32 x 4 half-pixel cells).
        assertEquals(32 * 4, WindowPaneGeometry.boundary(boxes, Direction.UP).cardinality());
        // East edge: the wooden rim, full height.
        assertEquals(32 * 4, WindowPaneGeometry.boundary(boxes, Direction.EAST).cardinality());
        assertEquals(0, WindowPaneGeometry.boundary(boxes, Direction.NORTH).cardinality());
        WindowPose ceiling = WindowPosition.UP.pose(Direction.Axis.X);
        List<Box> top = WindowPaneGeometry.panel(WindowDesign.plain(WindowProfile.ROUND), ceiling).stream().map(Piece::box).toList();
        // The round face's wood and glass fill the whole top except the glass recess.
        assertFalse(WindowPaneGeometry.boundary(top, Direction.UP).isEmpty());
        assertEquals(32 * 32, WindowPaneGeometry.boundary(top, Direction.UP).cardinality()
                + 4 * countOpening(WindowProfile.ROUND));
    }

    private static int countOpening(WindowProfile profile) {
        int count = 0;
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                count += profile.inOpening(u, v) ? 1 : 0;
            }
        }
        return count;
    }

    private static double[] stripPoint(Direction side, double y) {
        return switch (side) {
            case NORTH -> new double[] {8, y, 0.25};
            case SOUTH -> new double[] {8, y, 15.75};
            case WEST -> new double[] {0.25, y, 8};
            default -> new double[] {15.75, y, 8};
        };
    }

    static double[] range(Box box, Direction.Axis axis) {
        return switch (axis) {
            case X -> new double[] {box.minX(), box.maxX()};
            case Y -> new double[] {box.minY(), box.maxY()};
            case Z -> new double[] {box.minZ(), box.maxZ()};
        };
    }

    private static void assertBox(Box box, double x1, double y1, double z1, double x2, double y2, double z2) {
        assertEquals(new Box(x1, y1, z1, x2, y2, z2), box);
    }

    /** Occupancy on the half-pixel grid (32 per axis), glass and wood told apart. */
    static final class Voxels {
        private final byte[] cells = new byte[32 * 32 * 32];

        Voxels(List<Piece> pieces) {
            for (Piece piece : pieces) {
                byte kind = (byte) (piece.texture().equals(WindowPaneGeometry.FRAME) ? 1 : 2);
                Box b = piece.box();
                for (int x = (int) Math.round(b.minX() * 2); x < (int) Math.round(b.maxX() * 2); x++) {
                    for (int y = (int) Math.round(b.minY() * 2); y < (int) Math.round(b.maxY() * 2); y++) {
                        for (int z = (int) Math.round(b.minZ() * 2); z < (int) Math.round(b.maxZ() * 2); z++) {
                            cells[index(x, y, z)] = kind;
                        }
                    }
                }
            }
        }

        private Voxels(byte[] cells) {
            System.arraycopy(cells, 0, this.cells, 0, cells.length);
        }

        private static int index(int x, int y, int z) {
            return (x * 32 + y) * 32 + z;
        }

        boolean at(double x, double y, double z) {
            return cells[index((int) (x * 2), (int) (y * 2), (int) (z * 2))] != 0;
        }

        /** Some half-pixel cell's centre lies inside {@code box}, and every such cell is occupied. */
        boolean covers(Box box) {
            int inside = 0;
            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 32; y++) {
                    for (int z = 0; z < 32; z++) {
                        double cx = x / 2.0 + 0.25;
                        double cy = y / 2.0 + 0.25;
                        double cz = z / 2.0 + 0.25;
                        if (box.containsStrictly(cx, cy, cz)) {
                            if (cells[index(x, y, z)] == 0) {
                                return false;
                            }
                            inside++;
                        }
                    }
                }
            }
            return inside > 0;
        }

        /** Flood fill through occupied cells from {@code start}; true when it touches a panel cell. */
        boolean connectsToPanel(double[] start, Voxels panel) {
            boolean[] seen = new boolean[cells.length];
            java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
            int[] s = {(int) (start[0] * 2), (int) (start[1] * 2), (int) (start[2] * 2)};
            if (cells[index(s[0], s[1], s[2])] == 0) {
                return false;
            }
            queue.add(s);
            seen[index(s[0], s[1], s[2])] = true;
            int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!queue.isEmpty()) {
                int[] c = queue.poll();
                for (int[] d : steps) {
                    int x = c[0] + d[0];
                    int y = c[1] + d[1];
                    int z = c[2] + d[2];
                    if (x < 0 || y < 0 || z < 0 || x >= 32 || y >= 32 || z >= 32) {
                        continue;
                    }
                    int i = index(x, y, z);
                    if (panel.cells[i] != 0) {
                        return true;
                    }
                    if (cells[i] != 0 && !seen[i]) {
                        seen[i] = true;
                        queue.add(new int[] {x, y, z});
                    }
                }
            }
            return false;
        }

        /** Turned a quarter clockwise seen from above, as {@link Box#rotateY}: (x, z) to (16 - z, x). */
        Voxels rotated() {
            byte[] out = new byte[cells.length];
            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 32; y++) {
                    for (int z = 0; z < 32; z++) {
                        out[index(31 - z, y, x)] = cells[index(x, y, z)];
                    }
                }
            }
            return new Voxels(out);
        }

        Voxels mirrored(Direction.Axis axis) {
            byte[] out = new byte[cells.length];
            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 32; y++) {
                    for (int z = 0; z < 32; z++) {
                        int mx = axis == Direction.Axis.X ? 31 - x : x;
                        int mz = axis == Direction.Axis.Z ? 31 - z : z;
                        out[index(mx, y, mz)] = cells[index(x, y, z)];
                    }
                }
            }
            return new Voxels(out);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Voxels v && java.util.Arrays.equals(cells, v.cells);
        }

        @Override
        public int hashCode() {
            return java.util.Arrays.hashCode(cells);
        }

        @Override
        public String toString() {
            return "Voxels";
        }
    }
}
