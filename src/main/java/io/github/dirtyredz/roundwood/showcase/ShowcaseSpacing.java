package io.github.dirtyredz.roundwood.showcase;

/**
 * The showcase's spacing rule (0.13.1, dk-4350cd9e), the one place the sections read it from. Every
 * sample (a piece, or the few blocks of one demo) is separated from the next by {@link #GAP} clear
 * blocks along {@code r}, and from the next row by the same in {@code f}, with its sign beside it in
 * its own row: {@link #LABEL_OFFSET} columns to the builder's left, so a player can walk between
 * samples, read each sign on its own, and never finds a sign in front of a piece or on top of the
 * next sample or sign. A sample {@code width} columns wide takes {@link #pitch} columns, and sections
 * place their samples by adding that to the column of the one before; the GameTest scan of the built
 * showcase ({@code SpacingChecks}) proves the result block by block rather than trusting the sums.
 */
public final class ShowcaseSpacing {
    /** Clear blocks between neighbouring samples, in a row and between rows (a sign stands inside it). */
    public static final int GAP = 4;
    /** Columns (to the builder's right) from a piece's lower-left cell to its label: two to the left. */
    public static final int LABEL_OFFSET = -2;
    /** The first sample's column: its label stands in column 0. */
    public static final int FIRST_R = -LABEL_OFFSET;

    /** Rows ({@code f}) of the layout, nearest the builder first; each leaves {@link #GAP} clear rows after the deepest sample before it. */
    public static final int ROW_PIECES = 1;
    /** The materials wall's blocks; its signs hang on the front face one row nearer. */
    public static final int ROW_WALL = 7;
    public static final int ROW_POSITIONS = 12;
    public static final int ROW_ARCHES = 18;
    public static final int ROW_CORNERS = 24;

    private ShowcaseSpacing() {
    }

    /** Columns a sample {@code width} wide takes from its lower-left cell to the next sample's: its width plus the gap. */
    public static int pitch(int width) {
        return width + GAP;
    }
}
