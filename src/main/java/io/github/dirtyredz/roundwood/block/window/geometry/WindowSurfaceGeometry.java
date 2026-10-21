package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The surfaces a window model actually draws, from its completed logical geometry (0.7.0). The
 * pieces are painted, in order, into a half-pixel voxel grid of the cell (a later piece replaces an
 * earlier one where they overlap). A voxel face is
 * drawn only where it can be seen:
 * <ul>
 *     <li>translucent material (glass) never against anything: no glass sheet inside glass, and none
 *     against wood (0.6.1's horizontal seams were the faces between stacked glass boxes);</li>
 *     <li>opaque material (wood, beams) against air or against glass, which leaves the frame's
 *     and the beams' reveals visible through the glazing, never against other opaque material.</li>
 * </ul>
 * Faces with the same material, direction and plane are merged into rectangles; their default UVs
 * come from their position, so the glass texture runs on continuously across the opening, exactly
 * once on each side. Plain Java apart from {@link Direction}.
 */
public final class WindowSurfaceGeometry {
    private static final int N = 32;
    private static final double CELL = 0.5;

    private WindowSurfaceGeometry() {
    }

    /**
     * @param pieces      the completed geometry, every coordinate on the half-pixel grid inside the cell
     * @param translucent the texture variables that are see-through (the glass)
     */
    public static List<Surface> surfaces(List<Piece> pieces, Set<String> translucent) {
        List<String> materials = new ArrayList<>();
        byte[] grid = new byte[N * N * N];
        for (Piece piece : pieces) {
            int index = materials.indexOf(piece.texture());
            if (index < 0) {
                materials.add(piece.texture());
                index = materials.size() - 1;
            }
            paint(grid, piece.box(), (byte) (index + 1));
        }
        boolean[] seeThrough = new boolean[materials.size() + 1];
        for (int i = 0; i < materials.size(); i++) {
            seeThrough[i + 1] = translucent.contains(materials.get(i));
        }
        List<Surface> out = new ArrayList<>();
        for (Direction face : Direction.values()) {
            int n = WindowPose.index(face.getAxis());
            int a = (n + 1) % 3;
            int b = (n + 2) % 3;
            int step = face.getAxisDirection().getStep();
            for (int layer = 0; layer < N; layer++) {
                for (int material = 1; material <= materials.size(); material++) {
                    boolean[] mask = new boolean[N * N];
                    boolean any = false;
                    for (int i = 0; i < N; i++) {
                        for (int j = 0; j < N; j++) {
                            int[] at = new int[3];
                            at[n] = layer;
                            at[a] = i;
                            at[b] = j;
                            if (grid[index(at)] != material) {
                                continue;
                            }
                            at[n] = layer + step;
                            int neighbour = at[n] < 0 || at[n] >= N ? 0 : grid[index(at)];
                            if (visible(seeThrough[material], neighbour, seeThrough)) {
                                mask[i * N + j] = true;
                                any = true;
                            }
                        }
                    }
                    if (any) {
                        merge(mask, face, n, a, b, layer, materials.get(material - 1), out);
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    private static boolean visible(boolean translucent, int neighbour, boolean[] seeThrough) {
        if (neighbour == 0) {
            return true;
        }
        return !translucent && seeThrough[neighbour];
    }

    /** Greedy rectangles over the mask, row-major, each written as the face of a half-voxel-thick backing box. */
    private static void merge(boolean[] mask, Direction face, int n, int a, int b, int layer, String texture, List<Surface> out) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (!mask[i * N + j]) {
                    continue;
                }
                int j2 = j;
                while (j2 < N && mask[i * N + j2]) {
                    j2++;
                }
                int i2 = i + 1;
                while (i2 < N && run(mask, i2, j, j2)) {
                    i2++;
                }
                for (int ii = i; ii < i2; ii++) {
                    for (int jj = j; jj < j2; jj++) {
                        mask[ii * N + jj] = false;
                    }
                }
                double[] min = new double[3];
                double[] max = new double[3];
                min[a] = i * CELL;
                max[a] = i2 * CELL;
                min[b] = j * CELL;
                max[b] = j2 * CELL;
                min[n] = layer * CELL;
                max[n] = (layer + 1) * CELL;
                out.add(new Surface(new Box(min[0], min[1], min[2], max[0], max[1], max[2]), face, texture));
            }
        }
    }

    private static boolean run(boolean[] mask, int i, int j1, int j2) {
        for (int j = j1; j < j2; j++) {
            if (!mask[i * N + j]) {
                return false;
            }
        }
        return true;
    }

    private static void paint(byte[] grid, Box box, byte material) {
        int x1 = cell(box.minX());
        int x2 = cell(box.maxX());
        int y1 = cell(box.minY());
        int y2 = cell(box.maxY());
        int z1 = cell(box.minZ());
        int z2 = cell(box.maxZ());
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                for (int z = z1; z < z2; z++) {
                    grid[(x * N + y) * N + z] = material;
                }
            }
        }
    }

    private static int cell(double coordinate) {
        double scaled = coordinate / CELL;
        long rounded = Math.round(scaled);
        if (Math.abs(scaled - rounded) > 1e-6 || rounded < 0 || rounded > N) {
            throw new IllegalArgumentException("not on the half-pixel grid inside the cell: " + coordinate);
        }
        return (int) rounded;
    }

    private static int index(int[] at) {
        return (at[0] * N + at[1]) * N + at[2];
    }
}
