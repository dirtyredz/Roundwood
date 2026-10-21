package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.BitSet;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft-facing window data from {@link WindowPaneGeometry}, cached per design, pose, connection
 * set and trimmed sides (a few thousand variants at most): the collision shape (the whole slab plus
 * the active junction), the outline and target shape (that plus the in-cell part of any trim) and
 * what each cell boundary shows, for face culling. Trims are attachments drawn by a renderer and
 * never take part in culling.
 */
public final class WindowShapes {
    private record Key(WindowDesign design, WindowPose pose, int connections, int trimSides) {
    }

    private static final ConcurrentHashMap<Key, VoxelShape> COLLISION = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Key, VoxelShape> SELECTION = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Key, BitSet[]> BOUNDARIES = new ConcurrentHashMap<>();

    private WindowShapes() {
    }

    /** What collides: never the trim. */
    public static VoxelShape collision(WindowDesign design, WindowPose pose, Collection<Direction> connected) {
        return COLLISION.computeIfAbsent(key(design, pose, connected, Set.of()),
                k -> VoxelShapeFactory.shape(WindowPaneGeometry.boxes(design, pose, connected)));
    }

    /** What is outlined and targeted: the collision shape plus the in-cell trim on {@code trimSides}. */
    public static VoxelShape selection(WindowDesign design, WindowPose pose, Collection<Direction> connected,
                                       Collection<Direction> trimSides) {
        return SELECTION.computeIfAbsent(key(design, pose, connected, trimSides),
                k -> VoxelShapeFactory.shape(WindowPaneGeometry.selection(design, pose, connected, trimSides)));
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
        return BOUNDARIES.computeIfAbsent(key(design, pose, connected, Set.of()), k -> {
            var boxes = WindowPaneGeometry.pieces(design, pose, connected).stream().map(Piece::box).toList();
            BitSet[] sides = new BitSet[6];
            for (Direction direction : Direction.values()) {
                sides[direction.ordinal()] = WindowPaneGeometry.boundary(boxes, direction);
            }
            return sides;
        })[side.ordinal()];
    }

    private static Key key(WindowDesign design, WindowPose pose, Collection<Direction> connected, Collection<Direction> trimSides) {
        return new Key(design, pose, mask(connected), mask(trimSides));
    }

    private static int mask(Collection<Direction> sides) {
        int mask = 0;
        for (Direction side : sides) {
            mask |= 1 << side.ordinal();
        }
        return mask;
    }
}
