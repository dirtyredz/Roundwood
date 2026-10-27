package io.github.dirtyredz.roundwood.client.door;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DoorPreviewMemoTest {
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
    record Target(int hit,int held,int revision) {}
    @Test void repeatedFramesReuseProposalsAndGeometryIncludingMisses() {
        var memo=new DoorPreviewMemo<Target,Optional<Object>>();var calls=new AtomicInteger();
        var target=new Target(1,1,1);
        java.util.function.Supplier<Optional<Object>> build=()->{calls.incrementAndGet();return Optional.of(new Object());};
        var first=memo.get(target,build);
        for(int frame=0;frame<1000;frame++)assertSame(first,memo.get(new Target(1,1,1),build));
        assertEquals(1,calls.get());
        memo.get(new Target(2,1,1),build);memo.get(new Target(2,2,1),build);memo.get(new Target(2,2,2),build);
        assertEquals(4,calls.get());
        memo.clear();memo.get(new Target(2,2,2),build);assertEquals(5,calls.get());
        var miss=new Target(0,0,0);
        for(int frame=0;frame<1000;frame++)assertTrue(memo.get(miss,()->{calls.incrementAndGet();return Optional.empty();}).isEmpty());
        assertEquals(6,calls.get());
    }
}
