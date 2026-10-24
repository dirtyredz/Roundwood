package io.github.dirtyredz.roundwood.block.door.placement;

import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.fixture.PlacementCanceller;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertNoDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertOneAnchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.useItemOn;

/**
 * Placing the 1 x 2 arched door (0.11.0) through the real item: both cells or nothing, the clicked
 * cell the lower anchor, the hinge judged at the one-column footprint's real edges, every refusal
 * charging nothing, a cancelled placement rolled back, and the placement plan the client previews
 * equal to what the server writes (for the round door too).
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchedDoorPlacementGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private ArchedDoorPlacementGameTests() {
    }

    private static AssemblyDoorBlock arched() {
        return ModBlocks.archedDoor();
    }

    private static DoorLayout.Assembly arched(GameTestHelper helper, BlockPos anchor, Direction facing, DoorHingeSide hinge) {
        return assembly(helper, anchor, facing, hinge, DoorDesign.ARCHED);
    }

    @GameTest(template = TEMPLATE)
    public static void placesTwoCellsInEveryFacingAndHinge(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                String name = facing + "/" + hinge;
                ServerPlayer player = player(helper, "arched_" + facing + "_" + hinge, GameType.SURVIVAL, facing);
                ItemStack stack = new ItemStack(arched().asItem(), 2);
                check(placeDoor(helper, player, stack, ANCHOR, hinge).consumesAction(), name + ": placement failed");
                check(stack.getCount() == 1, name + ": expected one item consumed, have " + stack.getCount());
                DoorLayout.Assembly door = arched(helper, ANCHOR, facing, hinge);
                assertAssembly(helper, door, arched(), false);
                check(door.positions().size() == 2, name + ": not two cells");
                BlockState lower = helper.getLevel().getBlockState(door.anchor());
                BlockState upper = helper.getLevel().getBlockState(door.anchor().above());
                check(lower.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && upper.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
                        name + ": halves");
                check(!lower.hasProperty(RoundDoorBlock.COLUMN), name + ": the arched door has a column property");
                BlockPos besides = door.anchor().relative(facing.getClockWise());
                check(helper.getLevel().getBlockState(besides).isAir() && helper.getLevel().getBlockState(besides.above()).isAir(),
                        name + ": something written where a second column would be");
                check(DoorLayout.of(door.anchor().above(), upper).equals(door), name + ": the upper half names another assembly");
                assertOneAnchorEntity(helper, door);
                helper.setBlock(ANCHOR, Blocks.AIR);
                assertNoDoor(helper, door);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void theHingeIsJudgedAtTheOneColumnFootprint(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "arched_edges", GameType.SURVIVAL, Direction.NORTH);
        // Facing north the door is the single column x = 3; its right edge borders x = 4.
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(new BlockPos(4, y, 3), Blocks.STONE);
        }
        check(placeDoor(helper, player, new ItemStack(arched().asItem()), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "right edge");
        check(helper.getBlockState(ANCHOR).getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT, "a wall on the right should give hinge RIGHT");
        helper.setBlock(ANCHOR, Blocks.AIR);
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(new BlockPos(4, y, 3), Blocks.AIR);
            helper.setBlock(new BlockPos(2, y, 3), Blocks.STONE);
        }
        check(placeDoor(helper, player, new ItemStack(arched().asItem()), ANCHOR, DoorHingeSide.RIGHT).consumesAction(), "left edge");
        check(helper.getBlockState(ANCHOR).getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT, "a wall on the left should give hinge LEFT");
        helper.setBlock(ANCHOR, Blocks.AIR);
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(new BlockPos(2, y, 3), Blocks.AIR);
        }
        // A pair opens from the middle: the second door, right of the first, hinges on its right.
        check(placeDoor(helper, player, new ItemStack(arched().asItem()), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "first of pair");
        check(placeDoor(helper, player, new ItemStack(arched().asItem()), ANCHOR.east(), DoorHingeSide.LEFT).consumesAction(), "second of pair");
        assertAssembly(helper, arched(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT), arched(), false);
        assertAssembly(helper, arched(helper, ANCHOR.east(), Direction.NORTH, DoorHingeSide.RIGHT), arched(), false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyBlockedCellRefusesWithoutCharge(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(ANCHOR.above(), Blocks.OAK_PLANKS);
        assertRefused(helper, "occupied_upper");
        check(helper.getBlockState(ANCHOR.above()).is(Blocks.OAK_PLANKS), "the occupied cell was changed");
        helper.setBlock(ANCHOR.above(), Blocks.AIR);

        helper.setBlock(ANCHOR.above(), Blocks.WATER);
        assertRefused(helper, "fluid");
        helper.setBlock(ANCHOR.above(), Blocks.AIR);

        var pig = helper.spawn(EntityType.PIG, ANCHOR.above());
        assertRefused(helper, "entity");
        pig.discard();

        // Unsupported: click the side of a block so the anchor stands over a hole.
        helper.setBlock(new BlockPos(5, 0, 5), Blocks.AIR);
        helper.setBlock(new BlockPos(5, 1, 6), Blocks.STONE);
        ServerPlayer player = player(helper, "arched_support", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(arched().asItem());
        InteractionResult result = useItemOn(helper, player, stack, new BlockPos(5, 1, 6), Direction.NORTH);
        check(!result.consumesAction() && stack.getCount() == 1, "placed over a missing floor: " + result);
        assertNoDoor(helper, arched(helper, new BlockPos(5, 1, 5), Direction.NORTH, DoorHingeSide.LEFT));

        // A block where a round door's right column would stand does not matter to the narrow door.
        helper.setBlock(ANCHOR.east(), Blocks.OAK_PLANKS);
        helper.setBlock(ANCHOR.east().above(), Blocks.OAK_PLANKS);
        ServerPlayer narrow = player(helper, "arched_narrow", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, narrow, new ItemStack(arched().asItem()), ANCHOR, DoorHingeSide.LEFT).consumesAction(),
                "a full block beside the door refused the one-column placement");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aCancelledPlacementRollsBackBothCellsAndTheItem(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(ANCHOR.above(), Blocks.GRASS); // replaceable, so it is written and must come back
        int eventsBefore = PlacementCanceller.multiPlaceEvents();
        ServerPlayer player = player(helper, "cancelled", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(arched().asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        useItemOn(helper, player, stack, ANCHOR.below(), Direction.UP);
        check(PlacementCanceller.multiPlaceEvents() == eventsBefore + 1, "expected exactly one multi-place event");
        check(player.getMainHandItem().getCount() == 1, "item not restored: " + player.getMainHandItem());
        assertNoDoor(helper, arched(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT));
        check(helper.getBlockState(ANCHOR.above()).is(Blocks.GRASS), "the replaced grass was not restored");
        helper.succeed();
    }

    /** The plan the client previews is exactly what the server writes, for both designs, every facing and hinge. */
    @GameTest(template = TEMPLATE)
    public static void thePreviewedPlanIsWhatIsPlaced(GameTestHelper helper) {
        floor(helper);
        for (AssemblyDoorBlock door : ModBlocks.doors()) {
            for (Direction facing : FACINGS) {
                for (DoorHingeSide hinge : DoorHingeSide.values()) {
                    String name = door.design() + "/" + facing + "/" + hinge;
                    ServerPlayer player = player(helper, "plan_" + door.design() + "_" + facing + "_" + hinge, GameType.SURVIVAL, facing);
                    ItemStack stack = new ItemStack(door.asItem());
                    BlockHitResult hit = floorHit(helper, player, hinge);
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    Optional<AssemblyPlacementPlan<DoorLayout.Part, DoorLayout.Assembly>> plan = DoorPlacement.of(door,
                            new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)));
                    check(plan.isPresent(), name + ": no plan for a free spot");
                    check(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), name + ": placement failed");
                    DoorLayout.Assembly planned = plan.get().assembly();
                    check(planned.anchor().equals(helper.absolutePos(ANCHOR)) && planned.hinge() == hinge && planned.design() == door.design(),
                            name + ": planned " + planned);
                    check(planned.parts().size() == door.design().columns() * 2, name + ": planned parts");
                    for (DoorLayout.Part part : planned.parts()) {
                        BlockState placed = helper.getLevel().getBlockState(planned.pos(part));
                        check(placed.equals(plan.get().states().get(part)), name + ": " + part + " placed " + placed
                                + ", planned " + plan.get().states().get(part));
                    }
                    check(DoorLayout.of(planned.anchor(), helper.getLevel().getBlockState(planned.anchor())).equals(planned),
                            name + ": the placed door is another assembly");
                    helper.setBlock(ANCHOR, Blocks.AIR);
                    assertNoDoor(helper, planned);
                }
            }
        }
        // A refused spot has no plan and places nothing.
        helper.setBlock(ANCHOR.above(), Blocks.STONE);
        ServerPlayer player = player(helper, "plan_refused", GameType.SURVIVAL, Direction.NORTH);
        BlockHitResult hit = floorHit(helper, player, DoorHingeSide.LEFT);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(arched().asItem()));
        check(DoorPlacement.of(arched(), new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit))).isEmpty(),
                "a plan for a blocked spot");
        helper.succeed();
    }

    /** A lone half (a structure, a command) is not a door: integrity removes it instead of inventing a partner. */
    @GameTest(template = TEMPLATE)
    public static void aLoneHalfIsRemovedNotCompleted(GameTestHelper helper) {
        floor(helper);
        BlockState lower = arched().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        helper.getLevel().setBlock(helper.absolutePos(ANCHOR), lower, Block.UPDATE_ALL);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(5, 2, 3)), upper, Block.UPDATE_ALL);
        helper.runAfterDelay(3, () -> {
            check(!helper.getBlockState(ANCHOR).is(arched()), "a lone lower half stayed");
            check(!helper.getBlockState(new BlockPos(5, 2, 3)).is(arched()), "a lone upper half stayed");
            check(helper.getBlockState(ANCHOR.above()).isAir() && helper.getBlockState(new BlockPos(5, 1, 3)).isAir(),
                    "a partner was fabricated");
            helper.succeed();
        });
    }

    private static void assertRefused(GameTestHelper helper, String name) {
        ServerPlayer player = player(helper, "arched_" + name, GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(arched().asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult result = useItemOn(helper, player, stack, ANCHOR.below(), Direction.UP);
        check(!result.consumesAction(), name + ": placement should have been refused, got " + result);
        check(stack.getCount() == 1, name + ": item consumed on refusal");
        check(!helper.getBlockState(ANCHOR).is(arched()), name + ": a part was written");
    }

    /** The click {@code DoorFixtures.placeDoor} makes on the floor under {@link #ANCHOR} for {@code hinge}. */
    private static BlockHitResult floorHit(GameTestHelper helper, ServerPlayer player, DoorHingeSide hinge) {
        Direction across = player.getDirection().getClockWise();
        double along = hinge == DoorHingeSide.LEFT ? 0.25 : 0.75;
        BlockPos floor = helper.absolutePos(ANCHOR.below());
        Vec3 hit = Vec3.atCenterOf(floor).add(0, 0.5, 0)
                .add(across.getStepX() * (along - 0.5), 0, across.getStepZ() * (along - 0.5));
        return new BlockHitResult(hit, Direction.UP, floor, false);
    }
}
