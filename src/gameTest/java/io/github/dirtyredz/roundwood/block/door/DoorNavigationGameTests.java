package io.github.dirtyredz.roundwood.block.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.fixture.VillagerPassage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * Villagers use the assembly through their normal brain: vanilla's InteractWithDoor opens it via
 * {@link DoorBlock#setOpen} (gated on {@code minecraft:wooden_doors} and the DoorBlock class), the
 * walk node evaluator treats it as a wooden door, and vanilla's close path shuts all four parts.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorNavigationGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 4);

    private DoorNavigationGameTests() {
    }

    /** A corridor (x 3..4) with a wall across z = 4 holding the door; the only way through is the door. */
    private static DoorLayout.Assembly corridor(GameTestHelper helper, String name) {
        return corridor(helper, name, DoorHingeSide.LEFT);
    }

    /** The corridor has walls on both sides, so only the explicit click point chooses the hinge. */
    private static DoorLayout.Assembly corridor(GameTestHelper helper, String name, DoorHingeSide hinge) {
        VillagerPassage.corridor(helper);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR, hinge).consumesAction(), "placement failed");
        DoorLayout.Assembly assembly = assembly(helper, ANCHOR, Direction.NORTH, hinge);
        check(assembly.complete(helper.getLevel(), oakDoor()), "door has the wrong hinge: " + helper.getBlockState(ANCHOR));
        return assembly;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksNorthToSouthThroughTheDoor(GameTestHelper helper) {
        walkThrough(helper, "nav_south", new BlockPos(3, 1, 1), new BlockPos(3, 1, 7));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksSouthToNorthThroughTheDoor(GameTestHelper helper) {
        walkThrough(helper, "nav_north", new BlockPos(4, 1, 7), new BlockPos(4, 1, 1));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksThroughARightHingedDoor(GameTestHelper helper) {
        walkThrough(helper, "nav_right", new BlockPos(3, 1, 1), new BlockPos(3, 1, 7), DoorHingeSide.RIGHT);
    }

    /** Camouflaged corners are decorative: they must not change passage. */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksThroughACamouflagedDoor(GameTestHelper helper) {
        walkThrough(helper, "nav_camo", new BlockPos(3, 1, 1), new BlockPos(3, 1, 7), DoorHingeSide.LEFT, true);
    }

    private static void walkThrough(GameTestHelper helper, String name, BlockPos start, BlockPos target) {
        walkThrough(helper, name, start, target, DoorHingeSide.LEFT);
    }

    private static void walkThrough(GameTestHelper helper, String name, BlockPos start, BlockPos target, DoorHingeSide hinge) {
        walkThrough(helper, name, start, target, hinge, false);
    }

    private static void walkThrough(GameTestHelper helper, String name, BlockPos start, BlockPos target, DoorHingeSide hinge,
                                    boolean camouflaged) {
        DoorLayout.Assembly assembly = corridor(helper, name, hinge);
        if (camouflaged) {
            anchorEntity(helper, assembly).setCamouflage(CamouflageData.free(Blocks.STONE.defaultBlockState()));
        }
        Villager villager = VillagerPassage.spawn(helper, start, target);
        BlockPos absoluteTarget = helper.absolutePos(target);
        AtomicBoolean sawOpen = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty() && !arrived(helper, villager, target)) {
                villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(absoluteTarget, 0.6F, 0));
            }
            if (helper.getLevel().getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN)) {
                sawOpen.set(true);
                // Opening is all-or-nothing even when a villager does it.
                for (BlockPos pos : assembly.positions()) {
                    check(helper.getLevel().getBlockState(pos).getValue(DoorBlock.OPEN), "villager opened only part of the door");
                }
            }
        });
        // Closing behind itself depends on vanilla's door-memory timing (vanilla villagers also leave
        // wooden doors open sometimes); the close path itself is pinned by vanillaCloseBehaviourClosesAllFour.
        helper.succeedWhen(() -> {
            check(arrived(helper, villager, target), "villager has not reached the far side: at " + villager.blockPosition()
                    + ", expected " + absoluteTarget + ", walk target "
                    + villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).map(w -> w.getTarget().currentBlockPosition())
                    + ", path " + (villager.getNavigation().getPath() == null ? "none" : villager.getNavigation().getPath().getTarget())
                    + ", activities " + villager.getBrain().getActiveActivities());
            check(sawOpen.get(), "the door never opened");
            villager.discard();
        });
    }

    private static boolean arrived(GameTestHelper helper, Villager villager, BlockPos target) {
        Vec3 origin = Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO));
        Vec3 rel = villager.position().subtract(origin);
        boolean pastWall = target.getZ() > ANCHOR.getZ() ? rel.z > ANCHOR.getZ() + 1.5 : rel.z < ANCHOR.getZ() - 0.5;
        return pastWall && Math.abs(rel.z - (target.getZ() + 0.5)) < 1.5;
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaCloseBehaviourClosesAllFour(GameTestHelper helper) {
        DoorLayout.Assembly assembly = corridor(helper, "nav_close");
        Villager villager = helper.spawn(EntityType.VILLAGER, ANCHOR.south());
        BlockPos remembered = assembly.pos(DoorLayout.Part.LOWER_RIGHT);
        oakDoor().setOpen(villager, helper.getLevel(), helper.getLevel().getBlockState(remembered), remembered, true);
        for (BlockPos pos : assembly.positions()) {
            check(helper.getLevel().getBlockState(pos).getValue(DoorBlock.OPEN), "open request missed " + pos);
        }
        Set<GlobalPos> doors = new HashSet<>(Set.of(GlobalPos.of(helper.getLevel().dimension(), remembered)));
        InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough(helper.getLevel(), villager, null, null, doors, Optional.empty());
        for (BlockPos pos : assembly.positions()) {
            check(!helper.getLevel().getBlockState(pos).getValue(DoorBlock.OPEN), "vanilla close left " + pos + " open");
        }
        villager.discard();
        helper.succeed();
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
