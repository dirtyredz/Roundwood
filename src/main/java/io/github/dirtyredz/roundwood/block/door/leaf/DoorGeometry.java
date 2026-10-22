package io.github.dirtyredz.roundwood.block.door.leaf;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A door's single leaf, per {@link DoorDesign}, authored facing north (the placing player stands
 * south of it, looking north). Plain Java: models, collision and the renderer all read it.
 *
 * <p>The leaf fills the design's outline in the plane u (west to east, 0..width) by v (bottom to
 * top, 0..32), occupying depth z 13..16 of the cells like a vanilla door: one plank panel over the
 * whole outline, with brass straps on the hinge side and a knob on each face standing proud of it.
 * It turns as one rigid piece about a vertical axis through the centre of its hinge-side thickness:
 * x 1.5 (left hinge) or width - 1.5 (right hinge), z 14.5. Opening is a quarter turn away from the
 * front, so the open leaf lies along the hinge-side wall (x 0..3 or width-3..width). The round leaf's
 * outer half projects one block past the cells; the arched leaf, 16 wide, stays inside its cell and
 * leaves 13/16 of it clear.
 */
public final class DoorGeometry {
    /** The round design's width and height (its 32-pixel disc). */
    public static final int DIAMETER = 32;
    /** The closed leaf sits in the 3/16 slab nearest the placing player. */
    public static final double BACK = 13;
    public static final double FRONT = 16;
    /** The swing axis runs through the middle of the leaf's thickness. */
    public static final double PIVOT_Z = (BACK + FRONT) / 2;

    /** The knob is a 2 x 2 pixel stud on each face, standing {@link #KNOB_PROUD} proud of it. */
    public static final double KNOB_SIZE = 2;
    public static final double KNOB_PROUD = 2;
    /** The hinge straps are half a pixel proud of the panel. */
    public static final double STRAP_PROUD = 0.5;
    /**
     * How far, in pixels, the decorative hardware can reach outside the leaf's own slab and the cells
     * plus the projection, at any angle of the swing (the knob reaches 2): render bounds use it.
     */
    public static final double SWEEP_MARGIN = 3;

    public static final String PANEL = "panel";
    public static final String BRASS = "brass";

    private record Leaf(List<Piece> left, List<Piece> right, List<Box> envelope) {
    }

    private static final Map<DoorDesign, Leaf> LEAVES = new EnumMap<>(DoorDesign.class);

    static {
        for (DoorDesign design : DoorDesign.values()) {
            List<Piece> left = buildLeaf(design);
            LEAVES.put(design, new Leaf(left, mirror(left, design.width()),
                    design.leafRects().stream().map(rect -> rect.extrude(BACK, FRONT)).toList()));
        }
    }

    private DoorGeometry() {
    }

    /**
     * The whole closed leaf in assembly coordinates (x = u 0..width, y = v 0..32), hardware on the
     * hinge side. The hardware is decorative: it is drawn but neither collides nor selects ({@link #envelope}).
     */
    public static List<Piece> leaf(DoorDesign design, DoorHingeSide hinge) {
        Leaf leaf = LEAVES.get(design);
        return hinge == DoorHingeSide.LEFT ? leaf.left() : leaf.right();
    }

    /** The closed leaf's solid slab (the whole outline, 3/16 thick) in assembly coordinates. */
    public static List<Box> envelope(DoorDesign design) {
        return LEAVES.get(design).envelope();
    }

    /** x of the swing axis in assembly pixels: the middle of the hinge-side leaf thickness. */
    public static double pivotX(DoorDesign design, DoorHingeSide hinge) {
        double inset = (FRONT - BACK) / 2;
        return hinge == DoorHingeSide.LEFT ? inset : design.width() - inset;
    }

    /**
     * The fully open angle, in degrees about +Y (counter-clockwise seen from above, the convention
     * of {@code Axis.YP.rotationDegrees}). Both hinges swing the free edge away from the front.
     */
    public static float openDegrees(DoorHingeSide hinge) {
        return hinge == DoorHingeSide.LEFT ? 90F : -90F;
    }

    /**
     * The leaf's angle at swing {@code progress} (0 closed .. 1 open), degrees about +Y: what the
     * renderer turns the leaf by and what the swinging highlight follows.
     */
    public static double swingDegrees(DoorHingeSide hinge, double progress) {
        return openDegrees(hinge) * progress;
    }

