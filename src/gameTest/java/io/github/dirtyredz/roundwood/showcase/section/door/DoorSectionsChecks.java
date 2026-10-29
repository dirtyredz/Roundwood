package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;

/**
 * The door-family examples of the showcase read back from the world: the one camouflaged door and
 * the lever-driven redstone door. Called right after building (camouflage) and once after it
 * (redstone, which changes the world), for every facing the showcase test builds.
 */
public final class DoorSectionsChecks {
    private DoorSectionsChecks() {
    }

    public static void verifyCamouflage(ServerLevel level, ShowcaseFrame frame) {
        BlockPos anchor = frame.pos(CamouflageSection.R, 0, CamouflageSection.F);
        BlockState door = level.getBlockState(anchor);
        check(door.is(ModBlocks.roundDoor()) && !door.getValue(DoorBlock.OPEN), "camouflage door missing or open");
        check(level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity
                        && entity.camouflage().isPresent()
                        && entity.camouflage().get().material().is(CamouflageSection.MATERIAL),
                "camouflage not applied");
    }

    public static void verifyRedstone(ServerLevel level, ShowcaseFrame frame) {
        BlockPos door = frame.pos(RedstoneSection.DOOR_R, 0, RedstoneSection.DOOR_F);
        BlockPos leverPos = frame.pos(RedstoneSection.LEVER_R, 0, RedstoneSection.DOOR_F);
        BlockState lever = level.getBlockState(leverPos);
        check(lever.is(Blocks.LEVER) && !lever.getValue(LeverBlock.POWERED), "lever missing or already on");
        check(!level.getBlockState(door).getValue(DoorBlock.OPEN), "redstone door starts open");
        ((LeverBlock) Blocks.LEVER).pull(lever, level, leverPos);
        check(level.getBlockState(door).getValue(DoorBlock.OPEN), "flipping the lever did not open the door");
    }
}
