package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import io.github.dirtyredz.roundwood.geometry.PixelSlab;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The static corners a camouflage material fills: everything inside the assembly's rectangle that
 * the leaf does not cover, at the closed leaf's depth (z 13..16), in the assembly frame
 * {@link DoorGeometry} uses. The round door has four (the square minus the disc); the arched door
 * (0.11.0) only the two upper exterior corners beside its crown. They never move: not with the
 * hinge, not when the door opens. Plain Java: selection shapes, hit classification and the client
 * renderer all read it.
 */
public final class CamouflageGeometry {
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

    private static final Map<DoorDesign, List<Box>> CORNERS = new EnumMap<>(DoorDesign.class);

    static {
        for (DoorDesign design : DoorDesign.values()) {
            CORNERS.put(design, PixelCircle.rasterize(design.width(), design.height(), (u, v) -> inCorner(design, u, v)).stream()
                    .map(rect -> rect.extrude(BACK, FRONT)).toList());
        }
    }

    private CamouflageGeometry() {
    }

    /** True when pixel (u, v) of the door's rectangle is a corner pixel (inside the rectangle, outside the leaf). */
    public static boolean inCorner(DoorDesign design, int u, int v) {
        return u >= 0 && v >= 0 && u < design.width() && v < design.height() && !design.inLeaf(u, v);
    }

    /** All the design's corners as assembly-coordinate boxes. */
    public static List<Box> corners(DoorDesign design) {
        return corners(design, CamouflageCoverage.ALL);
    }

    public static List<Box> corners(DoorDesign design, CamouflageCoverage coverage) {
        if (coverage == CamouflageCoverage.NONE) return List.of();
        if (coverage == CamouflageCoverage.ALL) return CORNERS.get(design);
        Box lower = new Box(0, 0, BACK, design.width(), 16, FRONT);
        return CORNERS.get(design).stream().flatMap(b -> b.intersect(lower).stream()).toList();
    }

    /** The corner pieces inside one cell (column, row 0..1), cell-local (0..16), still facing north. */
    public static List<Box> quarter(DoorDesign design, int column, int row) {
        return quarter(design, column, row, CamouflageCoverage.ALL);
    }

    public static List<Box> quarter(DoorDesign design, int column, int row, CamouflageCoverage coverage) {
        Box cell = cell(column, row);
        List<Box> out = new ArrayList<>();
        for (Box box : corners(design, coverage)) {
            box.intersect(cell).map(b -> b.translate(-16 * column, -16 * row, 0)).ifPresent(out::add);
        }
        return out;
    }

    /**
     * Only the surfaces of one cell's corner that can be seen: front and back of every piece, and
     * each side face only where the neighbouring pixel is not corner (the leaf, or outside the
     * rectangle). Cell-local, facing north. Internal faces between merged pieces are never emitted.
     */
    public static List<Face> exposedFaces(DoorDesign design, int column, int row) {
        return exposedFaces(design, column, row, CamouflageCoverage.ALL);
    }

    public static List<Face> exposedFaces(DoorDesign design, int column, int row, CamouflageCoverage coverage) {
        List<Face> faces = new ArrayList<>();
        for (Surface surface : PixelSlab.cellFaces((u, v) -> coverage.contains(design, u, v), 16 * column, 16 * row, BACK, FRONT, "")) {
            faces.add(new Face(surface.backing(), side(surface.face())));
        }
        return faces;
    }

    /**
     * True when an assembly-frame point (pixels) lies in a corner and not in the leaf: the corner
     * region at the closed leaf's depth, minus whatever the leaf in its current pose covers.
     */
    public static boolean isCornerHit(DoorDesign design, DoorHingeSide hinge, boolean open, double x, double y, double z) {
        return isCornerHit(design, hinge, open, x, y, z, CamouflageCoverage.ALL);
    }

    public static boolean isCornerHit(DoorDesign design, DoorHingeSide hinge, boolean open, double x, double y, double z, CamouflageCoverage coverage) {
        if (z < BACK - EDGE || z > FRONT + EDGE || !coverage.contains(design, (int) Math.floor(x), (int) Math.floor(y))) {
            return false;
        }
        for (Box box : DoorGeometry.placedEnvelope(design, hinge, open)) {
            if (box.minX() < x && x < box.maxX() && box.minY() < y && y < box.maxY() && box.minZ() < z && z < box.maxZ()) {
                return false;
            }
        }
        return true;
    }

    private static Side side(Direction direction) {
        return switch (direction) {
            case DOWN -> Side.DOWN;
            case UP -> Side.UP;
            case NORTH -> Side.NORTH;
            case SOUTH -> Side.SOUTH;
            case WEST -> Side.WEST;
            case EAST -> Side.EAST;
        };
    }

    private static Box cell(int column, int row) {
        return new Box(16 * column, 16 * row, 0, 16 * column + 16, 16 * row + 16, 16);
    }
}
