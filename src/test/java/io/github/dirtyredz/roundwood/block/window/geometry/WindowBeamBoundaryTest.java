package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.*;
import io.github.dirtyredz.roundwood.block.window.placement.WindowOrientation;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowBeamBoundaryTest {
    @Test
    void everyPlainBoundaryIsASubsetOfItsFittedBoundary() {
        for (var profile : WindowProfile.values()) for (var orientation : WindowOrientation.values())
            for (var depth : WindowDepth.values()) {
                var pose = orientation.pose(depth);
                var plain = WindowPaneGeometry.panel(WindowDesign.plain(profile), pose).stream().map(Piece::box).toList();
                var fitted = WindowPaneGeometry.panel(new WindowDesign(profile, WindowPattern.CROSS), pose).stream().map(Piece::box).toList();
                for (var side : Direction.values()) {
                    var uncovered = WindowPaneGeometry.boundary(plain, side);
                    uncovered.andNot(WindowPaneGeometry.boundary(fitted, side));
                    if (!uncovered.isEmpty()) fail(profile + " " + pose + " lost coverage on " + side);
                }
            }
    }
}
