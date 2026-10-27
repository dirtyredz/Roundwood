package io.github.dirtyredz.roundwood.block.window.geometry.face;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometryTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FramelessWindowGeometryTest {
    @Test
    void beamsRemainRuntimeGeometryWithoutInventingARetiredProduct() {
        var blank = WindowProfile.BLANK;
        var crossed = new WindowDesign(blank, WindowPattern.CROSS);
        assertTrue(WindowDesign.all().contains(crossed));
        var registered = io.github.dirtyredz.roundwood.registry.ModBlocks.WindowProducts.all();
        assertFalse(registered.contains(crossed));
        assertEquals(13, registered.stream().filter(d -> d.pattern() == WindowPattern.CROSS).count());
        assertEquals(14, registered.stream().filter(d -> d.pattern() == WindowPattern.PLAIN).count());
        for (var pose : WindowPaneGeometryTest.allPoses()) {
            var pieces = WindowPaneGeometry.panel(crossed, pose);
            assertTrue(pieces.stream().anyMatch(p -> p.texture().equals(WindowPaneGeometry.BEAM)));
            assertFalse(pieces.stream().anyMatch(p -> p.texture().equals(WindowPaneGeometry.FRAME)));
        }
    }

    @Test
    void absentFrameCannotBeStyledOrTrimmedButGlassAndBeamsStillWork() {
        var glass = io.github.dirtyredz.roundwood.block.window.interaction.WindowHitRegions.Region.GLASS;
        for (var item : java.util.List.of(
                io.github.dirtyredz.roundwood.block.window.interaction.WindowUseRules.Item.PLANKS,
                io.github.dirtyredz.roundwood.block.window.interaction.WindowUseRules.Item.UNIVERSAL_TRIM)) {
            assertEquals(io.github.dirtyredz.roundwood.block.window.interaction.WindowUseRules.Action.REFUSE_FRAME,
                    io.github.dirtyredz.roundwood.block.window.interaction.WindowUseRules.decide(item, glass, false, false));
        }
        assertFalse(io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry.supports(
                io.github.dirtyredz.roundwood.block.attachment.TrimSlot.PERIMETER, WindowProfile.BLANK));
    }

    @Test
    void fullGlazingWithoutFrameInEveryPoseAndDepth() {
        WindowProfile blank = java.util.Arrays.stream(WindowProfile.values())
                .filter(profile -> profile.id().equals("frameless_window"))
                .findFirst().orElseThrow(() -> new AssertionError("Missing frameless window profile"));
        assertTrue(blank.oriented());
        assertFalse(blank.inAssembly());
        assertFalse(blank.takesSill());
        for (int v = 0; v < 16; v++) for (int u = 0; u < 16; u++) {
            assertTrue(blank.inOpening(u, v));
        }
        for (var pose : WindowPaneGeometryTest.allPoses()) {
            var pieces = WindowPaneGeometry.panel(WindowDesign.plain(blank), pose);
            assertEquals(1, pieces.size(), "One solid glazing slab, without internal seams: " + pose);
            assertEquals(WindowPaneGeometry.GLASS, pieces.get(0).texture());
            assertEquals(pose.toWorld(0, 0, 16, 16,
                    pose.depth().slabMin() + 0.5, pose.depth().slabMax() - 0.5), pieces.get(0).box());
        }
    }
}
