package io.github.dirtyredz.roundwood.door.selection;

import io.github.dirtyredz.roundwood.door.camouflage.CamouflageGeometry;
import io.github.dirtyredz.roundwood.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.List;

/**
 * The selection outline of a whole assembly, so that every part outlines the complete door instead
 * of its own quarter. Built in the assembly frame (facing north) from the leaf in its resting pose,
 * open or closed, plus the static corners when they are targetable; then expressed relative to the
 * queried cell. Collision is separate and stays clipped to each cell ({@code leaf/DoorShapes}).
 * Plain Java.
 */
public final class DoorOutlineGeometry {
    private DoorOutlineGeometry() {
    }

    /** The whole outline in assembly coordinates: the leaf at rest, and the corners if asked for. */
    public static List<Box> outline(DoorHingeSide hinge, boolean open, boolean corners) {
        List<Box> boxes = new ArrayList<>(DoorGeometry.placedEnvelope(hinge, open));
        if (corners) {
            boxes.addAll(CamouflageGeometry.corners());
        }
        return boxes;
    }

    /**
     * The same outline relative to the cell at ({@code column}, {@code row}), still facing north:
     * block-local coordinates that may reach outside 0..16 into the partner cells (and, open, into
     * the visual-only projection one block past the cells).
     */
    public static List<Box> relativeTo(List<Box> outline, int column, int row) {
        return outline.stream().map(box -> box.translate(-16 * column, -16 * row, 0)).toList();
    }
}
