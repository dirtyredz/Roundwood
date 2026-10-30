package io.github.dirtyredz.roundwood.block.door.camouflage;

import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assertNoDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.blockEntities;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.rel;

/** Taking a camouflaged door down refunds the paid material exactly once, and only where the door itself would drop. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CamouflageRemovalGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private CamouflageRemovalGameTests() {
    }

    /** Test-only stand-in for a protection mod: cancels breaks by one designated fake player. */
    @EventBusSubscriber(modid = Roundwood.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static final class BreakCanceller {
        private BreakCanceller() {
        }

        @SubscribeEvent
        public static void onBreak(BlockEvent.BreakEvent event) {
            if ("rw_camo_cancelled".equals(event.getPlayer().getName().getString())) {
                event.setCanceled(true);
            }
        }
    }

    private static Item door() {
        return oakDoor().asItem();
    }

    private static CamouflageData paid() {
        return CamouflageData.paid(Blocks.STONE.defaultBlockState(), new ItemStack(Items.STONE));
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, String name) {
        return place(helper, name, DoorHingeSide.LEFT, false, paid());
    }

    /** Places a camouflaged door (hinge, optionally opened) and clears the drops. */
    private static DoorLayout.Assembly place(GameTestHelper helper, String name, DoorHingeSide hinge, boolean open, CamouflageData data) {
        ServerPlayer placer = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, door(), ANCHOR, hinge).consumesAction(), "placement failed");
        if (open) {
            oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(helper.absolutePos(ANCHOR)),
                    helper.absolutePos(ANCHOR), true);
        }
        DoorLayout.Assembly assembly = assembly(helper, ANCHOR, Direction.NORTH, hinge);
        anchorEntity(helper, assembly).setCamouflage(data);
        clearDrops(helper);
        return assembly;
    }

    private static void assertDrops(GameTestHelper helper, String name, int doors, int stone) {
        check(dropped(helper, door()) == doors, name + ": expected " + doors + " door(s), got " + dropped(helper, door()));
        check(dropped(helper, Items.STONE) == stone, name + ": expected " + stone + " stone, got " + dropped(helper, Items.STONE));
    }

    private static void assertGone(GameTestHelper helper, String name, DoorLayout.Assembly assembly) {
        assertNoDoor(helper, assembly);
        check(blockEntities(helper, assembly) == 0, name + ": block entity left behind");
        check(!(helper.getLevel().getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity),
                name + ": the anchor still holds a door entity that could refund again");
    }

    @GameTest(template = TEMPLATE)
    public static void survivalBreakOfAnyPartDropsTheDoorAndOneRefund(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (boolean open : new boolean[] {false, true}) {
                for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
                    String name = hinge + "/open=" + open + "/" + part;
                    DoorLayout.Assembly assembly = place(helper, "camo_survival", hinge, open, paid());
                    ServerPlayer breaker = player(helper, "camo_survival_breaker", GameType.SURVIVAL, Direction.NORTH);
                    check(breaker.gameMode.destroyBlock(assembly.pos(part)), name + ": break refused");
                    assertGone(helper, name, assembly);
                    assertDrops(helper, name, 1, 1);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeBreakOfAnyPartDiscardsTheCamouflage(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (boolean open : new boolean[] {false, true}) {
                for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
                    String name = hinge + "/open=" + open + "/" + part;
                    DoorLayout.Assembly assembly = place(helper, "camo_creative", hinge, open, paid());
                    ServerPlayer breaker = player(helper, "camo_creative_breaker", GameType.CREATIVE, Direction.NORTH);
                    check(breaker.gameMode.destroyBlock(assembly.pos(part)), name + ": break refused");
                    assertGone(helper, name, assembly);
                    assertDrops(helper, name, 0, 0);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void freeCamouflageRefundsNothing(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, "camo_free", DoorHingeSide.LEFT, false,
                CamouflageData.free(Blocks.STONE.defaultBlockState()));
        ServerPlayer breaker = player(helper, "camo_free_breaker", GameType.SURVIVAL, Direction.NORTH);
        check(breaker.gameMode.destroyBlock(assembly.pos(DoorLayout.Part.UPPER_LEFT)), "break refused");
        assertGone(helper, "free", assembly);
        assertDrops(helper, "free", 1, 0);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void losingSupportUnderEitherColumnRefundsOnce(GameTestHelper helper) {
        for (BlockPos support : new BlockPos[] {ANCHOR.below(), ANCHOR.below().east()}) {
            floor(helper);
            DoorLayout.Assembly assembly = place(helper, "camo_support");
            helper.setBlock(support, Blocks.AIR);
            assertGone(helper, "support " + support, assembly);
            assertDrops(helper, "support " + support, 1, 1);
            clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void explosionRefundsExactlyOnce(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, "camo_explosion");
        BlockPos centre = assembly.pos(DoorLayout.Part.UPPER_RIGHT);
        helper.getLevel().explode(null, centre.getX(), centre.getY(), centre.getZ() - 0.5, 3.0F, Level.ExplosionInteraction.TNT);
        assertGone(helper, "explosion", assembly);
        check(dropped(helper, door()) <= 1, "explosion dropped " + dropped(helper, door()) + " doors");
        check(dropped(helper, Items.STONE) == 1, "explosion refunded " + dropped(helper, Items.STONE) + " stone, expected exactly 1");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void replacementRefundsOnceAndDropsNoDoor(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, "camo_replace");
        helper.setBlock(rel(helper, assembly.pos(DoorLayout.Part.UPPER_LEFT)), Blocks.STONE);
        assertGone(helper, "replacement", assembly);
        assertDrops(helper, "replacement", 0, 1);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fireCleanupRefundsOnceAndDropsNoDoor(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, "camo_fire");
        helper.getLevel().removeBlock(assembly.pos(DoorLayout.Part.LOWER_RIGHT), false);
        assertGone(helper, "removeBlock", assembly);
        assertDrops(helper, "removeBlock", 0, 1);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "roundwood_gamerules")
    public static void doTileDropsFalseDropsNothing(GameTestHelper helper) {
        floor(helper);
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        boolean before = rule.get();
        rule.set(false, helper.getLevel().getServer());
        try {
            DoorLayout.Assembly assembly = place(helper, "camo_tiledrops");
            ServerPlayer breaker = player(helper, "camo_tiledrops_breaker", GameType.SURVIVAL, Direction.NORTH);
            breaker.gameMode.destroyBlock(assembly.pos(DoorLayout.Part.UPPER_RIGHT));
            assertGone(helper, "tiledrops", assembly);
            assertDrops(helper, "doTileDrops=false", 0, 0);
        } finally {
            rule.set(before, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancelledBreakLeavesTheDoorAndItsCamouflage(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, "camo_cancel");
        ServerPlayer breaker = player(helper, "camo_cancelled", GameType.SURVIVAL, Direction.NORTH);
        check("rw_camo_cancelled".equals(breaker.getName().getString()), "fixture player is not the cancelled one");
        for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
            check(!breaker.gameMode.destroyBlock(assembly.pos(part)), part + ": the cancelled break went through");
        }
        assertAssembly(helper, assembly, oakDoor(), false);
        check(anchorEntity(helper, assembly).camouflage().isPresent(), "the camouflage was lost by a cancelled break");
        assertDrops(helper, "cancelled", 0, 0);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void incompleteCleanupRefundsAtMostOnce(GameTestHelper helper) {
        floor(helper);
        // A lone left column carrying camouflage, as a command or a structure tool could leave it.
        DoorLayout.Assembly assembly = assembly(helper, ANCHOR, Direction.NORTH);
        helper.setBlock(ANCHOR, assembly.stateFor(DoorLayout.Part.LOWER_LEFT, oakDoor(), false, false));
        helper.setBlock(ANCHOR.above(), assembly.stateFor(DoorLayout.Part.UPPER_LEFT, oakDoor(), false, false));
        anchorEntity(helper, assembly).setCamouflage(paid());
        clearDrops(helper);
        helper.runAfterDelay(3, () -> {
            assertGone(helper, "incomplete", assembly);
            check(helper.getBlockState(ANCHOR).isAir() && helper.getBlockState(ANCHOR.above()).isAir(), "the lone column was not removed");
            // Counted next to the door only: the delay lets neighbouring tests' drops land in the wider test area.
            AABB near = new AABB(helper.absolutePos(ANCHOR)).inflate(2);
            int refunds = helper.getLevel().getEntitiesOfClass(ItemEntity.class, near, e -> e.getItem().is(Items.STONE)).stream()
                    .mapToInt(e -> e.getItem().getCount()).sum();
            check(refunds == 1, "incomplete cleanup refunded " + refunds);
            helper.succeed();
        });
    }
}
