package io.github.dirtyredz.roundwood.door;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * Villagers use the assembly through their normal brain: vanilla's InteractWithDoor opens it via
 * {@link DoorBlock#setOpen} (gated on {@code minecraft:wooden_doors} and the DoorBlock class), the
 * walk node evaluator treats it as a wooden door, and the villager closes it behind itself.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorNavigationGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 4);

    private DoorNavigationGameTests() {
    }

    /** A corridor (x 3..4) with a wall across z = 4 holding the door; the only way through is the door. */
    private static DoorLayout.Assembly corridor(GameTestHelper helper, String name) {
        floor(helper);
        for (int z = 0; z < 9; z++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(2, y, z), Blocks.STONE);
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR).consumesAction(), "placement failed");
        return assembly(helper, ANCHOR, Direction.NORTH);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksNorthToSouthThroughTheDoor(GameTestHelper helper) {
        walkThrough(helper, "nav_south", new BlockPos(3, 1, 1), new BlockPos(3, 1, 7));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksSouthToNorthThroughTheDoor(GameTestHelper helper) {
        walkThrough(helper, "nav_north", new BlockPos(4, 1, 7), new BlockPos(4, 1, 1));
    }

    private static void walkThrough(GameTestHelper helper, String name, BlockPos start, BlockPos target) {
        DoorLayout.Assembly assembly = corridor(helper, name);
        Villager villager = helper.spawn(EntityType.VILLAGER, start);
        BlockPos absoluteTarget = helper.absolutePos(target);
        AtomicBoolean sawOpen = new AtomicBoolean();
        StringBuilder transitions = new StringBuilder();
        AtomicBoolean lastOpen = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty() && !arrived(helper, villager, target)) {
                villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(absoluteTarget, 0.6F, 0));
            }
            boolean nowOpen = helper.getLevel().getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN);
            if (nowOpen != lastOpen.getAndSet(nowOpen)) {
                transitions.append(nowOpen ? " open@" : " closed@").append(helper.getTick())
                        .append(String.format("(z=%.2f)", villager.getZ() - helper.absolutePos(BlockPos.ZERO).getZ()));
            }
            if (nowOpen) {
                sawOpen.set(true);
                // Opening is all-or-nothing even when a villager does it.
                for (BlockPos pos : assembly.positions()) {
                    check(helper.getLevel().getBlockState(pos).getValue(DoorBlock.OPEN), "villager opened only part of the door");
                }
            }
        });
        helper.succeedWhen(() -> {
            check(arrived(helper, villager, target), "villager has not reached the far side");
            check(sawOpen.get(), "the door never opened");
            check(!helper.getLevel().getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN), "villager left the door open; villager at "
                    + villager.position().subtract(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO))) + ", doors to close "
                    + villager.getBrain().getMemory(MemoryModuleType.DOORS_TO_CLOSE) + ", path " + villager.getBrain().getMemory(MemoryModuleType.PATH)
                    + ", door anchor " + assembly.anchor() + ", transitions" + transitions);
        });
    }

    private static boolean arrived(GameTestHelper helper, Villager villager, BlockPos target) {
        Vec3 origin = Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO));
        Vec3 rel = villager.position().subtract(origin);
        boolean pastWall = target.getZ() > ANCHOR.getZ() ? rel.z > ANCHOR.getZ() + 1.5 : rel.z < ANCHOR.getZ() - 0.5;
        return pastWall && Math.abs(rel.z - (target.getZ() + 0.5)) < 1.5;
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaDoorQueriesSeeAWoodenDoor(GameTestHelper helper) {
        DoorLayout.Assembly assembly = corridor(helper, "nav_static");
        for (boolean open : new boolean[] {false, true}) {
            oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(assembly.anchor()), assembly.anchor(), open);
            for (BlockPos pos : assembly.positions()) {
                BlockState state = helper.getLevel().getBlockState(pos);
                check(DoorBlock.isWoodenDoor(state), "not a wooden door to vanilla at " + pos);
                check(state.is(BlockTags.WOODEN_DOORS) && state.is(BlockTags.DOORS), "missing door tags at " + pos);
                BlockPathTypes type = WalkNodeEvaluator.getBlockPathTypeStatic(helper.getLevel(), pos.mutable());
                BlockPathTypes expected = open ? BlockPathTypes.DOOR_OPEN : BlockPathTypes.DOOR_WOOD_CLOSED;
                check(type == expected, "path type at " + pos + " is " + type + ", expected " + expected);
            }
        }
        helper.succeed();
    }
}
