package io.github.dirtyredz.roundwood.block.archway;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPlacementPlan;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayGeometry;
import io.github.dirtyredz.roundwood.fixture.PlacementCanceller;
import io.github.dirtyredz.roundwood.geometry.Box;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.assertArchway;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.assertNoArchway;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.block;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.place;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.useItemOn;

/**
 * Placing archways (0.12.0) through the real item with an ordinary click: four cells or nothing, the
 * clicked cell the lower-left anchor and the archway extending right and up, in every facing; every
 * refusal (occupied, wet, obstructed, unsupported, out of the world) charging nothing; someone in the
 * passage not blocking it; a cancelled placement rolled back with the item; sneaking changing nothing;
 * and the plan the client previews equal to what the server writes, its outline exactly the placed
 * cells' shapes.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchwayPlacementGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private ArchwayPlacementGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void placesFourCellsInEveryFacing(GameTestHelper helper) {
        floor(helper);
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (Direction facing : FACINGS) {
                String name = design + "/" + facing;
                ServerPlayer player = player(helper, "archway_" + design + "_" + facing, GameType.SURVIVAL, facing);
                ItemStack stack = new ItemStack(block(design).asItem(), 2);
                check(place(helper, player, stack, ANCHOR).consumesAction(), name + ": placement failed");
                check(stack.getCount() == 1, name + ": expected one item used, have " + stack.getCount());
                ArchwayLayout.Assembly archway = assembly(helper, ANCHOR, facing, design);
                assertArchway(helper, archway);
                check(archway.pos(ArchwayPart.LOWER_RIGHT).equals(archway.anchor().relative(facing.getClockWise())),
                        name + ": the right column is not on the placer's right");
                for (ArchwayPart part : ArchwayPart.values()) {
                    check(ArchwayLayout.of(archway.pos(part), helper.getLevel().getBlockState(archway.pos(part))).equals(archway),
                            name + ": " + part + " names another archway");
                }
                helper.setBlock(ANCHOR, Blocks.AIR);
                assertNoArchway(helper, archway);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyBlockedCellRefusesWithoutCharge(GameTestHelper helper) {
        floor(helper);
        BlockPos upperRight = ANCHOR.east().above();
        helper.setBlock(upperRight, Blocks.OAK_PLANKS);
        assertRefused(helper, "occupied");
        check(helper.getBlockState(upperRight).is(Blocks.OAK_PLANKS), "the occupied cell was changed");
        helper.setBlock(upperRight, Blocks.AIR);

        helper.setBlock(upperRight, Blocks.WATER);
        assertRefused(helper, "fluid");
        helper.setBlock(upperRight, Blocks.AIR);

        // A pig in the lower-left cell stands where the jamb would be.
        var pig = helper.spawn(EntityType.PIG, new Vec3(ANCHOR.getX() + 0.3, ANCHOR.getY(), ANCHOR.getZ() + 0.5));
        assertRefused(helper, "entity");
        pig.discard();

        // No floor under the lower-right cell.
        helper.setBlock(ANCHOR.east().below(), Blocks.AIR);
        assertRefused(helper, "support");
        helper.setBlock(ANCHOR.east().below(), Blocks.STONE);

        // Out of the world: through the build height, into unloaded chunks (never loaded or written).
        BlockPos base = helper.absolutePos(ANCHOR);
        int top = helper.getLevel().getMaxBuildHeight() - 1;
        for (ArchwayLayout.Assembly far : List.of(
                new ArchwayLayout.Assembly(new BlockPos(base.getX(), top, base.getZ()), Direction.NORTH, ArchwayDesign.ORDINARY),
                new ArchwayLayout.Assembly(base.offset(1_000_000, 0, 1_000_000), Direction.NORTH, ArchwayDesign.ORDINARY))) {
            check(ArchwayPlacementPlan.at(helper.getLevel(), block(ArchwayDesign.ORDINARY), far).isEmpty(), "a plan outside the world: " + far);
        }
        helper.succeed();
    }

    /** Someone standing in the middle of the passage does not obstruct it: only the frame collides. */
    @GameTest(template = TEMPLATE)
    public static void someoneInThePassageDoesNotBlockIt(GameTestHelper helper) {
        floor(helper);
        for (ArchwayDesign design : ArchwayDesign.values()) {
            var pig = helper.spawn(EntityType.PIG, new Vec3(ANCHOR.getX() + 1.0, ANCHOR.getY(), ANCHOR.getZ() + 0.5));
            ServerPlayer player = player(helper, "passage_" + design, GameType.SURVIVAL, Direction.NORTH);
            check(place(helper, player, new ItemStack(block(design).asItem()), ANCHOR).consumesAction(), design + ": refused");
            assertArchway(helper, assembly(helper, ANCHOR, Direction.NORTH, design));
            pig.discard();
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aCancelledPlacementRollsBackEveryCellAndTheItem(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(ANCHOR.above(), Blocks.GRASS); // replaceable, so it is written and must come back
        int eventsBefore = PlacementCanceller.multiPlaceEvents();
        ServerPlayer player = player(helper, "cancelled", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(block(ArchwayDesign.KEYHOLE).asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        useItemOn(helper, player, stack, ANCHOR.below(), Direction.UP);
        check(PlacementCanceller.multiPlaceEvents() == eventsBefore + 1, "expected exactly one multi-place event");
        check(player.getMainHandItem().getCount() == 1, "item not restored: " + player.getMainHandItem());
        assertNoArchway(helper, assembly(helper, ANCHOR, Direction.NORTH, ArchwayDesign.KEYHOLE));
        check(helper.getBlockState(ANCHOR.above()).is(Blocks.GRASS), "the replaced grass was not restored");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sneakingPlacesTheSame(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "sneak_place", GameType.SURVIVAL, Direction.EAST);
        player.setShiftKeyDown(true);
        try {
            check(place(helper, player, new ItemStack(block(ArchwayDesign.ORDINARY).asItem()), ANCHOR).consumesAction(), "refused");
        } finally {
            player.setShiftKeyDown(false);
        }
        assertArchway(helper, assembly(helper, ANCHOR, Direction.EAST, ArchwayDesign.ORDINARY));
        helper.succeed();
    }

    /**
     * The plan the client previews is exactly what the server writes, for both designs in every facing,
     * and the preview's frame outline ({@link ArchwayGeometry#placed}) covers exactly the placed cells'
     * shapes.
     */
    @GameTest(template = TEMPLATE)
    public static void thePreviewedPlanIsWhatIsPlaced(GameTestHelper helper) {
        floor(helper);
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (Direction facing : FACINGS) {
                String name = design + "/" + facing;
                ServerPlayer player = player(helper, "plan_" + design + "_" + facing, GameType.SURVIVAL, facing);
                ItemStack stack = new ItemStack(block(design).asItem());
                BlockPos floorAbs = helper.absolutePos(ANCHOR.below());
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floorAbs).add(0, 0.5, 0), Direction.UP, floorAbs, false);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                Optional<ArchwayPlacementPlan> plan = ArchwayPlacementPlan.of(block(design),
                        new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)));
                check(plan.isPresent(), name + ": no plan for a free spot");
                check(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), name + ": placement failed");
                ArchwayLayout.Assembly planned = plan.get().assembly();
                check(planned.equals(assembly(helper, ANCHOR, facing, design)), name + ": planned " + planned);
                double shapes = 0;
                for (ArchwayPart part : ArchwayPart.values()) {
                    BlockPos cell = planned.pos(part);
                    BlockState placed = helper.getLevel().getBlockState(cell);
                    check(placed.equals(plan.get().states().get(part)), name + ": " + part + " placed " + placed);
                    shapes += placed.getShape(helper.getLevel(), cell, CollisionContext.empty()).toAabbs().stream()
                            .mapToDouble(box -> box.getXsize() * box.getYsize() * box.getZsize()).sum();
                    // Every box of the cell's shape lies inside the outline's boxes, offset to the cell.
                    BlockPos offset = cell.subtract(planned.anchor());
                    for (var box : placed.getShape(helper.getLevel(), cell, CollisionContext.empty()).toAabbs()) {
                        Vec3 centre = box.getCenter().add(offset.getX(), offset.getY(), offset.getZ()).scale(16);
                        check(ArchwayGeometry.placed(design, facing).stream().anyMatch(b -> b.containsStrictly(centre.x, centre.y, centre.z)),
                                name + ": " + part + " shape outside the previewed frame");
                    }
                }
                double outline = ArchwayGeometry.placed(design, facing).stream().mapToDouble(Box::volume).sum() / 4096.0;
                check(Math.abs(shapes - outline) < 1e-6, name + ": the outline " + outline + " is not the placed shapes " + shapes);
                helper.setBlock(ANCHOR, Blocks.AIR);
                assertNoArchway(helper, planned);
            }
        }
        // A refused spot has no plan.
        helper.setBlock(ANCHOR.above(), Blocks.STONE);
        ServerPlayer player = player(helper, "plan_refused", GameType.SURVIVAL, Direction.NORTH);
        BlockPos floorAbs = helper.absolutePos(ANCHOR.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floorAbs).add(0, 0.5, 0), Direction.UP, floorAbs, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block(ArchwayDesign.ORDINARY).asItem()));
        check(ArchwayPlacementPlan.of(block(ArchwayDesign.ORDINARY),
                new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit))).isEmpty(), "a plan for a blocked spot");
        helper.succeed();
    }

    private static void assertRefused(GameTestHelper helper, String name) {
        ServerPlayer player = player(helper, "archway_" + name, GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = new ItemStack(block(ArchwayDesign.ORDINARY).asItem());
        InteractionResult result = place(helper, player, stack, ANCHOR);
        check(!result.consumesAction(), name + ": placement should have been refused, got " + result);
        check(stack.getCount() == 1, name + ": item consumed on refusal");
        assertNoArchway(helper, assembly(helper, ANCHOR, Direction.NORTH, ArchwayDesign.ORDINARY));
    }
}