    /**
     * Turns the point (x, z) about the hinge's swing axis by {@code degrees} about +Y. Returns {x, z}.
     * This is the transform the renderer applies to the leaf model.
     */
    public static double[] turn(DoorDesign design, double x, double z, double degrees, DoorHingeSide hinge) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double px = pivotX(design, hinge);
        double dx = x - px;
        double dz = z - PIVOT_Z;
        return new double[] {px + dx * cos + dz * sin, PIVOT_Z - dx * sin + dz * cos};
    }

    /** A closed-leaf box turned to the fully open position: an exact quarter turn about the swing axis. */
    public static Box swing(DoorDesign design, Box box, DoorHingeSide hinge) {
        double[] a = turn(design, box.minX(), box.minZ(), openDegrees(hinge), hinge);
        double[] b = turn(design, box.maxX(), box.maxZ(), openDegrees(hinge), hinge);
        return new Box(round(Math.min(a[0], b[0])), box.minY(), round(Math.min(a[1], b[1])),
                round(Math.max(a[0], b[0])), box.maxY(), round(Math.max(a[1], b[1])));
    }

    /**
     * Collision boxes for one cell, block-local, facing north: the leaf slab turned as one piece,
     * then clipped to the cell. Whatever the open leaf covers outside the cells is visual only.
     * The selection outline is the whole leaf instead (see {@code door/selection/DoorOutlineGeometry}).
     */
    public static List<Box> cellEnvelope(DoorDesign design, DoorHingeSide hinge, int column, int row, boolean open) {
        Box cell = new Box(16 * column, 16 * row, 0, 16 * column + 16, 16 * row + 16, 16);
        List<Box> out = new ArrayList<>();
        for (Box box : envelope(design)) {
            Box placed = open ? swing(design, box, hinge) : box;
            placed.intersect(cell).map(b -> b.translate(-16 * column, -16 * row, 0)).ifPresent(out::add);
        }
        return out;
    }

    /** The whole leaf, open or closed, as one list of assembly-coordinate boxes (for tests and bounds). */
    public static List<Box> placedEnvelope(DoorDesign design, DoorHingeSide hinge, boolean open) {
        return open ? envelope(design).stream().map(box -> swing(design, box, hinge)).toList() : envelope(design);
    }

    /** True when {@code box} lies in the design's own cells (x 0..width, y 0..32, z 0..16). */
    public static boolean insideCells(DoorDesign design, Box box) {
        return box.within(new Box(0, 0, 0, design.width(), DoorDesign.HEIGHT, 16));
    }

    /** The part of the open leaf outside the cells (the visual-only projection), if any. */
    public static Optional<Box> projection(Box openBox) {
        return openBox.intersect(new Box(-16, 0, -16, 48, DoorDesign.HEIGHT, 0));
    }

    /** Removes floating-point noise from quarter-turn results (every input is a multiple of 1/2 pixel). */
    private static double round(double value) {
        return Math.round(value * 1024.0) / 1024.0;
    }

    private static List<Piece> buildLeaf(DoorDesign design) {
        List<Piece> pieces = new ArrayList<>();
        for (PixelCircle.Rect rect : design.leafRects()) {
            pieces.add(new Piece(rect.extrude(BACK, FRONT), PANEL));
        }
        // Left hinge: two straps reaching in from the west edge on both faces, one knob on each face.
        // All brass stands proud of the panel.
        int from = design.strapFrom();
        int to = from + design.strapLength();
        for (int v : design.strapRows()) {
            pieces.add(new Piece(new Box(from, v, FRONT, to, v + 1, FRONT + STRAP_PROUD), BRASS));
            pieces.add(new Piece(new Box(from, v, BACK - STRAP_PROUD, to, v + 1, BACK), BRASS));
        }
        double kx = design.knobX();
        double ky = design.knobY();
        pieces.add(new Piece(new Box(kx, ky, FRONT, kx + KNOB_SIZE, ky + KNOB_SIZE, FRONT + KNOB_PROUD), BRASS));
        pieces.add(new Piece(new Box(kx, ky, BACK - KNOB_PROUD, kx + KNOB_SIZE, ky + KNOB_SIZE, BACK), BRASS));
        return List.copyOf(pieces);
    }

    /** The same leaf with its hardware on the other side: mirrored across the design's centre line. */
    private static List<Piece> mirror(List<Piece> pieces, int width) {
        return pieces.stream().map(piece -> {
            Box b = piece.box();
            return new Piece(new Box(width - b.maxX(), b.minY(), b.minZ(), width - b.minX(), b.maxY(), b.maxZ()),
                    piece.texture());
        }).toList();
    }
}
