package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.*;
import io.github.dirtyredz.roundwood.block.window.placement.WindowOrientation;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WindowBeamBoundaryTest {
    @Test
    void everyPlainBoundaryIsASubsetOfItsFittedBoundary() {
        var sides = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        for (var profile : WindowProfile.values()) for (var orientation : WindowOrientation.values())
            for (var depth : WindowDepth.values()) for (int mask = 0; mask < 16; mask++) {
                var connections = EnumSet.noneOf(Direction.class);
                for (int i = 0; i < 4; i++) if ((mask & (1 << i)) != 0) connections.add(sides.get(i));
                var pose = orientation.pose(depth);
                var plain = WindowPaneGeometry.pieces(WindowDesign.plain(profile), pose, connections).stream().map(Piece::box).toList();
                var fitted = WindowPaneGeometry.pieces(new WindowDesign(profile, WindowPattern.CROSS), pose, connections).stream().map(Piece::box).toList();
                for (var side : Direction.values()) {
                    var uncovered = WindowPaneGeometry.boundary(plain, side);
                    uncovered.andNot(WindowPaneGeometry.boundary(fitted, side));
                    if (!uncovered.isEmpty()) fail(profile + " " + pose + " " + connections + " lost coverage on " + side);
                }
            }
    }
}
