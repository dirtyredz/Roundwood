package io.github.dirtyredz.roundwood.window;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;

import java.util.ArrayList;
import java.util.List;

/**
 * The round window, authored for {@code axis=x}: a thin pane in the x-y plane at mid depth, like a
 * glass pane. {@code axis=z} is the same geometry turned a quarter.
 *
 * <p>The core is a square 16 x 16 wooden face, two pixels thick (z 7..9), with wood in every pixel
 * outside the centred {@link #OPENING}-pixel circle and glass inside it (z 7.5..8.5). The face's
 * edges already meet in-plane neighbours, so those connections add nothing. A connection across the
 * plane (north or south for {@code axis=x}) adds an ordinary two-pixel glass arm from the block edge
 * to the panel, trimmed where it meets the wood. Plain Java, no Minecraft classes.
 */
public final class WindowGeometry {
    public static final int SIZE = 16;
    /** Diameter of the glazed opening. */
    public static final int OPENING = 12;
    public static final double PANEL_BACK = 7;
    public static final double PANEL_FRONT = 9;
    public static final double GLASS_BACK = 7.5;
    public static final double GLASS_FRONT = 8.5;
    /** The arm is a vanilla pane's width, centred on the panel. */
    public static final double ARM_MIN = 7;
    public static final double ARM_MAX = 9;

    public static final String FRAME = "frame";
    public static final String GLASS = "glass";

    private static final List<Piece> PIECES = buildCore();
    private static final List<Piece> ARM = buildArm();

    private WindowGeometry() {
    }

    /** Wooden face and glass, block-local, for {@code axis=x}: what every window shows, and the item. */
    public static List<Piece> pieces() {
        return PIECES;
    }

    /**
     * The glass arm towards the north edge (turn it a quarter per side clockwise): from z 0 to the
     * wood's back face, reaching on to the glass where the opening crosses it.
     */
    public static List<Piece> arm() {
        return ARM;
    }

    /** True when pixel (u, v) of the window face is inside the glazed opening. */
    public static boolean inOpening(int u, int v) {
        return PixelCircle.inDisc(SIZE, OPENING, u, v);
    }

    /**
     * Collision and outline boxes, block-local, for a state: the panel turned for its axis plus an arm
     * on every connected side across the plane. Connections are in world directions.
     */
    public static List<Box> boxes(boolean axisZ, boolean north, boolean east, boolean south, boolean west) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(0, 0, PANEL_BACK, SIZE, SIZE, PANEL_FRONT).rotateY(axisZ ? 1 : 0));
        // Quarter turns of the north arm per world side: north 0, east 1, south 2, west 3.
        boolean[] arms = axisZ ? new boolean[] {false, east, false, west} : new boolean[] {north, false, south, false};
        for (int turns = 0; turns < 4; turns++) {
            if (arms[turns]) {
                for (Piece piece : ARM) {
                    boxes.add(piece.box().rotateY(turns));
                }
            }
        }
        return boxes;
    }

    private static List<Piece> buildCore() {
        List<Piece> pieces = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.rasterize(SIZE, SIZE, (u, v) -> !inOpening(u, v))) {
            pieces.add(new Piece(rect.extrude(PANEL_BACK, PANEL_FRONT), FRAME));
        }
        int inset = (SIZE - OPENING) / 2;
        for (PixelCircle.Rect rect : PixelCircle.disc(OPENING)) {
            pieces.add(new Piece(rect.offset(inset, inset).extrude(GLASS_BACK, GLASS_FRONT), GLASS));
        }
        return List.copyOf(pieces);
    }

    private static List<Piece> buildArm() {
        List<Piece> pieces = new ArrayList<>();
        pieces.add(new Piece(new Box(ARM_MIN, 0, 0, ARM_MAX, SIZE, PANEL_BACK), GLASS));
        // Where the arm's width crosses the opening, the glass is recessed: reach on to it.
        int u0 = (int) ARM_MIN;
        int width = (int) (ARM_MAX - ARM_MIN);
        for (PixelCircle.Rect rect : PixelCircle.rasterize(width, SIZE, (u, v) -> inOpening(u0 + u, v))) {
            pieces.add(new Piece(rect.offset(u0, 0).extrude(PANEL_BACK, GLASS_BACK), GLASS));
        }
        return List.copyOf(pieces);
    }
}
