package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft-facing window data from {@link WindowPaneGeometry}, cached per design, pose, connection
 * set and attached sides (a few thousand variants at most): the collision shape (the whole slab plus
 * the active junction), the outline and target shape (that plus the in-cell part of any perimeter
 * trim and, since 0.10.0, any arch sill) and what each cell boundary shows, for face culling.
 * Attachments are drawn by a renderer and never take part in culling. This class composes pane and
 * attachment volumes ({@link #selectionBoxes}); the pane geometry itself knows nothing of trims.
 */
public final class WindowShapes {
    private record Key(WindowDesign design, WindowPose pose, int connections, int trimSides, int sillSides) {
    }

    private static final ConcurrentHashMap<Key, VoxelShape> COLLISION = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Key, VoxelShape> SELECTION = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Key, BitSet[]> BOUNDARIES = new ConcurrentHashMap<>();

    private WindowShapes() {
    }

    /** What collides: never an attachment. */
    public static VoxelShape collision(WindowDesign design, WindowPose pose, Collection<Direction> connected) {
        return COLLISION.computeIfAbsent(key(design, pose, connected, Set.of(), Set.of()),
                k -> VoxelShapeFactory.shape(WindowPaneGeometry.boxes(design, pose, connected)));
    }

    /**
     * What is outlined and targeted: the collision shape plus the in-cell perimeter trim on
     * {@code trimSides} and the in-cell sill on {@code sillSides} (world sides along the normal).
     */
    public static VoxelShape selection(WindowDesign design, WindowPose pose, Collection<Direction> connected,
                                       Collection<Direction> trimSides, Collection<Direction> sillSides) {
        return SELECTION.computeIfAbsent(key(design, pose, connected, trimSides, sillSides),
                k -> VoxelShapeFactory.shape(selectionBoxes(design, pose, connected, trimSides, sillSides)));
    }

    /**
     * Outline and target boxes, plain Java: the collision boxes plus each attachment where it lies
     * inside the cell. A band beyond the cell is targeted through the slab face under it.
     */
    public static List<Box> selectionBoxes(WindowDesign design, WindowPose pose, Collection<Direction> connected,
                                           Collection<Direction> trimSides, Collection<Direction> sillSides) {
        List<Box> boxes = new ArrayList<>(WindowPaneGeometry.boxes(design, pose, connected));
        boxes.addAll(WindowTrimGeometry.targetBoxes(TrimSlot.PERIMETER, design.profile(), pose, trimSides));
        boxes.addAll(WindowTrimGeometry.targetBoxes(TrimSlot.SILL, design.profile(), pose, sillSides));
        return List.copyOf(boxes);
    }

    /**
     * True when the neighbour's drawn geometry on the shared boundary covers everything this window
     * draws on its {@code side} boundary, so this window's faces there may be skipped.
     */
    public static boolean hides(WindowDesign neighbourDesign, WindowPose neighbourPose, Collection<Direction> neighbourConnected,
                                WindowDesign design, WindowPose pose, Collection<Direction> connected, Direction side) {
        BitSet mine = boundary(design, pose, connected, side);
        if (mine.isEmpty()) {
            return false;
        }
        BitSet uncovered = (BitSet) mine.clone();
        uncovered.andNot(boundary(neighbourDesign, neighbourPose, neighbourConnected, side.getOpposite()));
        return uncovered.isEmpty();
    }

    /** What the window draws on its {@code side} cell boundary ({@link WindowPaneGeometry#boundary}). */
    public static BitSet boundary(WindowDesign design, WindowPose pose, Collection<Direction> connected, Direction side) {
        return BOUNDARIES.computeIfAbsent(key(design, pose, connected, Set.of(), Set.of()), k -> {
            var boxes = WindowPaneGeometry.pieces(design, pose, connected).stream().map(Piece::box).toList();
            BitSet[] sides = new BitSet[6];
            for (Direction direction : Direction.values()) {
                sides[direction.ordinal()] = WindowPaneGeometry.boundary(boxes, direction);
            }
            return sides;
        })[side.ordinal()];
    }

    private static Key key(WindowDesign design, WindowPose pose, Collection<Direction> connected,
                           Collection<Direction> trimSides, Collection<Direction> sillSides) {
        return new Key(design, pose, mask(connected), mask(trimSides), mask(sillSides));
    }

    private static int mask(Collection<Direction> sides) {
        int mask = 0;
        for (Direction side : sides) {
            mask |= 1 << side.ordinal();
        }
        return mask;
    }
}
