package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import java.util.Map;
import java.util.List;
import java.util.BitSet;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;

/** Conservative boundary visibility using immutable neighbour snapshots only. */
public final class WindowBeamCulling {
    private record Coverage(WindowPaneState.Snapshot pane, Direction side, boolean glass) { }
    private static final AppearanceModelCache<Coverage, BitSet> BOUNDARIES =
            new AppearanceModelCache<>(2048);
    private WindowBeamCulling() { }

    public static boolean visible(Surface surface, Map<Direction, WindowPaneState.Snapshot> neighbours) {
        var neighbour = neighbours.get(surface.face());
        if (neighbour == null) return true;
        BitSet boundary = WindowPaneGeometry.boundary(List.of(surface.backing()), surface.face());
        if (boundary.isEmpty()) return true;
        BitSet covered = BOUNDARIES.get(new Coverage(neighbour, surface.face().getOpposite(), surface.texture().equals(WindowPaneGeometry.GLASS)), key -> {
            var pieces = WindowPaneGeometry.pieces(key.pane().design(), key.pane().pose(), key.pane().connections()).stream()
                    .filter(piece -> key.glass() || !piece.texture().equals(WindowPaneGeometry.GLASS)).map(Piece::box).toList();
            return WindowPaneGeometry.boundary(pieces, key.side());
        });
        boundary.andNot(covered);
        return !boundary.isEmpty();
    }
}
