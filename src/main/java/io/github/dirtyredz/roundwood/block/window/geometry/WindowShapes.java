package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft-facing window data from {@link WindowPaneGeometry}, cached per design, pose, connection
 * set and trim (a few thousand variants at most): the collision shape (the whole slab plus the active
 * junction), the outline and target shape (that plus any trim) and what each cell boundary shows,
 * for face culling. A trim never reaches a cell boundary beyond the slab's own footprint, so the
 * boundaries ignore it.
 */
public final class WindowShapes {
    private record Key(WindowDesign design, WindowPose pose, int connections, boolean trimmed) {
    }

    private static final ConcurrentHashMap<Key, VoxelShape> COLLISION = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Key, VoxelShape> SELECTION = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Key, BitSet[]> BOUNDARIES = new ConcurrentHashMap<>();

    private WindowShapes() {
    }

    /** What collides: never the trim. */
    public static VoxelShape collision(WindowDesign design, WindowPose pose, Collection<Direction> connected) {
        return COLLISION.computeIfAbsent(key(design, pose, connected, false),
                k -> VoxelShapeFactory.shape(WindowPaneGeometry.boxes(design, pose, connected)));
    }

    /** What is outlined and targeted: the collision shape plus the trim when there is one. */
    public static VoxelShape selection(WindowDesign design, WindowPose pose, Collection<Direction> connected, boolean trimmed) {
        return SELECTION.computeIfAbsent(key(design, pose, connected, trimmed),
                k -> VoxelShapeFactory.shape(WindowPaneGeometry.selection(design, pose, connected, trimmed)));
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
        return BOUNDARIES.computeIfAbsent(key(design, pose, connected, false), k -> {
            var boxes = WindowPaneGeometry.pieces(design, pose, connected).stream().map(Piece::box).toList();
            BitSet[] sides = new BitSet[6];
            for (Direction direction : Direction.values()) {
                sides[direction.ordinal()] = WindowPaneGeometry.boundary(boxes, direction);
            }
            return sides;
        })[side.ordinal()];
    }

    private static Key key(WindowDesign design, WindowPose pose, Collection<Direction> connected, boolean trimmed) {
        int mask = 0;
        for (Direction side : connected) {
            mask |= 1 << side.ordinal();
        }
        return new Key(design, pose, mask, trimmed);
    }
}
