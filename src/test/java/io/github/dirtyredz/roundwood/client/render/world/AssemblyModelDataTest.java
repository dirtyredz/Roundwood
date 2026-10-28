package io.github.dirtyredz.roundwood.client.render.world;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AssemblyModelDataTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }

    @Test void regionMembershipRejectsMissingReplacedAndUnavailableDataWithoutMutatingRetainedSnapshots() {
        var assembly = new DoorLayout.Assembly(new BlockPos(15, 15, 15), Direction.NORTH,
                DoorHingeSide.LEFT, DoorDesign.TALL_DOUBLE_ARCHED);
        var states = new HashMap<BlockPos, BlockState>();
        for (var cell : assembly.positions()) states.put(cell, Blocks.STONE.defaultBlockState());
        var snapshot = new AssemblyRenderSnapshot(1, assembly, states, true, Appearance.DEFAULT,
                FaceTrims.NONE, io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderExtras.NONE);
        assertTrue(AssemblyModelData.matches(assembly, snapshot, states::get));
        assertFalse(AssemblyModelData.matches(assembly, null, states::get)); // satellite arrives first
        var satellite = assembly.positions().get(1);
        states.remove(satellite); // unloaded / anchor-first arrival
        assertFalse(AssemblyModelData.matches(assembly, snapshot, states::get));
        assertEquals(12, snapshot.members().size());
        states.put(satellite, Blocks.DIRT.defaultBlockState());
        assertFalse(AssemblyModelData.matches(assembly, snapshot, states::get));
        states.put(satellite, Blocks.STONE.defaultBlockState());
        assertTrue(AssemblyModelData.matches(assembly, snapshot, states::get));
        var retired = new AssemblyRenderSnapshot(2, assembly, states, false, Appearance.DEFAULT,
                FaceTrims.NONE, io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderExtras.NONE);
        assertFalse(AssemblyModelData.matches(assembly, retired, pos -> { throw new AssertionError("retired data read region"); }));
        var other = new DoorLayout.Assembly(assembly.anchor().east(), assembly.facing(), assembly.hinge(), assembly.design());
        assertFalse(AssemblyModelData.matches(other, snapshot, pos -> { throw new AssertionError("wrong identity read region"); }));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.members().clear());
    }
}
