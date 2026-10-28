package io.github.dirtyredz.roundwood.client.door.hardware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorHardwarePreviewCacheTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    @Test void previewGeometrySurvivesProposalRevalidation() {
        var door=new io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly(net.minecraft.core.BlockPos.ZERO,
                net.minecraft.core.Direction.NORTH,net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT,
                io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND);
        var boxes=io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareGeometry.placed(door.design(),door.hinge(),true).stream().map(p->p.box()).toList();
        var proposal=new io.github.dirtyredz.roundwood.block.door.interaction.DoorHardwareTarget.Proposal(door,
                io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareSlot.HINGES,
                io.github.dirtyredz.roundwood.block.door.hardware.DoorHardware.DEFAULT,
                io.github.dirtyredz.roundwood.appearance.Appearance.DEFAULT,true,true,boxes);
        var shape=DoorHardwarePreviewCache.shape(proposal);
        for(int frame=0;frame<1000;frame++)assertSame(shape,DoorHardwarePreviewCache.shape(proposal));
    }
}
