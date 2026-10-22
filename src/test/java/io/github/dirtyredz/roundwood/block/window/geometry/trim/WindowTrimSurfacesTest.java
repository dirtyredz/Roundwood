package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometryTest;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowSurfaceGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimSurfaces;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The projecting trim's visible surfaces (0.8.0), checked against an independently written band mask
 * ({@link WindowTrimGeometryTest#expectedBand}): the outward face covers exactly the band at the
 * band's outer depth, the reveals are exactly the band's perimeter, nothing faces the pane, no face
 * lies between two band pixels, UVs stay inside the sprite even past the cell, and pane plus trims never
 * put two surfaces on one spot facing the same way.
 */
class WindowTrimSurfacesTest {
    private static final double EPS = 1e-9;

    @Test
    void theOutwardFaceCoversExactlyTheBandAtItsOuterDepth() {
        for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
            for (Direction side : WindowTrimGeometryTest.sides(pose)) {
                double outer = side.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? pose.depth().slabMax() + 1 : pose.depth().slabMin() - 1;
                for (WindowProfile profile : WindowProfile.values()) {
                    double area = 0;
                    for (Surface surface : WindowTrimSurfaces.surfaces(profile, pose, side)) {
                        if (surface.face() == side.getOpposite()) {
                            fail(profile + " " + pose + " " + side + ": a mounting face is drawn " + surface);
                        }
                        if (surface.face() != side) {
                            continue;
                        }
                        double[] n = WindowTrimGeometryTest.range(surface.backing(), pose.normal());
                        double plane = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? n[1] : n[0];
                        if (Math.abs(plane - outer) > EPS) {
                            fail(profile + " " + pose + " " + side + ": outward face at " + plane + ", expected " + outer);
                        }
                        area += faceArea(surface);
                    }
                    assertEquals(bandPixels(profile), area, EPS, profile + " " + pose + " " + side);
                }
            }
        }
    }

    @Test
    void theRevealsAreExactlyThePerimeterAndNeverInternal() {
        for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
            for (Direction side : WindowTrimGeometryTest.sides(pose)) {
                for (WindowProfile profile : WindowProfile.values()) {
                    double area = 0;
                    for (Surface surface : WindowTrimSurfaces.surfaces(profile, pose, side)) {
                        if (surface.face().getAxis() == pose.normal()) {
                            continue;
                        }
                        assertEquals(WindowTrimGeometry.EDGE, surface.texture());
                        area += faceArea(surface);
                        for (double[] uv : faceCells(pose, surface)) {
                            int u = (int) Math.floor(uv[0]);
                            int v = (int) Math.floor(uv[1]);
                            if (!WindowTrimGeometryTest.expectedBand(profile, u, v)) {
                                fail(profile + " " + pose + ": reveal on a non-band pixel " + u + "," + v);
                            }
                            int[] step = step(pose, surface.face());
                            int nu = u + step[0];
                            int nv = v + step[1];
                            if (nu >= 0 && nu < 16 && nv >= 0 && nv < 16 && WindowTrimGeometryTest.expectedBand(profile, nu, nv)) {
                                fail(profile + " " + pose + ": internal reveal between " + u + "," + v + " and " + nu + "," + nv);
                            }
                        }
                    }
                    assertEquals(perimeter(profile), area, EPS, profile + " " + pose + " " + side + " perimeter");
                }
            }
        }
    }

    /** Wood tiles like a block: every face's tiled UVs lie in the sprite, the projecting faces included. */
    @Test
    void tiledUvsStayInsideTheSprite() {
        for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
            for (Direction side : WindowTrimGeometryTest.sides(pose)) {
                for (WindowProfile profile : WindowProfile.values()) {
                    for (Surface surface : WindowTrimSurfaces.surfaces(profile, pose, side)) {
                        float[] uv = FaceUv.tiled(surface.backing(), surface.face());
                        for (float value : uv) {
                            if (value < -EPS || value > 16 + EPS) {
                                fail(profile + " " + pose + " " + side + ": uv " + value + " for " + surface);
                            }
                        }
                    }
                }
            }
        }
    }

    /** The pane's own surfaces and both faces' trims together: no two surfaces on one spot facing the same way. */
    @Test
    void paneAndTrimsNeverOverlay() {
        for (WindowDesign design : WindowDesign.all()) {
            for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
                List<Surface> all = new ArrayList<>(WindowSurfaceGeometry.surfaces(WindowPaneGeometry.pieces(design, pose, Set.of()),
                        Set.of(WindowPaneGeometry.GLASS)));
                for (Direction side : WindowTrimGeometryTest.sides(pose)) {
                    all.addAll(WindowTrimSurfaces.surfaces(design.profile(), pose, side));
                }
                Set<String> spots = new HashSet<>();
                for (Surface surface : all) {
                    for (String spot : spots(surface)) {
                        if (!spots.add(spot)) {
                            fail(design.id() + " " + pose + ": two surfaces on " + spot);
                        }
                    }
                }
            }
        }
    }

    @Test
    void anOakFlushBandProjectsBeyondTheCellOnTheWallSide() {
        WindowPose flush = WindowPose.upright(Direction.SOUTH, WindowDepth.NEGATIVE);
        double min = Double.MAX_VALUE;
        for (Surface surface : WindowTrimSurfaces.surfaces(WindowProfile.ROUND, flush, Direction.NORTH)) {
            min = Math.min(min, surface.backing().minZ());
        }
        assertEquals(-1, min, EPS, "the wall-side band reaches z -1");
        assertTrue(WindowTrimSurfaces.surfaces(WindowProfile.ROUND, flush, Direction.SOUTH).stream()
                .allMatch(s -> s.backing().minZ() >= 2 - EPS), "the room-side band sits on the room face");
    }

    private static double bandPixels(WindowProfile profile) {
        int count = 0;
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                count += WindowTrimGeometryTest.expectedBand(profile, u, v) ? 1 : 0;
            }
        }
        return count;
    }

    /** Exposed band pixel edges (to non-band or off the face), one pixel deep each. */
    private static double perimeter(WindowProfile profile) {
        int count = 0;
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                if (!WindowTrimGeometryTest.expectedBand(profile, u, v)) {
                    continue;
                }
                for (int[] s : steps) {
                    int nu = u + s[0];
                    int nv = v + s[1];
                    if (nu < 0 || nu >= 16 || nv < 0 || nv >= 16 || !WindowTrimGeometryTest.expectedBand(profile, nu, nv)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static double faceArea(Surface surface) {
        Box b = surface.backing();
        return switch (surface.face().getAxis()) {
            case X -> (b.maxY() - b.minY()) * (b.maxZ() - b.minZ());
            case Y -> (b.maxX() - b.minX()) * (b.maxZ() - b.minZ());
            case Z -> (b.maxX() - b.minX()) * (b.maxY() - b.minY());
        };
    }

    /** The face pixel (u, v) centres a reveal runs along (one per pixel of its length). */
    private static List<double[]> faceCells(WindowPose pose, Surface surface) {
        Box b = surface.backing();
        List<double[]> out = new ArrayList<>();
        double[] n = WindowTrimGeometryTest.range(b, pose.normal());
        double depth = (n[0] + n[1]) / 2;
        for (double x = b.minX() + 0.5; x < b.maxX(); x += 1) {
            for (double y = b.minY() + 0.5; y < b.maxY(); y += 1) {
                for (double z = b.minZ() + 0.5; z < b.maxZ(); z += 1) {
                    double[] p = {x, y, z};
                    p[pose.normal().ordinal()] = depth;
                    out.add(pose.facePoint(p[0], p[1], p[2]));
                }
            }
        }
        return out;
    }

    /** The face-pixel step towards a world direction in the pane's plane. */
    private static int[] step(WindowPose pose, Direction world) {
        if (world == pose.right()) {
            return new int[] {1, 0};
        }
        if (world == pose.right().getOpposite()) {
            return new int[] {-1, 0};
        }
        return world == pose.top() ? new int[] {0, 1} : new int[] {0, -1};
    }

    /** Quarter-pixel sample points of a surface's face rectangle, keyed with its plane and direction. */
    private static List<String> spots(Surface surface) {
        Box b = surface.backing();
        Direction face = surface.face();
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        List<String> out = new ArrayList<>();
        double[] min = {b.minX(), b.minY(), b.minZ()};
        double[] max = {b.maxX(), b.maxY(), b.maxZ()};
        int n = face.getAxis().ordinal();
        double plane = positive ? max[n] : min[n];
        int a = (n + 1) % 3;
        int c = (n + 2) % 3;
        for (double i = min[a] + 0.25; i < max[a]; i += 0.5) {
            for (double j = min[c] + 0.25; j < max[c]; j += 0.5) {
                out.add(face + "@" + plane + ":" + i + "," + j);
            }
        }
        return out;
    }
}
