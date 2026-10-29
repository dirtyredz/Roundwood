package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import net.minecraft.server.level.ServerLevel;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;

public final class HardwareShowcaseChecks {
    private HardwareShowcaseChecks() {}
    public static void verify(ServerLevel level,ShowcaseFrame frame){
        var blocks=ModBlocks.doors();
        for(int i=0;i<blocks.size();i++){
            var pos=frame.pos(DoorHardwareSection.r(i),0,DoorHardwareSection.F);
            var state=level.getBlockState(pos);
            check(state.is(blocks.get(i)) && DoorLayout.of(pos,state).complete(level,blocks.get(i)),"hardware sample incomplete");
            var entity=(RoundDoorBlockEntity)level.getBlockEntity(pos);
            check(entity.hardware().equals(DoorHardwareSection.HARDWARE) && entity.appearance().equals(DoorHardwareSection.body(i)),"hardware sample finish");
        }
    }
}
