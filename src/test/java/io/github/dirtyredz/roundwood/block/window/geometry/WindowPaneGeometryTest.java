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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every pose of every face: panels in place, no glass beyond the pane's own slab whatever it touches
 * (no junctions since 0.13.1), no overlaps, nothing outside the cell, and rotation/mirroring that move
 * the real geometry.
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
                byte[] count = new byte[32 * 32 * 32];
                for (Piece piece : WindowPaneGeometry.panel(profile, pose)) {
                    Box box = piece.box();
                    assertTrue(box.within(Box.cell()), profile + " " + pose + ": outside " + box);
                    for (int x = (int) Math.round(box.minX() * 2); x < (int) Math.round(box.maxX() * 2); x++) {
                        for (int y = (int) Math.round(box.minY() * 2); y < (int) Math.round(box.maxY() * 2); y++) {
                            for (int z = (int) Math.round(box.minZ() * 2); z < (int) Math.round(box.maxZ() * 2); z++) {
                                if (count[(x * 32 + y) * 32 + z]++ != 0) {
                                    fail(profile + " " + pose + ": " + box + " overlaps another piece");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void aPaneIsOnlyItsSlab() {
        for (WindowDesign profile : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                assertEquals(List.of(pose.slab()), WindowPaneGeometry.boxes(pose), pose.toString());
                double thickness = range(pose.slab(), pose.normal())[1] - range(pose.slab(), pose.normal())[0];
                assertEquals(2, thickness, EPS);
            }
        }
    }

    /**
     * The junction guard (0.13.1, dk-a970707d): no pose of any face draws or collides with anything
     * outside its own slab, and the glass inside it only where the profile's glazing is. The 0.5.0
     * arm across a centred pane's plane (and the 0.6.0 posts, stems and fins) must never come back: a
     * window's geometry has no input a neighbour could change.
     */
    @Test
    void noGlassEverLiesOutsideAWindowsOwnSlabAndProfile() {
        for (WindowDesign design : WindowDesign.all()) {
            for (WindowPose pose : allPoses()) {
                Box slab = pose.slab();
                for (Piece piece : WindowPaneGeometry.panel(design, pose)) {
                    assertTrue(piece.box().within(slab), design + " " + pose + ": " + piece.texture() + " outside the slab: " + piece.box());
                    if (piece.texture().equals(WindowPaneGeometry.GLASS)) {
                        double[] u = pose.uRange(piece.box());
                        double[] v = pose.vRange(piece.box());
                        for (int pu = (int) Math.floor(u[0]); pu < Math.ceil(u[1]); pu++) {
                            for (int pv = (int) Math.floor(v[0]); pv < Math.ceil(v[1]); pv++) {
                                assertTrue(design.profile().inOpening(pu, pv), design + " " + pose + ": glass over frame pixel " + pu + "," + pv);
                            }
                        }
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
                Voxels turned = new Voxels(WindowPaneGeometry.panel(profile, pose)).rotated();
                assertEquals(turned, new Voxels(WindowPaneGeometry.panel(profile, pose.rotateY(1))), profile + " " + pose);
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
                byte kind = (byte) (piece.texture().equals(WindowPaneGeometry.GLASS) ? 2 : 1);
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
