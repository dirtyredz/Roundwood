package io.github.dirtyredz.roundwood.block.window.placement;

import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.PlacementCanceller;
import io.github.dirtyredz.roundwood.fixture.window.WindowFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;

/**
 * Targeted placement through the real item path: every face and zone of a block, replaceable,
 * partial and occupied targets, entities, water, cancellation and survival use. Each placement is
 * compared with what the preview's path predicted for the same hit.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowPlacementGameTests {
    /** Independent of the mod: the world side each edge zone names, per face ({U_MIN, U_MAX, V_MIN, V_MAX}). */
    private static final Map<Direction, Direction[]> EDGES = Map.of(
            Direction.UP, new Direction[] {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
            Direction.DOWN, new Direction[] {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
            Direction.NORTH, new Direction[] {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
            Direction.SOUTH, new Direction[] {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
            Direction.EAST, new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP},
            Direction.WEST, new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP});

    private WindowPlacementGameTests() {
    }

    private static ItemStack window() {
        return new ItemStack(ModBlocks.roundWindow());
    }

    private static WindowPosition expected(Direction face, WindowTargetZones.Zone zone) {
        return switch (zone) {
            case CENTER -> WindowPosition.CENTER;
            case U_MIN -> WindowPosition.flush(EDGES.get(face)[0]);
            case U_MAX -> WindowPosition.flush(EDGES.get(face)[1]);
            case V_MIN -> WindowPosition.flush(EDGES.get(face)[2]);
            case V_MAX -> WindowPosition.flush(EDGES.get(face)[3]);
        };
    }

    /** Places and checks: the predicted state is what is written, at the expected cell and position. */
    private static BlockState placeAndCompare(GameTestHelper helper, ServerPlayer player, BlockHitResult hit, BlockPos destination,
                                              WindowPosition position, String what) {
        BlockState predicted = WindowFixtures.predict(player, window(), hit);
        check(predicted != null, what + ": the preview path predicted nothing");
        InteractionResult result = WindowFixtures.place(player, window(), hit);
        check(result.consumesAction(), what + ": placement failed (" + result + ")");
        BlockState placed = helper.getBlockState(destination);
        check(placed.is(ModBlocks.roundWindow()), what + ": no window at " + destination + " (" + placed + ")");
        check(placed.getValue(RoundWindowBlock.POSITION) == position, what + ": position " + placed.getValue(RoundWindowBlock.POSITION)
                + ", expected " + position);
        check(placed.equals(predicted), what + ": placed " + placed + " but the preview predicted " + predicted);
        return placed;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void everyFaceAndZoneOfABlockPlacesWhereTheOverlaySays(GameTestHelper helper) {
        BlockPos target = new BlockPos(4, 3, 4);
        helper.setBlock(target, Blocks.STONE);
        ServerPlayer player = player(helper, "zones", GameType.CREATIVE, Direction.NORTH);
        for (Direction face : Direction.values()) {
            for (WindowTargetZones.Zone zone : WindowTargetZones.Zone.values()) {
                BlockPos destination = target.relative(face);
                String what = face + " " + zone;
                placeAndCompare(helper, player, WindowFixtures.zone(helper, target, face, zone), destination, expected(face, zone), what);
                helper.setBlock(destination, Blocks.AIR);
            }
        }
        // The centre keeps the 0.5.0 rule: the plane runs across the player's line of sight.
        ServerPlayer east = player(helper, "zones_east", GameType.CREATIVE, Direction.EAST);
        BlockState centred = placeAndCompare(helper, east, WindowFixtures.zone(helper, target, Direction.UP, WindowTargetZones.Zone.CENTER),
                target.above(), WindowPosition.CENTER, "centre facing east");
        check(centred.getValue(RoundWindowBlock.AXIS) == Direction.Axis.Z, "facing east should give axis z");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void zoneBoundariesAreTheCentreSquaresAndTiesGoToTheUEdge(GameTestHelper helper) {
        BlockPos target = new BlockPos(4, 3, 4);
        helper.setBlock(target, Blocks.STONE);
        ServerPlayer player = player(helper, "bounds", GameType.CREATIVE, Direction.NORTH);
        double[][] points = {{0.25, 0.25}, {0.75, 0.5}, {0.2499, 0.5}, {0.1, 0.1}, {0.875, 0.875}, {1.0, 0.0}};
        WindowPosition[] positions = {WindowPosition.CENTER, WindowPosition.CENTER, WindowPosition.WEST, WindowPosition.WEST,
                WindowPosition.EAST, WindowPosition.EAST};
        for (int i = 0; i < points.length; i++) {
            BlockHitResult hit = WindowFixtures.hit(helper, target, Direction.UP, points[i][0], points[i][1]);
            placeAndCompare(helper, player, hit, target.above(), positions[i], "top face at " + points[i][0] + "," + points[i][1]);
            helper.setBlock(target.above(), Blocks.AIR);
        }
        helper.succeed();
    }

    /** A replaceable block is replaced, and the zone is read on its own cell; a slab's top and stairs' side use theirs. */
    @GameTest(template = TEMPLATE)
    public static void replaceableAndPartialTargets(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "partial", GameType.CREATIVE, Direction.NORTH);
        BlockPos grass = new BlockPos(1, 1, 1);
        helper.setBlock(grass, Blocks.GRASS);
        placeAndCompare(helper, player, WindowFixtures.hit(helper, grass, Direction.UP, 0.5, 0.05), grass, WindowPosition.NORTH,
                "tall grass");
        BlockPos slab = new BlockPos(4, 1, 1);
        helper.setBlock(slab, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        placeAndCompare(helper, player, WindowFixtures.hit(helper, slab, Direction.UP, 0.95, 0.5, 0.5), slab.above(),
                WindowPosition.EAST, "slab top near the east edge");
        BlockPos stairs = new BlockPos(7, 1, 4);
        helper.setBlock(stairs, Blocks.OAK_STAIRS);
        placeAndCompare(helper, player, WindowFixtures.hit(helper, stairs, Direction.NORTH, 0.5, 0.9), stairs.north(),
                WindowPosition.UP, "stairs side near the top");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void occupiedCellsAndMobsRefuseAndKeepTheItem(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "refused", GameType.SURVIVAL, Direction.NORTH);
        BlockPos wall = new BlockPos(2, 1, 2);
        helper.setBlock(wall, Blocks.STONE);
        helper.setBlock(wall.north(), Blocks.STONE);
        ItemStack stack = window();
        InteractionResult occupied = WindowFixtures.place(player, stack, WindowFixtures.zone(helper, wall, Direction.NORTH,
                WindowTargetZones.Zone.U_MIN));
        check(!occupied.consumesAction() && stack.getCount() == 1, "placed into an occupied cell: " + occupied);
        BlockPos open = new BlockPos(6, 1, 6);
        Pig pig = helper.spawn(EntityType.PIG, open);
        pig.setNoAi(true);
        ItemStack second = window();
        InteractionResult blocked = WindowFixtures.place(player, second, WindowFixtures.zone(helper, open.below(), Direction.UP,
                WindowTargetZones.Zone.V_MIN));
        check(!blocked.consumesAction() && second.getCount() == 1 && helper.getBlockState(open).isAir(),
                "a flush window was placed through a pig: " + blocked);
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void waterWaterlogsEveryPosition(GameTestHelper helper) {
        BlockPos target = new BlockPos(4, 3, 4);
        helper.setBlock(target, Blocks.STONE);
        ServerPlayer player = player(helper, "water", GameType.CREATIVE, Direction.NORTH);
        for (WindowTargetZones.Zone zone : WindowTargetZones.Zone.values()) {
            BlockPos destination = target.above();
            helper.setBlock(destination, Blocks.WATER);
            BlockState placed = placeAndCompare(helper, player, WindowFixtures.zone(helper, target, Direction.UP, zone), destination,
                    expected(Direction.UP, zone), "water " + zone);
            check(placed.getValue(BlockStateProperties.WATERLOGGED), zone + ": not waterlogged");
            helper.setBlock(destination, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void survivalUsesOneAndACancelledPlacementRollsBack(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "survival", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(ModBlocks.roundWindow(), 2);
        BlockPos at = new BlockPos(2, 1, 2);
        check(WindowFixtures.place(player, stack, WindowFixtures.zone(helper, at.below(), Direction.UP, WindowTargetZones.Zone.U_MAX))
                .consumesAction(), "survival placement failed");
        check(stack.getCount() == 1, "survival placement used " + (2 - stack.getCount()));
        check(helper.getBlockState(at).getValue(RoundWindowBlock.POSITION) == WindowPosition.EAST, "survival placement position");
        ServerPlayer cancelled = player(helper, PlacementCanceller.PLAYER.substring("rw_".length()), GameType.SURVIVAL, Direction.NORTH);
        ItemStack kept = window();
        BlockPos denied = new BlockPos(6, 1, 6);
        WindowFixtures.place(cancelled, kept, WindowFixtures.zone(helper, denied.below(), Direction.UP, WindowTargetZones.Zone.V_MIN));
        check(helper.getBlockState(denied).isAir(), "a cancelled placement left a window");
        check(helper.getLevel().getBlockEntity(helper.absolutePos(denied)) == null, "a cancelled placement left its block entity");
        check(kept.getCount() == 1, "a cancelled placement used the item");
        helper.succeed();
    }

    /** Sneaking changes nothing: the real use path with shift held places the same position. */
    @GameTest(template = TEMPLATE)
    public static void sneakingDoesNotChangeThePosition(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "sneak", GameType.CREATIVE, Direction.NORTH);
        BlockPos at = new BlockPos(3, 1, 3);
        BlockHitResult hit = WindowFixtures.zone(helper, at.below(), Direction.UP, WindowTargetZones.Zone.V_MAX);
        BlockState predicted = WindowFixtures.predict(player, window(), hit);
        player.setItemInHand(InteractionHand.MAIN_HAND, window());
        check(AppearanceFixtures.use(helper, player, hit, true).consumesAction(), "sneak placement failed");
        check(helper.getBlockState(at).equals(predicted), "sneaking placed " + helper.getBlockState(at) + ", expected " + predicted);
        check(helper.getBlockState(at).getValue(RoundWindowBlock.POSITION) == WindowPosition.SOUTH, "sneak position");
        helper.succeed();
    }

    /** A neighbour placed later re-judges the connection exactly as placing next to it would have (a flush window: none). */
    @GameTest(template = TEMPLATE)
    public static void laterNeighboursMatchWhatPlacementWouldHavePredicted(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "later", GameType.CREATIVE, Direction.NORTH);
        BlockPos first = new BlockPos(2, 1, 2);
        BlockHitResult hit = WindowFixtures.zone(helper, first.below(), Direction.UP, WindowTargetZones.Zone.V_MIN);
        placeAndCompare(helper, player, hit, first, WindowPosition.NORTH, "first");
        WindowFixtures.place(player, new ItemStack(Items.GLASS_PANE), WindowFixtures.zone(helper, first.east().below(), Direction.UP,
                WindowTargetZones.Zone.CENTER));
        BlockState updated = helper.getBlockState(first);
        check(!updated.getValue(CrossCollisionBlock.EAST), "a pane placed later connected to a flush window");
        BlockPos second = new BlockPos(6, 1, 2);
        helper.setBlock(second.east(), Blocks.GLASS_PANE);
        BlockState next = placeAndCompare(helper, player, WindowFixtures.zone(helper, second.below(), Direction.UP,
                WindowTargetZones.Zone.V_MIN), second, WindowPosition.NORTH, "second");
        check(next.setValue(CrossCollisionBlock.WEST, false).equals(updated.setValue(CrossCollisionBlock.WEST, false)),
                "placing beside a pane gave " + next + ", a later pane gave " + updated);
        helper.succeed();
    }
}
