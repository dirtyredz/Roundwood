package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * The surfaces one face's trim band draws (0.8.0): its outward face ({@link WindowTrimGeometry#FACE})
 * and the reveals around its perimeter ({@link WindowTrimGeometry#EDGE}), each exactly once. Never the
 * mounting face against the pane (always covered) and never a face between two band pixels. A reveal
 * on the cell's own edge is drawn too: beside a matching piece it is enclosed between two bands and
 * cannot be seen. The band keeps its full proud depth whatever the pane's depth, so a flush window's
 * wall-side band reaches one pixel past the cell. An arch sill ({@link TrimSlot#SILL}, 0.10.0) is drawn
 * the same way from its own pixels. Plain Java apart from {@link Direction}.
 */
public final class WindowTrimSurfaces {
    private static final int SIZE = WindowProfile.SIZE;

    private WindowTrimSurfaces() {
    }

    /** The visible surfaces of the perimeter band on {@code side} (along the normal) of a pane posed {@code pose}. */
    public static List<Surface> surfaces(WindowProfile profile, WindowPose pose, Direction side) {
        return surfaces(TrimSlot.PERIMETER, profile, pose, side);
    }

    /** The visible surfaces of the attachment in {@code slot} on {@code side} of a pane posed {@code pose}. */
    public static List<Surface> surfaces(TrimSlot slot, WindowProfile profile, WindowPose pose, Direction side) {
        double[] depth = WindowTrimGeometry.depth(pose, side);
        boolean[] band = new boolean[SIZE * SIZE];
        for (int u = 0; u < SIZE; u++) {
            for (int v = 0; v < SIZE; v++) {
                band[u * SIZE + v] = WindowTrimGeometry.covers(slot, profile, u, v);
            }
        }
        List<Surface> out = new ArrayList<>();
        WindowTrimGeometry.band(slot, profile).forEach(rect -> out.add(new Surface(
                pose.toWorld(rect.u1(), rect.v1(), rect.u2(), rect.v2(), depth[0], depth[1]), side, WindowTrimGeometry.FACE)));
        Direction right = pose.right();
        Direction top = pose.top();
        reveals(band, 1, 0, right, pose, depth, out);
        reveals(band, -1, 0, right.getOpposite(), pose, depth, out);
        reveals(band, 0, 1, top, pose, depth, out);
        reveals(band, 0, -1, top.getOpposite(), pose, depth, out);
        return List.copyOf(out);
    }

    /**
     * The reveals facing face direction {@code (du, dv)}: every band pixel whose neighbour that way is
     * not band (or off the face), merged into runs along the edge.
     */
    private static void reveals(boolean[] band, int du, int dv, Direction world, WindowPose pose, double[] depth,
                                List<Surface> out) {
        boolean alongV = du != 0; // a reveal facing along u runs along v, and the other way round
        for (int line = 0; line < SIZE; line++) {
            int start = -1;
            for (int step = 0; step <= SIZE; step++) {
                boolean edge = step < SIZE && exposed(band, alongV ? line : step, alongV ? step : line, du, dv);
                if (edge && start < 0) {
                    start = step;
                } else if (!edge && start >= 0) {
                    Box box = alongV
                            ? pose.toWorld(line, start, line + 1, step, depth[0], depth[1])
                            : pose.toWorld(start, line, step, line + 1, depth[0], depth[1]);
                    out.add(new Surface(box, world, WindowTrimGeometry.EDGE));
                    start = -1;
                }
            }
        }
    }

    private static boolean exposed(boolean[] band, int u, int v, int du, int dv) {
        if (!band[u * SIZE + v]) {
            return false;
        }
        int nu = u + du;
        int nv = v + dv;
        return nu < 0 || nu >= SIZE || nv < 0 || nv >= SIZE || !band[nu * SIZE + nv];
    }
}
