package io.github.dirtyredz.roundwood.block.door.leaf;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The round door's single leaf, authored facing north (the placing player stands south of it,
 * looking north). Plain Java: models, collision and the renderer all read it.
 *
 * <p>The leaf is a 32-pixel disc in the plane u (west to east, 0..32) by v (bottom to top, 0..32),
 * occupying depth z 13..16 of the cells like a vanilla door: one plank panel over the whole disc
 * (no built-in trim ring since 0.4.0; frames are separate blocks), with brass straps on the hinge
 * side and a centred knob on each face standing proud of it. It turns as one rigid piece about a
 * vertical axis through the centre of its hinge-side thickness: x 1.5 (left hinge) or 30.5 (right
 * hinge), z 14.5. Opening is a quarter turn away from the front, so the open leaf lies along the
 * hinge-side wall (x 0..3 or 29..32) and its outer half projects one block past the cells.
 */
public final class DoorGeometry {
    public static final int DIAMETER = 32;
    /** The closed leaf sits in the 3/16 slab nearest the placing player. */
    public static final double BACK = 13;
    public static final double FRONT = 16;
    /** The swing axis runs through the middle of the leaf's thickness. */
    public static final double PIVOT_Z = (BACK + FRONT) / 2;

    /** The knob is a 2 x 2 pixel stud in the middle of each face, standing {@link #KNOB_PROUD} proud of it. */
    public static final double KNOB_SIZE = 2;
    public static final double KNOB_PROUD = 2;
    /** The hinge straps are half a pixel proud of the panel. */
    public static final double STRAP_PROUD = 0.5;
    /**
     * How far, in pixels, the decorative hardware can reach outside the leaf's own slab and the four
     * cells plus the projection, at any angle of the swing (the knob reaches 2): render bounds use it.
     */
    public static final double SWEEP_MARGIN = 3;

    public static final String PANEL = "panel";
    public static final String BRASS = "brass";

    private static final List<Piece> LEFT_LEAF = buildLeaf();
    private static final List<Piece> RIGHT_LEAF = mirror(LEFT_LEAF);
    private static final List<Box> ENVELOPE = PixelCircle.disc(DIAMETER).stream()
            .map(rect -> rect.extrude(BACK, FRONT)).toList();

    private DoorGeometry() {
    }

    /**
     * The whole closed leaf in assembly coordinates (x = u 0..32, y = v 0..32), hardware on the hinge
     * side. The hardware is decorative: it is drawn but neither collides nor selects ({@link #envelope}).
     */
    public static List<Piece> leaf(DoorHingeSide hinge) {
        return hinge == DoorHingeSide.LEFT ? LEFT_LEAF : RIGHT_LEAF;
    }

    /** The closed leaf's solid slab (the full disc, 3/16 thick) in assembly coordinates. */
    public static List<Box> envelope() {
        return ENVELOPE;
    }

    /** x of the swing axis in assembly pixels: the middle of the hinge-side leaf thickness. */
    public static double pivotX(DoorHingeSide hinge) {
        double inset = (FRONT - BACK) / 2;
        return hinge == DoorHingeSide.LEFT ? inset : DIAMETER - inset;
    }

    /**
     * The fully open angle, in degrees about +Y (counter-clockwise seen from above, the convention
     * of {@code Axis.YP.rotationDegrees}). Both hinges swing the free edge away from the front.
     */
    public static float openDegrees(DoorHingeSide hinge) {
        return hinge == DoorHingeSide.LEFT ? 90F : -90F;
    }

