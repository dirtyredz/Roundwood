package io.github.dirtyredz.roundwood.block.trim.geometry;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.RingGeometry;

import java.util.ArrayList;
import java.util.List;

/**
 * The round frame (the ring profile is {@link RingGeometry}, shared with the door's trim attachment), authored facing north like the door (the placing player stands south, looking
 * north): a flat ring in the 32 x 32 plane of a 2 x 2 assembly, outer diameter {@link #OUTER}, inner
 * {@link #INNER} (the two-pixel border the door's own trim ring had before 0.4.0), one pixel deep at
 * the back of its cells ({@link #BACK}..{@link #FRONT}). Fitted in front of a door (the cells one
 * step towards the viewer), that back face touches the door's front face, so the ring sits one pixel
 * proud of the door and covers the disc's edge; the camouflage corners outside the disc stay visible.
 * The centre is open. Plain Java.
 */
public final class TrimGeometry {
    public static final int OUTER = RingGeometry.OUTER;
    public static final int INNER = RingGeometry.INNER;
    public static final int WIDTH = RingGeometry.WIDTH;
    public static final double BACK = 0;
    public static final double FRONT = 1;

    /** Texture variables: the front and back faces, and the ring's sides. */
    public static final String FACE = "face";
    public static final String EDGE = "edge";

    private static final List<Box> RING = RingGeometry.extrude(BACK, FRONT);

    private TrimGeometry() {
    }

    /** True when pixel (u, v) of the 32 x 32 assembly plane is part of the ring. */
    public static boolean inRing(int u, int v) {
        return RingGeometry.inRing(u, v);
    }

    /** The whole ring in assembly coordinates (x 0..32 across, y 0..32 up). */
    public static List<Box> ring() {
        return RING;
    }

    /** The ring as model pieces painted with {@link #FACE} (the model writer switches the sides to {@link #EDGE}). */
    public static List<Piece> pieces() {
        return RING.stream().map(box -> new Piece(box, FACE)).toList();
    }

    /**
     * The part of the ring in the cell at ({@code column}, {@code row}) of the assembly, in that
     * cell's own coordinates (0..16).
     */
    public static List<Box> quarter(int column, int row) {
        Box cell = cell(column, row);
        List<Box> out = new ArrayList<>();
        for (Box box : RING) {
            box.intersect(cell).map(b -> b.translate(-16 * column, -16 * row, 0)).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    /** The cell at ({@code column}, {@code row}) in assembly coordinates. */
    public static Box cell(int column, int row) {
        return new Box(16 * column, 16 * row, 0, 16 * column + 16, 16 * row + 16, 16);
    }
}
