package io.github.dirtyredz.roundwood.appearance;

import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.wood;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertNoDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropReport;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.rel;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertNoTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;

/**
 * Every way a door, trim or window leaves the world yields exactly its one family item carrying its
 * appearance (or nothing where vanilla gives nothing), with camouflage refunded separately and
 * exactly once. Each test uses a look no other test drops, so neighbouring tests are never counted.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AppearanceDropGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private AppearanceDropGameTests() {
    }

    private static DoorLayout.Assembly door(GameTestHelper helper, Appearance look, String name) {
        ServerPlayer placer = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, styled(ModBlocks.roundDoor(), look), ANCHOR, DoorHingeSide.LEFT).consumesAction(),
                name + ": door placement failed");
        DoorLayout.Assembly door = assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT);
        clearDrops(helper);
        return door;
    }

    private static TrimLayout.Assembly trim(GameTestHelper helper, Appearance look, String name) {
        ServerPlayer placer = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, placer, styled(ModBlocks.roundTrim(), look), ANCHOR).consumesAction(), name + ": trim placement failed");
        clearDrops(helper);
        return trimAssembly(helper, ANCHOR, Direction.NORTH);
    }

    private static int doors(GameTestHelper helper, Appearance look) {
        return dropped(helper, ModItems.ROUND_DOOR.get(), look);
    }

    private static int trims(GameTestHelper helper, Appearance look) {
        return dropped(helper, ModItems.ROUND_TRIM.get(), look);
    }

    /** A survival break of any part, the anchor included, drops one door in its look plus the paid camouflage once. */
    @GameTest(template = TEMPLATE)
    public static void survivalHarvestOfAnyDoorPartKeepsTheLook(GameTestHelper helper) {
        floor(helper);
        Appearance look = wood("jungle_planks").painted(DyeColor.PINK);
        for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
            DoorLayout.Assembly door = door(helper, look, "harvest_" + part);
            anchorEntity(helper, door).setCamouflage(CamouflageData.paid(Blocks.CALCITE.defaultBlockState(), new ItemStack(Items.CALCITE)));
            ServerPlayer breaker = player(helper, "harvest_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(door.pos(part)), part + ": break refused");
            assertNoDoor(helper, door);
            check(doors(helper, look) == 1, part + ": expected one styled door, got " + dropReport(helper, ModItems.ROUND_DOOR.get(), look));
            check(dropped(helper, Items.CALCITE) == 1, part + ": camouflage refunded " + dropped(helper, Items.CALCITE) + " times");
            check(dropped(helper, ModItems.ROUND_DOOR.get()) == 1, part + ": an extra (unstyled) door dropped");
            clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void survivalHarvestOfAnyTrimQuarterKeepsTheLook(GameTestHelper helper) {
        floor(helper);
        Appearance look = Appearance.masonry(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "deepslate_bricks"));
        for (Quarter part : Quarter.values()) {
            TrimLayout.Assembly trim = trim(helper, look, "trim_harvest_" + part);
            ServerPlayer breaker = player(helper, "trim_harvest_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(trim.pos(part)), part + ": break refused");
            assertNoTrim(helper, trim);
            check(trims(helper, look) == 1, part + ": expected one styled trim, got " + dropReport(helper, ModItems.ROUND_TRIM.get(), look));
            clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void windowHarvestAndExplosionKeepTheLook(GameTestHelper helper) {
        floor(helper);
        Appearance look = wood("acacia_planks").painted(DyeColor.LIME);
        BlockPos rel = new BlockPos(4, 1, 4);
        BlockPos abs = helper.absolutePos(rel);
        helper.setBlock(rel, ModBlocks.roundWindow().defaultBlockState());
        ((io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity) helper.getLevel().getBlockEntity(abs)).setAppearance(look);
        clearDrops(helper);
        ServerPlayer breaker = player(helper, "window_breaker", GameType.SURVIVAL, Direction.NORTH);
        check(breaker.gameMode.destroyBlock(abs), "window break refused");
        check(dropped(helper, ModItems.ROUND_WINDOW.get(), look) == 1, "expected one styled window, got "
                + dropReport(helper, ModItems.ROUND_WINDOW.get(), look));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void supportLossDropsOneStyledDoor(GameTestHelper helper) {
        floor(helper);
        Appearance look = wood("mangrove_planks");
        DoorLayout.Assembly door = door(helper, look, "support");
        helper.setBlock(rel(helper, door.pos(DoorLayout.Part.LOWER_RIGHT)).below(), Blocks.AIR);
        assertNoDoor(helper, door);
        check(doors(helper, look) == 1, "support loss dropped " + dropReport(helper, ModItems.ROUND_DOOR.get(), look));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void explosionYieldsAtMostOneStyledDoor(GameTestHelper helper) {
        floor(helper);
        Appearance look = wood("cherry_planks");
        DoorLayout.Assembly door = door(helper, look, "explosion");
        BlockPos centre = door.pos(DoorLayout.Part.UPPER_RIGHT);
        helper.getLevel().explode(null, centre.getX(), centre.getY(), centre.getZ() + 0.5, 2F, Level.ExplosionInteraction.TNT);
        assertNoDoor(helper, door);
        check(dropped(helper, ModItems.ROUND_DOOR.get()) == doors(helper, look), "an explosion dropped a door without its look");
        check(doors(helper, look) <= 1, "explosion dropped " + dropReport(helper, ModItems.ROUND_DOOR.get(), look));
        helper.succeed();
    }

    /** An incomplete door found by the integrity check is cleared with its loot read from the anchor. */
    @GameTest(template = TEMPLATE)
    public static void integrityCleanupDropsAtMostOneStyledDoor(GameTestHelper helper) {
        floor(helper);
        Appearance look = wood("bamboo_planks").painted(DyeColor.MAGENTA);
        DoorLayout.Assembly door = assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT);
        for (DoorLayout.Part part : List.of(DoorLayout.Part.LOWER_LEFT, DoorLayout.Part.UPPER_LEFT, DoorLayout.Part.LOWER_RIGHT)) {
            helper.setBlock(rel(helper, door.pos(part)), door.stateFor(part, ModBlocks.roundDoor(), false, false));
        }
        anchorEntity(helper, door).setAppearance(look);
        clearDrops(helper);
        helper.runAfterDelay(3, () -> {
            assertNoDoor(helper, door);
            check(doors(helper, look) <= 1, "cleanup dropped " + dropReport(helper, ModItems.ROUND_DOOR.get(), look));
            check(dropped(helper, ModItems.ROUND_DOOR.get()) == doors(helper, look), "cleanup dropped a door without its look");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void creativeAndDisabledTileDropsDropNothing(GameTestHelper helper) {
        floor(helper);
        Appearance look = wood("crimson_planks");
        DoorLayout.Assembly door = door(helper, look, "creative");
        ServerPlayer creative = player(helper, "creative_breaker", GameType.CREATIVE, Direction.NORTH);
        check(creative.gameMode.destroyBlock(door.pos(DoorLayout.Part.UPPER_RIGHT)), "creative break refused");
        assertNoDoor(helper, door);
        check(doors(helper, look) == 0, "creative break dropped a door");

        ServerLevel level = helper.getLevel();
        GameRules.BooleanValue tileDrops = level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        boolean before = tileDrops.get();
        tileDrops.set(false, level.getServer());
        try {
            door = door(helper, look, "no_tile_drops");
            ServerPlayer survival = player(helper, "no_tile_drops_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(survival.gameMode.destroyBlock(door.pos(DoorLayout.Part.LOWER_RIGHT)), "break refused");
            assertNoDoor(helper, door);
            check(doors(helper, look) == 0, "doTileDrops=false still dropped a door");
        } finally {
            tileDrops.set(before, level.getServer());
        }
        helper.succeed();
    }

    /** Loot is only decorated: asking twice gives the same styled stack and changes nothing in the world. */
    @GameTest(template = TEMPLATE)
    public static void repeatedLootCallbacksAreSideEffectFree(GameTestHelper helper) {
        floor(helper);
        Appearance look = wood("dark_oak_planks");
        DoorLayout.Assembly door = door(helper, look, "repeat");
        BlockPos part = door.pos(DoorLayout.Part.UPPER_LEFT);
        BlockState state = helper.getLevel().getBlockState(part);
        List<ItemStack> first = Block.getDrops(state, helper.getLevel(), part, null);
        List<ItemStack> second = Block.getDrops(state, helper.getLevel(), part, null);
        check(first.size() == 1 && second.size() == 1 && AppearanceStacks.read(first.get(0)).equals(look)
                && ItemStack.isSameItemSameTags(first.get(0), second.get(0)), "loot differs: " + first + " / " + second);
        check(anchorEntity(helper, door).appearance().equals(look), "computing loot changed the door");
        helper.succeed();
    }

    /** A harvest whose callback never comes (another mod's break path) still removes the part, at the end of the tick. */
    @GameTest(template = TEMPLATE)
    public static void aDeferredHarvestIsAlwaysCompleted(GameTestHelper helper) {
        floor(helper);
        Appearance look = wood("birch_planks").painted(DyeColor.BROWN);
        DoorLayout.Assembly door = door(helper, look, "deferred");
        BlockPos part = door.pos(DoorLayout.Part.LOWER_RIGHT);
        ServerPlayer player = player(helper, "deferred_breaker", GameType.SURVIVAL, Direction.NORTH);
        BlockState state = helper.getLevel().getBlockState(part);
        check(state.onDestroyedByPlayer(helper.getLevel(), part, player, true, helper.getLevel().getFluidState(part)),
                "the break was not accepted");
        check(helper.getLevel().getBlockState(part).is(ModBlocks.roundDoor()), "a harvesting break removed the part before its loot");
        helper.runAfterDelay(2, () -> {
            assertNoDoor(helper, door);
            helper.succeed();
        });
    }
}
