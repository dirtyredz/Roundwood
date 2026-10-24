package io.github.dirtyredz.roundwood.geometry;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.util.StringRepresentable;

/**
 * The round door's ring profile (shared since 0.8.0): a flat ring in the 32 x 32 plane of a 2 x 2
 * assembly, outer diameter {@link #OUTER}, inner {@link #INNER} (the two-pixel border around the
 * door's disc). The freestanding frame ({@code block/trim}) and the door's trim attachment
 * ({@code block/door/trim}) both extrude it; neither family imports the other for it. Plain Java.
 */
public final class RingGeometry {
    public static final int OUTER = 32;
    public static final int INNER = 28;
    public static final int WIDTH = (OUTER - INNER) / 2;

    private static final List<PixelCircle.Rect> RECTS = PixelCircle.ring(OUTER, WIDTH);

    private RingGeometry() {
    }

    /** True when pixel (u, v) of the 32 x 32 assembly plane is part of the ring. */
    public static boolean inRing(int u, int v) {
        return PixelCircle.inDisc(OUTER, OUTER, u, v) && !PixelCircle.inDisc(OUTER, INNER, u, v);
    }

    /** True when pixel (u, v) is inside the ring's outer edge (the ring or the disc it frames). */
    public static boolean inOuterDisc(int u, int v) {
        return PixelCircle.inDisc(OUTER, OUTER, u, v);
    }

    /** The ring's pixels merged into rectangles (x across, y up, 0..32). */
    public static List<PixelCircle.Rect> rects() {
        return RECTS;
    }

    /** The ring extruded over {@code [z1, z2]}, assembly coordinates. */
    public static List<Box> extrude(double z1, double z2) {
        return RECTS.stream().map(rect -> rect.extrude(z1, z2)).toList();
    }

    /** Source-ring depth and texture slots, shared by frame and attached door-ring models. */
    public static final double BACK = 0;
    public static final double FRONT = 1;
    public static final String FACE = "face";
    public static final String EDGE = "edge";
    private static final List<Box> RING = extrude(BACK, FRONT);

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

    /** Cell identity for a two-by-two ring; serialized names and order are historical. */
    public enum Quarter implements StringRepresentable {
        LOWER_LEFT(0, 0),
        UPPER_LEFT(0, 1),
        LOWER_RIGHT(1, 0),
        UPPER_RIGHT(1, 1);

        public final int column;
        public final int row;

        Quarter(int column, int row) {
            this.column = column;
            this.row = row;
        }

        public static Quarter of(int column, int row) {
            for (Quarter part : values()) {
                if (part.column == column && part.row == row) {
                    return part;
                }
            }
            throw new IllegalArgumentException("no frame part at " + column + "," + row);
        }

        public boolean lower() {
            return row == 0;
        }

        /** The same quarter in the other column (a mirrored frame). */
        public Quarter mirrored() {
            return of(1 - column, row);
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

}
