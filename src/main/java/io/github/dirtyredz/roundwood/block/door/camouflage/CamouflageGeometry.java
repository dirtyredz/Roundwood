package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * The four static corners a camouflage material fills: everything inside the assembly's 32 x 32
 * square that the round leaf does not cover, at the closed leaf's depth (z 13..16), in the assembly
 * frame {@link DoorGeometry} uses. They never move: not with the hinge, not when the door opens.
 * Plain Java: selection shapes, hit classification and the client renderer all read it.
 */
public final class CamouflageGeometry {
    public static final int SIZE = DoorGeometry.DIAMETER;
    public static final double BACK = DoorGeometry.BACK;
    public static final double FRONT = DoorGeometry.FRONT;
    /** Tolerance for hits that land exactly on a corner's surface. */
    private static final double EDGE = 1e-3;

    /** One side of a corner piece; {@link #NORTH} is the back ({@code -z}), {@link #SOUTH} the front. */
    public enum Side {
        DOWN, UP, NORTH, SOUTH, WEST, EAST
    }

    /**
     * One exposed surface: the {@code side} face of {@code box}. Only that face is meant to be
     * drawn; the box gives its extent and its plane.
     */
    public record Face(Box box, Side side) {
    }

    private static final List<Box> CORNERS = PixelCircle.rasterize(SIZE, SIZE, CamouflageGeometry::inCorner).stream()
            .map(rect -> rect.extrude(BACK, FRONT)).toList();

    private CamouflageGeometry() {
    }

    /** True when pixel (u, v) of the 32 x 32 face is a corner pixel (inside the square, outside the disc). */
    public static boolean inCorner(int u, int v) {
        return u >= 0 && v >= 0 && u < SIZE && v < SIZE && !PixelCircle.inDisc(SIZE, SIZE, u, v);
    }

    /** All four corners as assembly-coordinate boxes. */
    public static List<Box> corners() {
        return CORNERS;
    }

    /** The corner pieces inside one cell (column 0..1, row 0..1), cell-local (0..16), still facing north. */
    public static List<Box> quarter(int column, int row) {
        Box cell = cell(column, row);
        List<Box> out = new ArrayList<>();
        for (Box box : CORNERS) {
            box.intersect(cell).map(b -> b.translate(-16 * column, -16 * row, 0)).ifPresent(out::add);
        }
        return out;
    }

    /**
     * Only the surfaces of one cell's corner that can be seen: front and back of every piece, and
     * each side face only where the neighbouring pixel is not corner (the disc, or outside the
     * square). Cell-local, facing north. Internal faces between merged pieces are never emitted.
     */
    public static List<Face> exposedFaces(int column, int row) {
        int u0 = 16 * column;
        int v0 = 16 * row;
        List<Face> faces = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.rasterize(16, 16, (u, v) -> inCorner(u0 + u, v0 + v))) {
            faces.add(new Face(rect.extrude(BACK, FRONT), Side.NORTH));
            faces.add(new Face(rect.extrude(BACK, FRONT), Side.SOUTH));
        }
        for (int line = 0; line < 16; line++) {
            int l = line;
            // Rows for the top/bottom faces, columns for the side faces; each a strip of exposed pixels.
            strips(faces, Side.UP, (i) -> exposed(u0 + i, v0 + l, 0, 1), (a, b) -> new Box(a, l, BACK, b, l + 1, FRONT));
            strips(faces, Side.DOWN, (i) -> exposed(u0 + i, v0 + l, 0, -1), (a, b) -> new Box(a, l, BACK, b, l + 1, FRONT));
            strips(faces, Side.EAST, (i) -> exposed(u0 + l, v0 + i, 1, 0), (a, b) -> new Box(l, a, BACK, l + 1, b, FRONT));
            strips(faces, Side.WEST, (i) -> exposed(u0 + l, v0 + i, -1, 0), (a, b) -> new Box(l, a, BACK, l + 1, b, FRONT));
        }
        return faces;
    }

    /** A corner pixel whose neighbour in direction (du, dv) is not corner: its face on that side shows. */
    private static boolean exposed(int u, int v, int du, int dv) {
        return inCorner(u, v) && !inCorner(u + du, v + dv);
    }

    private interface StripBox {
        Box of(int from, int to);
    }

    /** Adds one face per maximal run of exposed pixels along a 16-pixel line. */
    private static void strips(List<Face> out, Side side, IntPredicate exposed, StripBox box) {
        int i = 0;
        while (i < 16) {
            if (!exposed.test(i)) {
                i++;
                continue;
            }
            int start = i;
            while (i < 16 && exposed.test(i)) {
                i++;
            }
            out.add(new Face(box.of(start, i), side));
        }
    }

    /**
     * True when an assembly-frame point (pixels) lies in a corner and not in the leaf: the corner
     * region at the closed leaf's depth, minus whatever the leaf in its current pose covers.
     */
    public static boolean isCornerHit(DoorHingeSide hinge, boolean open, double x, double y, double z) {
        if (z < BACK - EDGE || z > FRONT + EDGE || !inCorner((int) Math.floor(x), (int) Math.floor(y))) {
            return false;
        }
        for (Box box : DoorGeometry.placedEnvelope(hinge, open)) {
            if (box.minX() < x && x < box.maxX() && box.minY() < y && y < box.maxY() && box.minZ() < z && z < box.maxZ()) {
                return false;
            }
        }
        return true;
    }

    private static Box cell(int column, int row) {
        return new Box(16 * column, 16 * row, 0, 16 * column + 16, 16 * row + 16, 16);
    }
}
