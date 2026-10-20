package io.github.dirtyredz.roundwood.window;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The round window, authored for {@code axis=x}: the window plane runs west-east (x by y) and is
 * centred in depth like a glass pane. {@code axis=z} is the same geometry turned a quarter.
 *
 * <p>Connections: an in-plane neighbour (east/west for axis x) fills that side's corners with glass
 * so the pane meets the rim; a perpendicular neighbour (north/south) gets a glass arm from the block
 * edge to the window plane, like a vanilla pane junction.
 */
public final class WindowGeometry {
    public static final int DIAMETER = 16;
    public static final int RIM_WIDTH = 2;
    private static final double RIM_BACK = 7;
    private static final double RIM_FRONT = 9;
    private static final double GLASS_BACK = 7.5;
    private static final double GLASS_FRONT = 8.5;

    public static final String RIM = "rim";
    public static final String GLASS = "glass";

    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private WindowGeometry() {
    }

    /** Rim and glass disc. */
    public static List<Piece> core() {
        List<Piece> pieces = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.ring(DIAMETER, RIM_WIDTH)) {
            pieces.add(new Piece(rect.extrude(RIM_BACK, RIM_FRONT), RIM));
        }
        for (PixelCircle.Rect rect : PixelCircle.disc(DIAMETER - 2 * RIM_WIDTH)) {
            pieces.add(new Piece(rect.offset(RIM_WIDTH, RIM_WIDTH).extrude(GLASS_BACK, GLASS_FRONT), GLASS));
        }
        return pieces;
    }

    /** Glass filling the east corners (outside the disc, x >= 8). Turn it for the other sides. */
    public static List<Piece> sideFill() {
        Box eastHalf = new Box(8, 0, 0, 16, 16, 16);
        List<Piece> pieces = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.outside(DIAMETER)) {
            rect.extrude(GLASS_BACK, GLASS_FRONT).intersect(eastHalf)
                    .ifPresent(box -> pieces.add(new Piece(box, GLASS)));
        }
        return pieces;
    }

    /** A glass arm from the north edge to the window plane. Turn it for the other sides. */
    public static List<Piece> arm() {
        return List.of(new Piece(new Box(7, 0, 0, 9, 16, GLASS_BACK), GLASS));
    }

    /**
     * Collision and outline for a state. {@code axisZ} turns everything a quarter; connections are
     * given in world directions.
     */
    public static VoxelShape shape(boolean axisZ, boolean north, boolean east, boolean south, boolean west) {
        int key = (axisZ ? 16 : 0) | (north ? 8 : 0) | (east ? 4 : 0) | (south ? 2 : 0) | (west ? 1 : 0);
        return SHAPES.computeIfAbsent(key, k -> VoxelShapeFactory.shape(boxes(axisZ, north, east, south, west)));
    }

    static List<Box> boxes(boolean axisZ, boolean north, boolean east, boolean south, boolean west) {
        List<Box> boxes = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.disc(DIAMETER)) {
            boxes.add(rect.extrude(RIM_BACK, RIM_FRONT));
        }
        int base = axisZ ? 1 : 0;
        // In-plane sides get the corner fill, perpendicular sides the arm (see the blockstate provider).
        boolean[] sides = axisZ
                ? new boolean[] {south, north, east, west}   // fill +90 (south), fill +270 (north), arm +90 (east), arm +270 (west)
                : new boolean[] {east, west, north, south};  // fill 0 (east), fill 180 (west), arm 0 (north), arm 180 (south)
        addTurned(boxes, sideFill(), sides[0], base);
        addTurned(boxes, sideFill(), sides[1], base + 2);
        addTurned(boxes, arm(), sides[2], base);
        addTurned(boxes, arm(), sides[3], base + 2);
        return boxes;
    }

    private static void addTurned(List<Box> out, List<Piece> pieces, boolean present, int turns) {
        if (present) {
            pieces.forEach(piece -> out.add(piece.box().rotateY(turns)));
        }
    }
}
