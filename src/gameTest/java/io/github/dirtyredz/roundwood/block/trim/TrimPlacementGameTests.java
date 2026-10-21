package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import io.github.dirtyredz.roundwood.fixture.PlacementCanceller;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.STRIP;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertNoTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.brickTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fitToDoor;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fittedTo;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.oakTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;

/**
 * Placing whole frames through real server item use: freestanding and fitted to a door in every
 * facing and from every clicked quarter, the support, cell, fluid, limit and permission checks,
 * exact consumption, and Forge's multi-place cancellation rolling everything back.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrimPlacementGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final double[][] QUARTERS = {{0.5, 0.5}, {1.5, 0.5}, {0.5, 1.5}, {1.5, 1.5}};

    private TrimPlacementGameTests() {
    }

    private static ItemStack two(RoundTrimBlock block) {
        return new ItemStack(block.asItem(), 2);
    }

    @GameTest(template = TEMPLATE)
    public static void freestandingFramesFaceAwayFromThePlayerInEveryFacing(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            ServerPlayer player = player(helper, "free_" + facing, GameType.SURVIVAL, facing);
            ItemStack stack = two(oakTrim());
            check(placeFreestanding(helper, player, stack, ANCHOR).consumesAction(), facing + ": placement refused");
            TrimLayout.Assembly assembly = trimAssembly(helper, ANCHOR, facing);
            assertTrim(helper, assembly, oakTrim());
            check(stack.getCount() == 1, facing + ": expected exactly one item consumed, have " + stack.getCount());
            helper.setBlock(ANCHOR, Blocks.AIR);
            assertNoTrim(helper, assembly);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sneakUseOnAClosedDoorFitsTheFrameFromAnyQuarter(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            ServerPlayer placer = player(helper, "fit_door_" + facing, GameType.SURVIVAL, facing);
            check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), facing + ": door");
            DoorLayout.Assembly door = assembly(helper, ANCHOR, facing, DoorHingeSide.LEFT);
            for (double[] q : QUARTERS) {
                String name = facing + " quarter " + q[0] + "," + q[1];
                ServerPlayer player = player(helper, "fitter", GameType.SURVIVAL, facing);
                ItemStack stack = two(brickTrim());
                InteractionResult result = fitToDoor(helper, player, stack, door, q[0], q[1]);
                check(result.consumesAction(), name + ": fitting refused: " + result);
                TrimLayout.Assembly fitted = fittedTo(door);
                assertTrim(helper, fitted, brickTrim());
                check(stack.getCount() == 1, name + ": expected one item consumed, have " + stack.getCount());
                assertAssembly(helper, door, oakDoor(), false);
                helper.getLevel().setBlockAndUpdate(fitted.anchor(), Blocks.AIR.defaultBlockState());
                assertNoTrim(helper, fitted);
            }
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missingSupportUnderEitherLowerQuarterRefuses(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = trimAssembly(helper, ANCHOR, Direction.NORTH);
        // Under the left quarter (nothing to click there, so through the command path).
        helper.setBlock(ANCHOR.below(), Blocks.AIR);
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), assembly), "placed with no floor under the left quarter");
        assertNoTrim(helper, assembly);
        helper.setBlock(ANCHOR.below(), Blocks.STONE);
        // Under the right quarter, through real item use.
        helper.setBlock(ANCHOR.below().east(), Blocks.AIR);
        ServerPlayer player = player(helper, "support", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = two(oakTrim());
        check(!placeFreestanding(helper, player, stack, ANCHOR).consumesAction() && stack.getCount() == 2,
                "placed with no floor under the right quarter");
        assertNoTrim(helper, assembly);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void occupiedWetAndDoorCellsRefuseWithoutChanges(GameTestHelper helper) {
        floor(helper);
        BlockPos upperRight = ANCHOR.above().east();
        Object[][] blockers = {{Blocks.STONE, "stone"}, {Blocks.WATER, "water"}};
        for (Object[] blocker : blockers) {
            helper.setBlock(upperRight, (net.minecraft.world.level.block.Block) blocker[0]);
            ServerPlayer player = player(helper, "occupied_" + blocker[1], GameType.SURVIVAL, Direction.NORTH);
            ItemStack stack = two(oakTrim());
            check(!placeFreestanding(helper, player, stack, ANCHOR).consumesAction() && stack.getCount() == 2,
                    blocker[1] + ": placed into a blocked cell");
            assertNoTrim(helper, trimAssembly(helper, ANCHOR, Direction.NORTH));
            helper.setBlock(upperRight, Blocks.AIR);
        }
        // A frame never overwrites a door: aiming so the frame would cover the door's cells refuses.
        ServerPlayer placer = player(helper, "door_cells", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR.east(), DoorHingeSide.LEFT).consumesAction(), "door");
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), trimAssembly(helper, ANCHOR, Direction.NORTH)),
                "a frame was placed over door cells");
        assertAssembly(helper, assembly(helper, ANCHOR.east(), Direction.NORTH, DoorHingeSide.LEFT), oakDoor(), false);
        helper.succeed();
    }

    /** The ring never collides, so a mob standing in the aperture does not stop placement. */
    @GameTest(template = TEMPLATE)
    public static void entitiesInTheApertureDoNotBlock(GameTestHelper helper) {
        floor(helper);
        helper.spawn(EntityType.PIG, ANCHOR.east());
        ServerPlayer player = player(helper, "entity", GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, player, two(oakTrim()), ANCHOR).consumesAction(), "a mob in the aperture blocked the frame");
        assertTrim(helper, trimAssembly(helper, ANCHOR, Direction.NORTH), oakTrim());
        helper.getLevel().getEntities(EntityType.PIG, e -> true).forEach(e -> e.discard());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void limitsPermissionsAndUnloadedCellsRefuse(GameTestHelper helper) {
        floor(helper);
        // Adventure players cannot place.
        ServerPlayer adventurer = player(helper, "adventure", GameType.ADVENTURE, Direction.NORTH);
        ItemStack stack = two(oakTrim());
        check(!placeFreestanding(helper, adventurer, stack, ANCHOR).consumesAction() && stack.getCount() == 2, "adventure placed");
        assertNoTrim(helper, trimAssembly(helper, ANCHOR, Direction.NORTH));
        // The top of the world: the upper quarters would be outside the build height.
        BlockPos base = helper.absolutePos(ANCHOR);
        int top = helper.getLevel().getMaxBuildHeight() - 1;
        TrimLayout.Assembly ceiling = new TrimLayout.Assembly(new BlockPos(base.getX(), top, base.getZ()), Direction.NORTH);
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), ceiling), "placed through the build height");
        // Far away: unloaded cells are never written (or loaded).
        TrimLayout.Assembly far = new TrimLayout.Assembly(base.offset(1_000_000, 0, 1_000_000), Direction.NORTH);
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), far), "placed into unloaded chunks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancelledPlacementRollsBackAllCellsAndTheItem(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(ANCHOR.east().below(), Blocks.DIRT);
        helper.setBlock(ANCHOR.east(), Blocks.GRASS); // a replaceable cell must come back too
        int eventsBefore = PlacementCanceller.multiPlaceEvents();
        ServerPlayer player = player(helper, "cancelled", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(oakTrim().asItem());
        placeFreestanding(helper, player, stack, ANCHOR);
        check(PlacementCanceller.multiPlaceEvents() == eventsBefore + 1, "expected exactly one multi-place event");
        check(player.getMainHandItem().getCount() == 1, "item not restored: " + player.getMainHandItem());
        assertNoTrim(helper, trimAssembly(helper, ANCHOR, Direction.NORTH));
        check(helper.getBlockState(ANCHOR.east()).is(Blocks.GRASS), "replaced grass not restored");
        helper.succeed();
    }

    @GameTest(template = STRIP)
    public static void placesAcrossAChunkBoundary(GameTestHelper helper) {
        floor(helper, 17, 4);
        for (int x = 0; x < 16; x++) {
            BlockPos anchor = new BlockPos(x, 1, 1);
            TrimLayout.Assembly assembly = trimAssembly(helper, anchor, Direction.NORTH);
            int left = assembly.anchor().getX();
            if ((left >> 4) != ((left + 1) >> 4)) {
                ServerPlayer player = player(helper, "chunk", GameType.SURVIVAL, Direction.NORTH);
                check(placeFreestanding(helper, player, two(oakTrim()), anchor).consumesAction(), "chunk-boundary placement refused");
                assertTrim(helper, assembly, oakTrim());
                helper.succeed();
                return;
            }
        }
        helper.fail("the strip template does not span a chunk boundary");
    }
}
