package io.github.dirtyredz.roundwood.window;

import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;

import java.util.ArrayList;
import java.util.List;

/**
 * The round window, authored for {@code axis=x}: the window plane runs west-east (x by y) and the
 * round opening runs through the block along z. {@code axis=z} is the same geometry turned a quarter.
 *
 * <p>A full 16 x 16 x 16 wooden housing: wood fills every pixel outside the centred
 * {@link #OPENING}-pixel circle, corners included, through the whole depth, so the opening's inner
 * walls are wood too. Glass fills the circle at mid-depth. Plain Java, no Minecraft classes.
 */
public final class WindowGeometry {
    public static final int SIZE = 16;
    /** Diameter of the glazed opening. */
    public static final int OPENING = 12;
    public static final double GLASS_BACK = 7.5;
    public static final double GLASS_FRONT = 8.5;

    public static final String FRAME = "frame";
    public static final String GLASS = "glass";

    private static final List<Piece> PIECES = build();

    private WindowGeometry() {
    }

    /** Housing and glass, block-local, for {@code axis=x}. */
    public static List<Piece> pieces() {
        return PIECES;
    }

    /** True when pixel (u, v) of the window face is inside the glazed opening. */
    public static boolean inOpening(int u, int v) {
        return PixelCircle.inDisc(SIZE, OPENING, u, v);
    }

    private static List<Piece> build() {
        List<Piece> pieces = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.rasterize(SIZE, SIZE, (u, v) -> !inOpening(u, v))) {
            pieces.add(new Piece(rect.extrude(0, SIZE), FRAME));
        }
        int inset = (SIZE - OPENING) / 2;
        for (PixelCircle.Rect rect : PixelCircle.disc(OPENING)) {
            pieces.add(new Piece(rect.offset(inset, inset).extrude(GLASS_BACK, GLASS_FRONT), GLASS));
        }
        return List.copyOf(pieces);
    }
}
