package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft-facing window data from {@link WindowPaneGeometry}, cached per face, pose and connection
 * set (a few thousand variants at most): the collision/outline shape (the whole slab plus the active
 * junction) and what each cell boundary shows, for face culling.
 */
public final class WindowShapes {
    private record Key(WindowProfile profile, WindowPose pose, int connections) {
    }

    private static final ConcurrentHashMap<Key, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Key, BitSet[]> BOUNDARIES = new ConcurrentHashMap<>();

    private WindowShapes() {
    }

    public static VoxelShape shape(WindowProfile profile, WindowPose pose, Collection<Direction> connected) {
        return SHAPES.computeIfAbsent(key(profile, pose, connected),
                k -> VoxelShapeFactory.shape(WindowPaneGeometry.boxes(profile, pose, connected)));
    }

    /**
     * True when the neighbour's drawn geometry on the shared boundary covers everything this window
     * draws on its {@code side} boundary, so this window's faces there may be skipped.
     */
    public static boolean hides(WindowProfile neighbourProfile, WindowPose neighbourPose, Collection<Direction> neighbourConnected,
                                WindowProfile profile, WindowPose pose, Collection<Direction> connected, Direction side) {
        BitSet mine = boundary(profile, pose, connected, side);
        if (mine.isEmpty()) {
            return false;
        }
        BitSet uncovered = (BitSet) mine.clone();
        uncovered.andNot(boundary(neighbourProfile, neighbourPose, neighbourConnected, side.getOpposite()));
        return uncovered.isEmpty();
    }

    /** What the window draws on its {@code side} cell boundary ({@link WindowPaneGeometry#boundary}). */
    public static BitSet boundary(WindowProfile profile, WindowPose pose, Collection<Direction> connected, Direction side) {
        return BOUNDARIES.computeIfAbsent(key(profile, pose, connected), k -> {
            var boxes = WindowPaneGeometry.pieces(profile, pose, connected).stream().map(Piece::box).toList();
            BitSet[] sides = new BitSet[6];
            for (Direction direction : Direction.values()) {
                sides[direction.ordinal()] = WindowPaneGeometry.boundary(boxes, direction);
            }
            return sides;
        })[side.ordinal()];
    }

    private static Key key(WindowProfile profile, WindowPose pose, Collection<Direction> connected) {
        int mask = 0;
        for (Direction side : connected) {
            mask |= 1 << side.ordinal();
        }
        return new Key(profile, pose, mask);
    }
}
