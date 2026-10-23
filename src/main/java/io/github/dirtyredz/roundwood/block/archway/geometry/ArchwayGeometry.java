package io.github.dirtyredz.roundwood.block.archway.geometry;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * An archway's frame as boxes (0.12.0): the design's frame pixels ({@link ArchwayDesign#inFrame})
 * merged into rectangles and extruded the full block deep. The target and selection shapes
 * ({@link ArchwayShapes}), the item model and the rendered models all derive from the same mask, so
 * what is drawn and hit agree; what is walked into is the lower jambs of it alone
 * ({@link #collisionCell}, 0.13.1). Assembly coordinates (x 0..32, y 0..32, facing north) or,
 * per cell, that cell's own coordinates. Plain Java.
 */
public final class ArchwayGeometry {
    private static final Map<ArchwayDesign, List<PixelCircle.Rect>> RECTS = new EnumMap<>(ArchwayDesign.class);

    static {
        for (ArchwayDesign design : ArchwayDesign.values()) {
            RECTS.put(design, PixelCircle.rasterize(ArchwayDesign.SIZE, ArchwayDesign.SIZE, design::inFrame));
        }
    }

    private ArchwayGeometry() {
    }

    /** The frame's pixels merged into rectangles (x across, y up, 0..32). */
    public static List<PixelCircle.Rect> rects(ArchwayDesign design) {
        return RECTS.get(design);
    }

    /** The whole frame, full depth, in assembly coordinates. */
    public static List<Box> frame(ArchwayDesign design) {
        return RECTS.get(design).stream().map(rect -> rect.extrude(ArchwayDesign.BACK, ArchwayDesign.FRONT)).toList();
    }

    /**
     * The whole frame as placed facing {@code facing}, in pixels relative to the anchor cell's origin
     * (the frame {@link ArchwayShapes} turns each cell's piece in): what the placement preview outlines.
     */
    public static List<Box> placed(ArchwayDesign design, Direction facing) {
        return VoxelShapeFactory.rotate(frame(design), VoxelShapeFactory.quarterTurns(facing));
    }

    /** The four cells' joint box as placed facing {@code facing}, relative to the anchor cell's origin. */
    public static Box footprint(Direction facing) {
        return new Box(0, 0, ArchwayDesign.BACK, 2 * Box.BLOCK, 2 * Box.BLOCK, ArchwayDesign.FRONT)
                .rotateY(VoxelShapeFactory.quarterTurns(facing));
    }

    /**
     * What collides in the cell at ({@code column}, {@code row}) (0.13.1, dk-73098f88): the straight
     * jambs below the spring line, which is the lower row's frame, and nothing of the crown in the upper
     * row. The 1.875-block crown left no headroom for a 1.95-block mob and little for a player stepping
     * off the middle, so the opening is open to the full two blocks while the drawn frame, the target and
     * the outline stay whole ({@link #cell}). In that cell's own coordinates (0..16).
     */
    public static List<Box> collisionCell(ArchwayDesign design, int column, int row) {
        return row == 0 ? cell(design, column, row) : List.of();
    }

    /** The frame inside the cell at ({@code column}, {@code row}), in that cell's own coordinates (0..16). */
    public static List<Box> cell(ArchwayDesign design, int column, int row) {
        Box cell = new Box(16 * column, 16 * row, 0, 16 * column + 16, 16 * row + 16, 16);
        List<Box> out = new ArrayList<>();
        for (Box box : frame(design)) {
            box.intersect(cell).map(b -> b.translate(-16 * column, -16 * row, 0)).ifPresent(out::add);
        }
        return List.copyOf(out);
    }
}