    /**
     * Turns the point (x, z) about the hinge's swing axis by {@code degrees} about +Y. Returns {x, z}.
     * This is the transform the renderer applies to the leaf model.
     */
    public static double[] turn(double x, double z, double degrees, DoorHingeSide hinge) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double px = pivotX(hinge);
        double dx = x - px;
        double dz = z - PIVOT_Z;
        return new double[] {px + dx * cos + dz * sin, PIVOT_Z - dx * sin + dz * cos};
    }

    /** A closed-leaf box turned to the fully open position: an exact quarter turn about the swing axis. */
    public static Box swing(Box box, DoorHingeSide hinge) {
        double[] a = turn(box.minX(), box.minZ(), openDegrees(hinge), hinge);
        double[] b = turn(box.maxX(), box.maxZ(), openDegrees(hinge), hinge);
        return new Box(round(Math.min(a[0], b[0])), box.minY(), round(Math.min(a[1], b[1])),
                round(Math.max(a[0], b[0])), box.maxY(), round(Math.max(a[1], b[1])));
    }

    /**
     * Collision boxes for one cell, block-local, facing north: the leaf slab turned as one piece,
     * then clipped to the cell. Whatever the open leaf covers outside the four cells is visual only.
     * The selection outline is the whole leaf instead (see {@code door/selection/DoorOutlineGeometry}).
     */
    public static List<Box> cellEnvelope(DoorHingeSide hinge, int column, int row, boolean open) {
        Box cell = new Box(16 * column, 16 * row, 0, 16 * column + 16, 16 * row + 16, 16);
        List<Box> out = new ArrayList<>();
        for (Box box : ENVELOPE) {
            Box placed = open ? swing(box, hinge) : box;
            placed.intersect(cell).map(b -> b.translate(-16 * column, -16 * row, 0)).ifPresent(out::add);
        }
        return out;
    }

    /** The whole leaf, open or closed, as one list of assembly-coordinate boxes (for tests and bounds). */
    public static List<Box> placedEnvelope(DoorHingeSide hinge, boolean open) {
        return open ? ENVELOPE.stream().map(box -> swing(box, hinge)).toList() : ENVELOPE;
    }

    /** Removes floating-point noise from quarter-turn results (every input is a multiple of 1/2 pixel). */
    private static double round(double value) {
        return Math.round(value * 1024.0) / 1024.0;
    }

    private static List<Piece> buildLeaf() {
        List<Piece> pieces = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.disc(DIAMETER)) {
            pieces.add(new Piece(rect.extrude(BACK, FRONT), PANEL));
        }
        // Left hinge: two straps reaching in from the west edge on both faces, one knob in the middle
        // of each face (hobbit style). All brass stands proud of the panel.
        for (int v : new int[] {8, 23}) {
            pieces.add(new Piece(new Box(4, v, FRONT, 11, v + 1, FRONT + STRAP_PROUD), BRASS));
            pieces.add(new Piece(new Box(4, v, BACK - STRAP_PROUD, 11, v + 1, BACK), BRASS));
        }
        double knob = (DIAMETER - KNOB_SIZE) / 2;
        pieces.add(new Piece(new Box(knob, knob, FRONT, knob + KNOB_SIZE, knob + KNOB_SIZE, FRONT + KNOB_PROUD), BRASS));
        pieces.add(new Piece(new Box(knob, knob, BACK - KNOB_PROUD, knob + KNOB_SIZE, knob + KNOB_SIZE, BACK), BRASS));
        return List.copyOf(pieces);
    }

    /** The same leaf with its hardware on the other side: mirrored across u = 16. */
    private static List<Piece> mirror(List<Piece> pieces) {
        return pieces.stream().map(piece -> {
            Box b = piece.box();
            return new Piece(new Box(DIAMETER - b.maxX(), b.minY(), b.minZ(), DIAMETER - b.minX(), b.maxY(), b.maxZ()),
                    piece.texture());
        }).toList();
    }

    /** True when {@code box} lies in the assembly's own four cells (x 0..32, y 0..32, z 0..16). */
    public static boolean insideCells(Box box) {
        return box.within(new Box(0, 0, 0, DIAMETER, DIAMETER, 16));
    }

    /** The part of the open leaf outside the four cells (the visual-only projection), if any. */
    public static Optional<Box> projection(Box openBox) {
        return openBox.intersect(new Box(-16, 0, -16, 48, DIAMETER, 0));
    }
}
