package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardware;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.testsupport.QuadModel;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class DoorLeafRenderStateTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    private static volatile Object sink;
    @Test void appearanceAndHardwareChangesResolveOnlyTheAffectedCachedInputs() {
        var state = new DoorLeafRenderState(new DoorLayout.Assembly(BlockPos.ZERO, Direction.WEST, DoorHingeSide.RIGHT, DoorDesign.ROUND));
        var calls = new int[1];
        Function<Appearance, DoorAppearanceModels.Look> looks = appearance -> new DoorAppearanceModels.Look(appearance.material(), -1);
        DoorLeafRenderState.Models models = (look, design, hinge, hardware) -> {
            assertEquals(DoorHingeSide.RIGHT, hinge); calls[0]++; return new QuadModel(List.of(), null);
        };
        var spruce = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));
        var hardware = new DoorHardware(java.util.Optional.of(spruce), java.util.Optional.empty());
        state.material(Appearance.DEFAULT, DoorHardware.DEFAULT, 0, looks, models);
        state.material(spruce, DoorHardware.DEFAULT, 0, looks, models);
        state.material(spruce, hardware, 0, looks, models);
        state.material(spruce, hardware, 0, looks, models);
        assertEquals(3, calls[0]);
    }
    @Test void materialsAndReloadInvalidateWhileWarmBoundsAndLookupsAllocateNothing() {
        var assembly = new DoorLayout.Assembly(BlockPos.ZERO, Direction.NORTH, DoorHingeSide.LEFT, DoorDesign.TALL_DOUBLE_ARCHED);
        var state = new DoorLeafRenderState(assembly);
        var calls = new int[1];
        Function<Appearance, DoorAppearanceModels.Look> looks = appearance -> new DoorAppearanceModels.Look(
                ResourceLocation.fromNamespaceAndPath("test", "panel"), -1);
        DoorLeafRenderState.Models models = (look, design, hinge, hardware) -> {
            calls[0]++; return new QuadModel(List.of(), null);
        };
        state.material(Appearance.DEFAULT, DoorHardware.DEFAULT, 1, looks, models);
        assertEquals(2, calls[0]);
        assertEquals(assembly.sweepBounds(), state.bounds());
        assertEquals(DoorHingeSide.LEFT, state.hinge(0));
        assertEquals(DoorHingeSide.RIGHT, state.hinge(1));
        var previous = state.model(0);
        state.material(Appearance.DEFAULT, DoorHardware.DEFAULT, 2, looks, models);
        assertEquals(4, calls[0]); assertNotSame(previous, state.model(0));
        for (int i = 0; i < 20000; i++) warm(state, looks, models);
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().getId();
        long start = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 100000; i++) warm(state, looks, models);
        long bytes = bean.getThreadAllocatedBytes(thread) - start;
        System.out.printf("Warm leaf lookup/bounds/material reuse: %.3f B/op (%d bytes / 100000)%n", bytes / 100000.0, bytes);
        assertEquals(4, calls[0]);
        assertTrue(bytes <= 1024, "Warm operations allocated " + bytes + " bytes");
    }
    private static void warm(DoorLeafRenderState state, Function<Appearance, DoorAppearanceModels.Look> looks, DoorLeafRenderState.Models models) {
        state.material(Appearance.DEFAULT, DoorHardware.DEFAULT, 2, looks, models);
        sink = state.bounds(); sink = state.model(0); sink = state.hinge(1);
        sink = DoorLeafModels.location(DoorDesign.TALL_DOUBLE_ARCHED, DoorHingeSide.RIGHT);
        sink = DoorLayout.Part.of(DoorDesign.TALL_DOUBLE_ARCHED);
    }
}
