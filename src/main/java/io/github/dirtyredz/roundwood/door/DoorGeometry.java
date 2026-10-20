package io.github.dirtyredz.roundwood.door;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The round door, authored facing north (the placing player stands south of it, looking north).
 *
 * <p>The assembly is a 32-pixel disc in the plane u (west to east, 0..32) by v (bottom to top,
 * 0..32), occupying depth z 13..16 like a vanilla door. Column 0 is the left leaf (hinge LEFT, on the
 * west edge), column 1 the right leaf (hinge RIGHT, east edge). Each leaf is clipped into its two
 * cells; an open leaf turns 90 degrees about its outer front edge and stays inside its own cells.
 */
public final class DoorGeometry {
    public static final int DIAMETER = 32;
    public static final int TRIM_WIDTH = 2;
    /** Closed leaves sit in the 3/16 slab nearest the placing player. */
    public static final double BACK = 13;
    public static final double FRONT = 16;
    private static final double PANEL_BACK = 13.5;
    private static final double PANEL_FRONT = 15.5;

    public static final String TRIM = "trim";
    public static final String PANEL = "panel";
    public static final String BRASS = "brass";

    private static final List<Piece> ASSEMBLY = buildAssembly();
    private static final List<Box> ENVELOPE = PixelCircle.disc(DIAMETER).stream()
            .map(rect -> rect.extrude(BACK, FRONT)).toList();
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private DoorGeometry() {
    }

    /** Every closed piece in assembly coordinates (x = u 0..32, y = v 0..32). Used for the item model. */
    public static List<Piece> assembly() {
        return ASSEMBLY;
    }

    /** Model pieces for one cell, block-local, facing north. */
    public static List<Piece> pieces(int column, int row, boolean open) {
        List<Piece> out = new ArrayList<>();
        for (Piece piece : ASSEMBLY) {
            cellPart(piece.box(), column, row, open).ifPresent(box -> out.add(new Piece(box, piece.texture())));
        }
        return out;
    }

    /** Collision and outline boxes for one cell, block-local, facing north: the full disc slab. */
    public static List<Box> envelope(int column, int row, boolean open) {
        List<Box> out = new ArrayList<>();
        for (Box box : ENVELOPE) {
            cellPart(box, column, row, open).ifPresent(out::add);
        }
        return out;
    }

    public static int column(DoorHingeSide hinge) {
        return hinge == DoorHingeSide.LEFT ? 0 : 1;
    }

    public static int row(DoubleBlockHalf half) {
        return half == DoubleBlockHalf.LOWER ? 0 : 1;
    }

    public static VoxelShape shape(Direction facing, DoorHingeSide hinge, DoubleBlockHalf half, boolean open) {
        int column = column(hinge);
        int row = row(half);
        int turns = VoxelShapeFactory.quarterTurns(facing);
        int key = (turns << 3) | (column << 2) | (row << 1) | (open ? 1 : 0);
        return SHAPES.computeIfAbsent(key, k -> VoxelShapeFactory.shape(
                VoxelShapeFactory.rotate(envelope(column, row, open), turns)));
    }

    /** Clips an assembly box to one cell, moves it to block-local coordinates and swings it if open. */
    private static Optional<Box> cellPart(Box assemblyBox, int column, int row, boolean open) {
        Box cell = new Box(16 * column, 16 * row, 0, 16 * column + 16, 16 * row + 16, 16);
        return assemblyBox.intersect(cell)
                .map(box -> box.translate(-16 * column, -16 * row, 0))
                .map(box -> open ? swing(box, column) : box);
    }

    /**
     * Turns a closed leaf box 90 degrees about the leaf's outer front edge. Left leaf: hinge at
     * (x 0, z 16), x' = 16 - z, z' = 16 - x. Right leaf: hinge at (x 16, z 16), x' = z, z' = x.
     */
    static Box swing(Box box, int column) {
        if (column == 0) {
            return new Box(16 - box.maxZ(), box.minY(), 16 - box.maxX(), 16 - box.minZ(), box.maxY(), 16 - box.minX());
        }
        return new Box(box.minZ(), box.minY(), box.minX(), box.maxZ(), box.maxY(), box.maxX());
    }

    private static List<Piece> buildAssembly() {
        List<Piece> pieces = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.ring(DIAMETER, TRIM_WIDTH)) {
            pieces.add(new Piece(rect.extrude(BACK, FRONT), TRIM));
        }
        for (PixelCircle.Rect rect : PixelCircle.disc(DIAMETER - 2 * TRIM_WIDTH)) {
            pieces.add(new Piece(rect.offset(TRIM_WIDTH, TRIM_WIDTH).extrude(PANEL_BACK, PANEL_FRONT), PANEL));
        }
        // Brass: a knob each side of the seam on both faces, and two hinge straps on each front face.
        for (int u : new int[] {12, 19}) {
            pieces.add(new Piece(new Box(u, 15, PANEL_FRONT, u + 1, 17, FRONT), BRASS));
            pieces.add(new Piece(new Box(u, 15, BACK, u + 1, 17, PANEL_BACK), BRASS));
        }
        for (int v : new int[] {9, 22}) {
            pieces.add(new Piece(new Box(4, v, PANEL_FRONT, 9, v + 1, FRONT), BRASS));
            pieces.add(new Piece(new Box(23, v, PANEL_FRONT, 28, v + 1, FRONT), BRASS));
        }
        return List.copyOf(pieces);
    }
}
