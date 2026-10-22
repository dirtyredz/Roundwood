package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.block.window.geometry.*;
import io.github.dirtyredz.roundwood.block.window.geometry.face.*;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class WindowBeamCullingTest {
    @Test
    void beamEndsRemainAgainstPlainGlazingAndDisappearAgainstFittedBeams() {
        var pose = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        var end = new Surface(new Box(15.5, 7.5, 7, 16, 8.5, 9), Direction.EAST, WindowPaneGeometry.BEAM);
        var plain = new WindowPaneState.Snapshot(WindowDesign.plain(WindowProfile.EDGE_TOP), pose, Set.of());
        var fitted = new WindowPaneState.Snapshot(new WindowDesign(WindowProfile.EDGE_TOP, WindowPattern.CROSS), pose, Set.of());
        assertTrue(WindowBeamCulling.visible(end, Map.of()));
        assertTrue(WindowBeamCulling.visible(end, Map.of(Direction.EAST, plain)));
        assertFalse(WindowBeamCulling.visible(end, Map.of(Direction.EAST, fitted)));
        assertTrue(WindowBeamCulling.visible(end, Map.of(Direction.EAST,
                new WindowPaneState.Snapshot(fitted.design(), WindowPose.upright(Direction.SOUTH, WindowDepth.NEGATIVE), Set.of()))));
    }

    @Test
    void snapshotsOwnTheirInputs() {
        var connections = java.util.EnumSet.of(Direction.NORTH);
        var pane = new WindowPaneState.Snapshot(WindowDesign.plain(WindowProfile.ROUND),
                WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER), connections);
        connections.clear();
        assertEquals(Set.of(Direction.NORTH), pane.connections());
        assertThrows(UnsupportedOperationException.class, () -> pane.connections().clear());
    }
}
