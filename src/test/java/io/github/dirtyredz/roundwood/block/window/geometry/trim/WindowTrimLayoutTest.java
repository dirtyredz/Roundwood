package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every window attachment draws an explicit artwork layout (0.10.0), written out here: the lower arches
 * use the full radial circle (the arched art has straight courses below its spring line), the top
 * corners a head, single edges their own 16-pixel run, the sill its course. The sill under an upper
 * arch samples the artwork row just above the spring line, and a single edge spans 16 pixels.
 */
class WindowTrimLayoutTest {
    private static final Map<WindowProfile, WindowTrimLayout> PERIMETER = Map.ofEntries(
            entry(WindowProfile.ROUND, WindowTrimLayout.ROUND),
            entry(WindowProfile.ARCH_LEFT, WindowTrimLayout.ARCH),
            entry(WindowProfile.ARCH_RIGHT, WindowTrimLayout.ARCH),
            entry(WindowProfile.BOTTOM_LEFT, WindowTrimLayout.ARCH),
            entry(WindowProfile.BOTTOM_RIGHT, WindowTrimLayout.ARCH),
            entry(WindowProfile.ARCH_BOTTOM_LEFT, WindowTrimLayout.CIRCLE),
            entry(WindowProfile.ARCH_BOTTOM_RIGHT, WindowTrimLayout.CIRCLE),
            entry(WindowProfile.TOP_LEFT, WindowTrimLayout.FRAME),
            entry(WindowProfile.TOP_RIGHT, WindowTrimLayout.FRAME),
            entry(WindowProfile.EDGE_TOP, WindowTrimLayout.EDGE),
            entry(WindowProfile.EDGE_LEFT, WindowTrimLayout.EDGE),
            entry(WindowProfile.EDGE_BOTTOM, WindowTrimLayout.EDGE),
            entry(WindowProfile.EDGE_RIGHT, WindowTrimLayout.EDGE));

    @Test
    void everyProfileAndSlotHasItsLayout() {
        assertEquals(WindowProfile.values().length - 1, PERIMETER.size());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> WindowTrimLayout.of(TrimSlot.PERIMETER, WindowProfile.BLANK));
        for (WindowProfile profile : PERIMETER.keySet()) {
            assertEquals(PERIMETER.get(profile), WindowTrimLayout.of(TrimSlot.PERIMETER, profile), profile.id());
            assertEquals(WindowTrimLayout.SILL, WindowTrimLayout.of(TrimSlot.SILL, profile), profile.id());
        }
    }

    @Test
    void wholeFaceArtworkSpansThePiecesOwnExtent() {
        WindowPose front = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        // a lower arch's top edge meets an upper arch's bottom edge in the middle of the 32-pixel image
        assertArrayEquals(WindowTrimUv.whole(WindowProfile.ARCH_LEFT, front, 4, 0, 10, true),
                WindowTrimUv.whole(WindowProfile.ARCH_BOTTOM_LEFT, front, 4, 16, 10, true), 1e-6F);
        assertArrayEquals(new float[] {2, 8}, WindowTrimUv.whole(WindowProfile.ARCH_BOTTOM_LEFT, front, 4, 16, 10, true), 1e-6F);
        // a top corner sits in the upper half
        assertArrayEquals(new float[] {2, 0}, WindowTrimUv.whole(WindowProfile.TOP_LEFT, front, 4, 16, 10, true), 1e-6F);
        // a single edge spans its own 16 pixels, once
        assertArrayEquals(new float[] {4, 16}, WindowTrimUv.whole(WindowProfile.EDGE_BOTTOM, front, 4, 0, 10, true), 1e-6F);
        assertArrayEquals(new float[] {16, 0}, WindowTrimUv.whole(WindowProfile.EDGE_TOP, front, 16, 16, 10, true), 1e-6F);
        // the sill (rows 0..2 of an upper arch) samples sprite rows 7..8 of the 2 x 2 window's image
        assertArrayEquals(new float[] {1, 8}, WindowTrimUv.whole(WindowProfile.ARCH_LEFT, front, 2, 0, 10, true), 1e-6F);
        assertArrayEquals(new float[] {1, 7}, WindowTrimUv.whole(WindowProfile.ARCH_LEFT, front, 2, 2, 10, true), 1e-6F);
    }
}
