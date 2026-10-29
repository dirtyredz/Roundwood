package io.github.dirtyredz.roundwood.fixture.door;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;

/** A bounded passage and a destination, with vanilla villager AI left intact. */
public final class VillagerPassage {
    private VillagerPassage() {}

    public static void corridor(GameTestHelper helper) {
        DoorFixtures.floor(helper);
        for (int y = 1; y <= 3; y++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(2, y, z), Blocks.STONE);
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
            // Open ends let idle villagers leave the raised floor, get stuck outside, or bypass
            // the door entirely. Keep both the actor and its path search inside this test's plot.
            for (int x = 3; x <= 4; x++) {
                helper.setBlock(new BlockPos(x, y, 0), Blocks.STONE);
                helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
            }
        }
    }

    public static Villager spawn(GameTestHelper helper, BlockPos start, BlockPos target) {
        Villager villager = helper.spawn(EntityType.VILLAGER, start);
        // Supply the intent before the first brain tick can choose a random idle stroll.
        // Do not disable AI or drive navigation directly: MoveToTargetSink and InteractWithDoor
        // must still find the route and open the real door themselves.
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(helper.absolutePos(target), 0.6F, 0));
        return villager;
    }
}
