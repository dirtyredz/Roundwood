package io.github.dirtyredz.roundwood.block.door.placement;

import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceSamples;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorColumn;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.fixture.PlacementCanceller;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.STRIP;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertNoDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertOneAnchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.useItemOn;

/** Placing an assembly through the real item: all four parts or nothing. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorPlacementGameTests {
    private DoorPlacementGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void placesFourMatchingPartsInEveryFacing(GameTestHelper helper) {
        floor(helper);
        RoundDoorBlock door = oakDoor();
        List<Direction> facings = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        // Anchors chosen so the right column (clockwise of facing) stays inside the floor.
        List<BlockPos> anchors = List.of(new BlockPos(1, 1, 1), new BlockPos(6, 1, 1), new BlockPos(7, 1, 6), new BlockPos(2, 1, 7));
        for (int i = 0; i < 4; i++) {
            Direction facing = facings.get(i);
            ServerPlayer player = player(helper, "facing_" + i, GameType.SURVIVAL, facing);
            ItemStack stack = new ItemStack(door.asItem(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            InteractionResult result = useItemOn(helper, player, stack, anchors.get(i).below(), Direction.UP);
            check(result.consumesAction(), facing + ": placement failed: " + result);
            check(stack.getCount() == 1, facing + ": expected exactly one item consumed, have " + stack.getCount());
            assertAssembly(helper, assembly(helper, anchors.get(i), facing), door, false);
        }
        // Concretely, facing north the right column is one block east of the anchor.
        BlockState rightLower = helper.getBlockState(new BlockPos(2, 1, 1));
        check(rightLower.is(door) && rightLower.getValue(RoundDoorBlock.COLUMN) == DoorColumn.RIGHT
                && rightLower.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT
                && rightLower.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER, "north: right column not east of anchor");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyFacingAndHingeSharesOneHingeAndOneEntity(GameTestHelper helper) {
        floor(helper);
        RoundDoorBlock door = oakDoor();
        BlockPos anchor = new BlockPos(3, 1, 3);
        for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                String name = facing + "/" + hinge;
                ServerPlayer player = player(helper, "hinge_" + facing + "_" + hinge, GameType.SURVIVAL, facing);
                check(placeDoor(helper, player, door.asItem(), anchor, hinge).consumesAction(), name + ": placement failed");
                DoorLayout.Assembly assembly = assembly(helper, anchor, facing, hinge);
                assertAssembly(helper, assembly, door, false);
                for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
                    BlockState state = helper.getLevel().getBlockState(assembly.pos(part));
                    check(state.getValue(DoorBlock.HINGE) == hinge, name + ": hinge differs on " + part);
                    check(state.getValue(RoundDoorBlock.COLUMN) == part.column, name + ": wrong column on " + part);
                }
                check(assembly.pos(DoorLayout.Part.LOWER_RIGHT).equals(assembly.anchor().relative(facing.getClockWise())),
                        name + ": right column not clockwise of the anchor");
                assertOneAnchorEntity(helper, assembly);
                helper.setBlock(anchor, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fullBlocksJustOutsideAnEdgeDecideTheHinge(GameTestHelper helper) {
        floor(helper);
        BlockPos anchor = new BlockPos(3, 1, 3); // facing north: columns x = 3 (left) and 4 (right)
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(new BlockPos(5, y, 3), Blocks.STONE); // just outside the right edge
        }
        ServerPlayer player = player(helper, "edge_right", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), anchor).consumesAction(), "placement failed (right edge)");
        check(helper.getBlockState(anchor).getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT, "block outside the right edge should give hinge RIGHT");
        helper.setBlock(anchor, Blocks.AIR);
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(new BlockPos(5, y, 3), Blocks.AIR);
            helper.setBlock(new BlockPos(2, y, 3), Blocks.STONE); // just outside the left edge
        }
        check(placeDoor(helper, player, oakDoor().asItem(), anchor).consumesAction(), "placement failed (left edge)");
        check(helper.getBlockState(anchor).getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT, "block outside the left edge should give hinge LEFT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aNeighbouringRoundDoorMakesTheNewOneHingeAwayFromIt(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "neighbour", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), new BlockPos(1, 1, 3)).consumesAction(), "first placement failed");
        // The first door's right column (x = 2) is just outside the second door's left edge.
        check(placeDoor(helper, player, oakDoor().asItem(), new BlockPos(3, 1, 3)).consumesAction(), "second placement failed");
        assertAssembly(helper, assembly(helper, new BlockPos(1, 1, 3), Direction.NORTH, DoorHingeSide.LEFT), oakDoor(), false);
        assertAssembly(helper, assembly(helper, new BlockPos(3, 1, 3), Direction.NORTH, DoorHingeSide.RIGHT), oakDoor(), false);
        helper.succeed();
    }

    /** Every vanilla wood and every paint places the one door block, carrying the stack's look onto the anchor. */
    @GameTest(template = TEMPLATE)
    public static void everyLookPlacesAndTearsDown(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "looks", GameType.CREATIVE, Direction.NORTH);
        BlockPos anchor = new BlockPos(3, 1, 3);
        List<Appearance> looks = new ArrayList<>();
        AppearanceSamples.VANILLA_WOODS
                .forEach(id -> looks.add(Appearance.wood(id)));
        for (DyeColor dye : DyeColor.values()) {
            looks.add(Appearance.DEFAULT.painted(dye));
        }
        RoundDoorBlock door = oakDoor();
        for (Appearance look : looks) {
            check(placeDoor(helper, player, styled(door, look), anchor,
                    DoorHingeSide.LEFT).consumesAction(), look.key() + ": placement failed");
            DoorLayout.Assembly assembly = assembly(helper, anchor, Direction.NORTH);
            assertAssembly(helper, assembly, door, false);
            check(anchorEntity(helper, assembly).appearance().equals(look),
                    look.key() + ": the anchor does not carry the stack's look");
            helper.setBlock(anchor.above(), Blocks.AIR); // removing any part removes all
            assertNoDoor(helper, assembly);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void occupiedCellRefusesWithoutChanges(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.OAK_PLANKS); // where the upper-right part would go
        assertRefused(helper, "occupied", new BlockPos(1, 1, 1));
        check(helper.getBlockState(new BlockPos(2, 2, 1)).is(Blocks.OAK_PLANKS), "occupied cell was changed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missingSupportUnderEitherColumnRefuses(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(2, 0, 1), Blocks.AIR);
        assertRefused(helper, "support_right", new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.AIR);
        // Clicking the north side of a block so the anchor itself sits over the gap.
        helper.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);
        ServerPlayer player = player(helper, "support_left", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(oakDoor().asItem());
        InteractionResult result = useItemOn(helper, player, stack, new BlockPos(4, 1, 5), Direction.NORTH);
        check(!result.consumesAction() && stack.getCount() == 1, "placed over a missing floor: " + result);
        assertNoDoor(helper, assembly(helper, new BlockPos(4, 1, 4), Direction.NORTH));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void entityInACellRefuses(GameTestHelper helper) {
        floor(helper);
        var pig = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 1)); // mobs block building; armor stands do not
        assertRefused(helper, "entity", new BlockPos(1, 1, 1));
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fluidInACellRefuses(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.WATER);
        assertRefused(helper, "fluid", new BlockPos(1, 1, 1));
        check(helper.getBlockState(new BlockPos(2, 1, 1)).is(Blocks.WATER), "water was replaced");
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void buildHeightCeilingRefuses(GameTestHelper helper) {
        // One cell below the top of the world: the upper parts would be outside the build height.
        BlockPos base = helper.absolutePos(new BlockPos(1, 0, 1));
        int top = helper.getLevel().getMaxBuildHeight() - 1;
        BlockPos floorLeft = new BlockPos(base.getX(), top - 1, base.getZ());
        BlockPos floorRight = floorLeft.east();
        helper.getLevel().setBlockAndUpdate(floorLeft, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(floorRight, Blocks.STONE.defaultBlockState());
        try {
            ServerPlayer player = player(helper, "ceiling", GameType.SURVIVAL, Direction.NORTH);
            ItemStack stack = new ItemStack(oakDoor().asItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            InteractionResult result = stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floorLeft).add(0, 0.5, 0),
                            Direction.UP, floorLeft, false)));
            check(!result.consumesAction() && stack.getCount() == 1, "placed through the build height: " + result);
            check(helper.getLevel().getBlockState(floorLeft.above()).isAir(), "a part was written at the ceiling");
        } finally {
            helper.getLevel().setBlockAndUpdate(floorLeft, Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(floorRight, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancelledPlacementRollsBackAllCellsAndTheItem(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(2, 0, 1), Blocks.DIRT);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.GRASS); // a replaceable cell must come back too
        int eventsBefore = PlacementCanceller.multiPlaceEvents();
        ServerPlayer player = player(helper, "cancelled", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(oakDoor().asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        useItemOn(helper, player, stack, new BlockPos(1, 0, 1), Direction.UP);
        check(PlacementCanceller.multiPlaceEvents() == eventsBefore + 1, "expected exactly one multi-place event");
        check(player.getMainHandItem().getCount() == 1, "item not restored: " + player.getMainHandItem());
        assertNoDoor(helper, assembly(helper, new BlockPos(1, 1, 1), Direction.NORTH));
        check(helper.getBlockState(new BlockPos(2, 1, 1)).is(Blocks.GRASS), "replaced grass not restored");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void poweredPlacementOpensTheWholeAssembly(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.REDSTONE_BLOCK); // behind the upper-right cell
        ServerPlayer player = player(helper, "powered", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), new BlockPos(2, 1, 1)).consumesAction(), "placement failed");
        DoorLayout.Assembly assembly = assembly(helper, new BlockPos(2, 1, 1), Direction.NORTH);
        assertAssembly(helper, assembly, oakDoor(), true);
        for (BlockPos pos : assembly.positions()) {
            check(helper.getLevel().getBlockState(pos).getValue(DoorBlock.POWERED), "part not powered at " + pos);
        }
        helper.succeed();
    }

    @GameTest(template = STRIP)
    public static void placesAcrossAChunkBoundary(GameTestHelper helper) {
        floor(helper, 17, 4);
        // Find the relative x whose absolute x is the last column of a chunk; the right column is in the next chunk.
        int x = 0;
        while (((helper.absolutePos(new BlockPos(x, 0, 0)).getX()) & 15) != 15) {
            x++;
        }
        BlockPos anchor = new BlockPos(x, 1, 1);
        DoorLayout.Assembly assembly = assembly(helper, anchor, Direction.NORTH);
        check((assembly.pos(DoorLayout.Part.LOWER_LEFT).getX() >> 4) != (assembly.pos(DoorLayout.Part.LOWER_RIGHT).getX() >> 4),
                "test setup does not straddle chunks");
        ServerPlayer player = player(helper, "chunks", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), anchor).consumesAction(), "placement failed");
        assertAssembly(helper, assembly, oakDoor(), false);
        helper.succeed();
    }

    private static void assertRefused(GameTestHelper helper, String name, BlockPos anchor) {
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(oakDoor().asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult result = useItemOn(helper, player, stack, anchor.below(), Direction.UP);
        check(!result.consumesAction(), name + ": placement should have been refused, got " + result);
        check(stack.getCount() == 1, name + ": item consumed on refusal");
        assertNoDoor(helper, assembly(helper, anchor, Direction.NORTH));
    }
}
