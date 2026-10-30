package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorFeedbackUpgradeGameTests {
    @GameTest(template="doors_v0160", timeoutTicks=60)
    public static void oldDoorsKeepTheirPossessionsAndGainTheNewGeometry(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            int[] xs = {1, 5, 8, 12};
            DoorDepth[] depths = {DoorDepth.BACK, DoorDepth.MIDDLE, DoorDepth.FRONT, DoorDepth.BACK};
            String[] ids = {"round_door", "arched_door", "double_arched_door", "tall_double_arched_door"};
            int[] cells = {4, 2, 9, 12};
            for (int i=0; i<xs.length; i++) {
                var pos = helper.absolutePos(new BlockPos(xs[i],2,2));
                var state = helper.getLevel().getBlockState(pos);
                var assembly = DoorLayout.of(pos,state);
                check(assembly.facing()==Direction.NORTH, "historical facing");
                check(assembly.depth()==depths[i] && assembly.design().id().equals(ids[i]), "historical identity/depth");
                check(assembly.parts().size()==cells[i], "historical footprint");
                check(assembly.hinge()==(i==0 || i==3 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT), "historical hinge");
                check(assembly.complete(helper.getLevel(), state.getBlock()), "historical cells lost");
                check(state.getValue(DoorBlock.OPEN)==(i==3) && state.getValue(DoorBlock.POWERED)==(i==3), "historical power/open " + ids[i] + " " + state);
                var entity = (RoundDoorBlockEntity) helper.getLevel().getBlockEntity(pos);
                check(entity.appearance().material().toString().equals("minecraft:spruce_planks"), "historical panel look");
                var before = entity.saveWithFullMetadata();
                check(before.getCompound("DoorTrim").getCompound("Front").getCompound("RoundwoodAppearance")
                        .getString("Material").equals("minecraft:bricks"), "historical trim");
                entity.load(before);
                check(before.equals(entity.saveWithFullMetadata()), "reload changed saved possession");
                if (i==0) check(entity.camouflage().orElseThrow().paid(), "camouflage refund lost");
                if (i>=2) {
                    check(entity.arch().isPresent(), "historical fitted top");
                    check(assembly.design().inLeaf(23,assembly.design().height()-1), "replacement crown missing");
                }
            }
            helper.succeed();
        });
    }
}
