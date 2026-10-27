package io.github.dirtyredz.roundwood.block.door.appearance;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.MUSHROOM;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.WARPED;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.corner;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.doorLook;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.leaf;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.oakPainted;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.pick;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeOakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.rel;

/**
 * Restyling doors through the real interaction path ({@code ServerPlayerGameMode.useItemOn}, so
 * Forge's right-click event and the sneak bypass are exercised): which region wins, what each
 * gesture costs, that a door never toggles or receives a placed plank while being restyled, the
 * offhand rule and permissions.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorAppearanceGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private DoorAppearanceGameTests() {
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, Direction facing, DoorHingeSide hinge, String name) {
        return placeOakDoor(helper, ANCHOR, facing, hinge, name + "_placer");
    }

    private static boolean open(GameTestHelper helper, DoorLayout.Assembly door) {
        return helper.getLevel().getBlockState(door.anchor()).getValue(DoorBlock.OPEN);
    }

    /** No plank or other block was placed anywhere around the door. */
    private static void nothingPlacedAround(GameTestHelper helper, DoorLayout.Assembly door, String what) {
        for (BlockPos cell : door.positions()) {
            for (Direction side : Direction.values()) {
                BlockPos next = cell.relative(side);
                check(!helper.getLevel().getBlockState(next).is(Blocks.SPRUCE_PLANKS)
                        && !helper.getLevel().getBlockState(next).is(Blocks.WARPED_PLANKS), what + ": a plank was placed at " + next);
            }
        }
    }

    @GameTest(template = TEMPLATE)
    public static void sneakPlanksOnTheLeafReplanksForOnePlank(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "replank");
        ServerPlayer player = player(helper, "replank", GameType.SURVIVAL, Direction.NORTH);
        ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 3);
        check(use(helper, player, planks, ItemStack.EMPTY, leaf(door, false), true).consumesAction(), "replanking not handled");
        check(doorLook(helper, door).equals(SPRUCE), "door shows " + doorLook(helper, door));
        check(planks.getCount() == 2, "expected one plank consumed, left " + planks.getCount());
        check(!open(helper, door), "replanking toggled the door");
        nothingPlacedAround(helper, door, "replank");

        // The same wood again is handled but free.
        check(use(helper, player, planks, ItemStack.EMPTY, leaf(door, false), true).consumesAction(), "same wood not handled");
        check(planks.getCount() == 2, "re-applying the same wood consumed a plank");
        check(!open(helper, door), "the unchanged gesture toggled the door");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dyePaintsWithOrWithoutSneakingAndPlanksStripIt(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "paint");
        ServerPlayer player = player(helper, "paint", GameType.SURVIVAL, Direction.NORTH);
        ItemStack red = new ItemStack(Items.RED_DYE, 2);
        check(use(helper, player, red, ItemStack.EMPTY, leaf(door, false), false).consumesAction(), "painting not handled");
        check(doorLook(helper, door).equals(oakPainted(DyeColor.RED)), "door shows " + doorLook(helper, door));
        check(red.getCount() == 1 && !open(helper, door), "paint: dye count " + red.getCount() + ", open " + open(helper, door));
        // Same paint again (sneaking this time): handled, free.
        check(use(helper, player, red, ItemStack.EMPTY, leaf(door, false), true).consumesAction(), "repaint not handled");
        check(red.getCount() == 1, "repainting the same colour consumed dye");
        // Another dye repaints, the wood stays.
        ItemStack blue = new ItemStack(Items.BLUE_DYE);
        use(helper, player, blue, ItemStack.EMPTY, leaf(door, false), true);
        check(doorLook(helper, door).equals(oakPainted(DyeColor.BLUE)) && blue.isEmpty(), "blue repaint failed: " + doorLook(helper, door));
        // Re-planking with the same wood strips the paint and costs a plank.
        ItemStack oak = new ItemStack(Items.OAK_PLANKS, 2);
        use(helper, player, oak, ItemStack.EMPTY, leaf(door, false), true);
        check(doorLook(helper, door).equals(Appearance.DEFAULT) && oak.getCount() == 1,
                "same-wood replank did not strip paint: " + doorLook(helper, door) + ", planks " + oak.getCount());
        check(!open(helper, door), "the door toggled while being painted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyDyeAndAModdedWoodApply(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.RIGHT, "every_dye");
        ServerPlayer player = player(helper, "every_dye", GameType.CREATIVE, Direction.NORTH);
        for (DyeColor dye : DyeColor.values()) {
            ItemStack stack = new ItemStack(DyeItem.byColor(dye));
            use(helper, player, stack, ItemStack.EMPTY, leaf(door, false), false);
            check(doorLook(helper, door).equals(oakPainted(dye)), dye + ": door shows " + doorLook(helper, door));
            check(stack.getCount() == 1, dye + ": creative consumed dye");
        }
        // A tag-only plank (the GameTest datapack adds mushroom stems to roundwood:appearance_planks).
        use(helper, player, new ItemStack(Items.MUSHROOM_STEM), ItemStack.EMPTY, leaf(door, false), true);
        check(doorLook(helper, door).equals(MUSHROOM), "tag-based plank not applied: " + doorLook(helper, door));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cornersRefuseRestylingWithoutPlacingOrToggling(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "corner");
        ServerPlayer player = player(helper, "corner", GameType.SURVIVAL, Direction.NORTH);
        ItemStack planks = new ItemStack(Items.WARPED_PLANKS, 2);
        InteractionResult result = use(helper, player, planks, ItemStack.EMPTY, corner(door), true);
        check(result.consumesAction(), "sneak + planks on a corner was not handled: " + result);
        check(doorLook(helper, door).equals(Appearance.DEFAULT) && planks.getCount() == 2, "corner sneak-plank changed or consumed");
        check(!open(helper, door) && anchorEntity(helper, door).camouflage().isEmpty(), "corner sneak-plank toggled or camouflaged");
        nothingPlacedAround(helper, door, "corner");
        ItemStack dye = new ItemStack(Items.GREEN_DYE);
        use(helper, player, dye, ItemStack.EMPTY, corner(door), true);
        check(doorLook(helper, door).equals(Appearance.DEFAULT) && dye.getCount() == 1 && !open(helper, door), "corner dye changed something");
        // Not sneaking, planks on a corner are ordinary camouflage (paid, refundable), not restyling.
        use(helper, player, planks, ItemStack.EMPTY, corner(door), false);
        check(anchorEntity(helper, door).camouflage().map(c -> c.material().is(Blocks.WARPED_PLANKS)).orElse(false),
                "plain planks on a corner did not camouflage");
        check(doorLook(helper, door).equals(Appearance.DEFAULT) && planks.getCount() == 1, "camouflage changed the look or cost wrong");
        helper.succeed();
    }

    /** Every facing, hinge and open state: the leaf takes the wood from any part, and a plain click toggles exactly once. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void everyFacingHingeAndOpenStateRestylesFromTheLeaf(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                for (boolean openState : new boolean[] {false, true}) {
                    String what = facing + "/" + hinge + "/" + (openState ? "open" : "closed");
                    DoorLayout.Assembly door = new DoorLayout.Assembly(helper.absolutePos(ANCHOR),facing,hinge,io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND);
                    check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(helper.getLevel(),oakDoor(),door,openState),"settled body fixture");
                    ServerPlayer player = player(helper, "all_" + facing + hinge, GameType.SURVIVAL, facing);
                    ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 2);
                    check(use(helper, player, planks, ItemStack.EMPTY, leaf(door, openState), true).consumesAction(), what + ": not handled");
                    check(doorLook(helper, door).equals(SPRUCE), what + ": door shows " + doorLook(helper, door));
                    check(open(helper, door) == openState, what + ": restyling toggled the door");
                    check(planks.getCount() == 1, what + ": cost " + (2 - planks.getCount()));
                    nothingPlacedAround(helper, door, what);
                    helper.setBlock(rel(helper, door.anchor()), Blocks.AIR);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void offhandPaintsOnlyWithAnEmptyMainHand(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "offhand");
        ServerPlayer player = player(helper, "offhand", GameType.SURVIVAL, Direction.NORTH);
        ItemStack dye = new ItemStack(Items.YELLOW_DYE, 2);
        check(use(helper, player, ItemStack.EMPTY, dye, leaf(door, false), false).consumesAction(), "offhand dye not handled");
        check(doorLook(helper, door).equals(oakPainted(DyeColor.YELLOW)) && dye.getCount() == 1, "offhand did not paint");
        check(!open(helper, door), "the main hand toggled before the offhand painted");
        // Main hand occupied: the main hand's ordinary use (a toggle) wins, the offhand dye is untouched.
        ItemStack orange = new ItemStack(Items.ORANGE_DYE);
        use(helper, player, new ItemStack(Items.STICK), orange, leaf(door, false), false);
        check(open(helper, door), "a click with a stick did not toggle the door");
        check(doorLook(helper, door).equals(oakPainted(DyeColor.YELLOW)) && orange.getCount() == 1, "the offhand acted with a full main hand");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeIsFreeAndAdventureIsRefused(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "modes");
        ServerPlayer creative = player(helper, "modes_creative", GameType.CREATIVE, Direction.NORTH);
        ItemStack planks = new ItemStack(Items.WARPED_PLANKS);
        use(helper, creative, planks, ItemStack.EMPTY, leaf(door, false), true);
        check(doorLook(helper, door).equals(WARPED) && planks.getCount() == 1, "creative replank failed or cost a plank");
        ServerPlayer adventure = player(helper, "modes_adventure", GameType.ADVENTURE, Direction.NORTH);
        ItemStack dye = new ItemStack(Items.PURPLE_DYE);
        use(helper, adventure, dye, ItemStack.EMPTY, leaf(door, false), true);
        check(doorLook(helper, door).equals(WARPED) && dye.getCount() == 1, "an adventure player restyled the door");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pickingAnyPartGivesTheDoorInItsLook(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly door = place(helper, Direction.EAST, DoorHingeSide.RIGHT, "pick");
        anchorEntity(helper, door).setAppearance(SPRUCE.painted(DyeColor.CYAN));
        for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
            ItemStack picked = pick(helper, door.pos(part));
            check(picked.is(oakDoor().asItem()), part + ": picked " + picked);
            check(AppearanceStacks.read(picked).equals(SPRUCE.painted(DyeColor.CYAN)),
                    part + ": picked look " + AppearanceStacks.read(picked));
            check(!picked.getOrCreateTag().contains("Camouflage"), part + ": the pick carries camouflage");
        }
        helper.succeed();
    }
}
