package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BIRCH;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.doorLook;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.pick;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.trimLook;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fitToDoor;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fittedTo;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.frontHit;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.oakTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;

/**
 * Restyling a legacy frame (placed by the fixture writer, not the item) with the sneak and dye gestures: wood, paint, masonry, what is consumed in each
 * game mode, who may do it, which hand, and that the whole frame (one anchor) follows.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrimAppearanceGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    /** Points on the ring in each quarter (45 degrees out from the centre), as (across, up). */
    private static final double[][] QUARTER_HITS = {{0.36, 0.36}, {0.36, 1.64}, {1.64, 0.36}, {1.64, 1.64}};

    private TrimAppearanceGameTests() {
    }

    private static TrimLayout.Assembly place(GameTestHelper helper, String name) {
        floor(helper);
        ServerPlayer placer = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, placer, new ItemStack(ModItems.ROUND_TRIM.get()), ANCHOR).consumesAction(), name + ": placement");
        return trimAssembly(helper, ANCHOR, Direction.NORTH);
    }

    private static BlockHitResult side(TrimLayout.Assembly trim) {
        return frontHit(trim, 0.05, 1.0);
    }

    /** Uses {@code stack} as the main hand on the trim's left side. */
    private static boolean gesture(GameTestHelper helper, ServerPlayer player, ItemStack stack, TrimLayout.Assembly trim, boolean sneaking) {
        return use(helper, player, stack, ItemStack.EMPTY, side(trim), sneaking).consumesAction();
    }

    private static void expect(GameTestHelper helper, TrimLayout.Assembly trim, Appearance look, String step) {
        check(trimLook(helper, trim).equals(look), step + ": trim shows " + trimLook(helper, trim).key() + ", expected " + look.key());
        assertTrim(helper, trim, oakTrim());
    }

    @GameTest(template = TEMPLATE)
    public static void survivalSequenceWoodPaintMasonryAndBack(GameTestHelper helper) {
        TrimLayout.Assembly trim = place(helper, "seq");
        ServerPlayer player = player(helper, "seq", GameType.SURVIVAL, Direction.NORTH);

        ItemStack spruce = new ItemStack(Items.SPRUCE_PLANKS, 2);
        check(gesture(helper, player, spruce, trim, true), "sneak + spruce was not handled");
        expect(helper, trim, SPRUCE, "spruce");
        check(spruce.getCount() == 1, "exactly one plank should be consumed, left " + spruce.getCount());

        gesture(helper, player, spruce, trim, true);
        expect(helper, trim, SPRUCE, "spruce again");
        check(spruce.getCount() == 1, "an unchanged result consumed a plank");

        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        check(gesture(helper, player, dye, trim, false), "dye was not handled");
        expect(helper, trim, SPRUCE.painted(DyeColor.RED), "painted");
        check(dye.getCount() == 1, "exactly one dye should be consumed, left " + dye.getCount());

        ItemStack again = new ItemStack(Items.SPRUCE_PLANKS, 2);
        check(gesture(helper, player, again, trim, true), "sneak + spruce over paint was not handled");
        expect(helper, trim, SPRUCE, "paint removed");
        check(again.getCount() == 1, "removing paint should consume one plank, left " + again.getCount());

        ItemStack bricks = new ItemStack(Items.BRICKS, 2);
        check(gesture(helper, player, bricks, trim, true), "sneak + bricks was not handled");
        expect(helper, trim, BRICKS, "bricks");
        check(bricks.getCount() == 1, "exactly one brick block should be consumed, left " + bricks.getCount());

        for (boolean sneaking : new boolean[] {true, false}) {
            ItemStack refused = new ItemStack(Items.BLUE_DYE, 2);
            gesture(helper, player, refused, trim, sneaking);
            expect(helper, trim, BRICKS, "dye on masonry, sneaking=" + sneaking);
            check(refused.getCount() == 2, "dye on masonry consumed (sneaking=" + sneaking + "): " + refused.getCount());
        }

        ItemStack oak = new ItemStack(Items.OAK_PLANKS, 2);
        check(gesture(helper, player, oak, trim, true), "sneak + oak over masonry was not handled");
        expect(helper, trim, Appearance.DEFAULT, "masonry back to wood");
        check(oak.getCount() == 1, "oak over masonry should consume one, left " + oak.getCount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeChangesAndConsumesNothing(GameTestHelper helper) {
        TrimLayout.Assembly trim = place(helper, "creative");
        ServerPlayer player = player(helper, "creative", GameType.CREATIVE, Direction.NORTH);
        ItemStack planks = new ItemStack(Items.BIRCH_PLANKS, 2);
        check(gesture(helper, player, planks, trim, true), "creative sneak + planks not handled");
        expect(helper, trim, BIRCH, "creative wood");
        ItemStack dye = new ItemStack(Items.GREEN_DYE, 2);
        check(gesture(helper, player, dye, trim, false), "creative dye not handled");
        expect(helper, trim, BIRCH.painted(DyeColor.GREEN), "creative paint");
        check(planks.getCount() == 2 && dye.getCount() == 2, "creative consumed items: " + planks.getCount() + ", " + dye.getCount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aPlayerWhoMayNotBuildChangesAndLosesNothing(GameTestHelper helper) {
        TrimLayout.Assembly trim = place(helper, "adventure");
        ServerPlayer player = player(helper, "adventure", GameType.ADVENTURE, Direction.NORTH);
        ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 2);
        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        gesture(helper, player, planks, trim, true);
        gesture(helper, player, dye, trim, false);
        expect(helper, trim, Appearance.DEFAULT, "adventure");
        check(planks.getCount() == 2 && dye.getCount() == 2, "adventure lost items: " + planks.getCount() + ", " + dye.getCount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void theOffhandActsOnlyWhenTheMainHandIsEmpty(GameTestHelper helper) {
        TrimLayout.Assembly trim = place(helper, "hands");
        ServerPlayer player = player(helper, "hands", GameType.SURVIVAL, Direction.NORTH);
        ItemStack blocked = new ItemStack(Items.RED_DYE, 2);
        use(helper, player, new ItemStack(Items.STICK), blocked, side(trim), false);
        expect(helper, trim, Appearance.DEFAULT, "stick in the main hand");
        check(blocked.getCount() == 2, "the offhand dye was consumed behind a held stick");

        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        check(use(helper, player, ItemStack.EMPTY, dye, side(trim), false).consumesAction(), "offhand dye not handled");
        expect(helper, trim, Appearance.DEFAULT.painted(DyeColor.RED), "offhand dye");
        check(dye.getCount() == 1, "offhand dye count " + dye.getCount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aimingAtAnyQuarterRestylesTheWholeFrame(GameTestHelper helper) {
        TrimLayout.Assembly trim = place(helper, "quarters");
        ServerPlayer player = player(helper, "quarters", GameType.CREATIVE, Direction.NORTH);
        Appearance[] looks = {SPRUCE, BIRCH, BRICKS, Appearance.DEFAULT};
        for (int i = 0; i < QUARTER_HITS.length; i++) {
            BlockHitResult hit = frontHit(trim, QUARTER_HITS[i][0], QUARTER_HITS[i][1]);
            ItemStack stack = new ItemStack(i == 2 ? Items.BRICKS : i == 0 ? Items.SPRUCE_PLANKS : i == 1 ? Items.BIRCH_PLANKS : Items.OAK_PLANKS);
            check(use(helper, player, stack, ItemStack.EMPTY, hit, true).consumesAction(), "quarter " + i + " was not handled");
            expect(helper, trim, looks[i], "quarter " + i);
            int stored = 0;
            for (TrimPart part : TrimPart.values()) {
                if (helper.getLevel().getBlockEntity(trim.pos(part)) instanceof RoundTrimBlockEntity entity
                        && !entity.appearance().equals(Appearance.DEFAULT)) {
                    stored++;
                }
            }
            check(stored <= 1, "more than one entity stores the look: " + stored);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void restylingAFittedTrimLeavesTheDoorAlone(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "fitted_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "door placement");
        DoorLayout.Assembly door = assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT);
        check(fitToDoor(helper, placer, new ItemStack(oakTrim().asItem()), door, 0.5, 0.5).consumesAction(), "fitting");
        TrimLayout.Assembly trim = fittedTo(door);

        ServerPlayer player = player(helper, "fitted", GameType.SURVIVAL, Direction.NORTH);
        check(gesture(helper, player, new ItemStack(Items.SPRUCE_PLANKS, 2), trim, true), "restyling the fitted trim was not handled");
        expect(helper, trim, SPRUCE, "fitted trim");
        check(doorLook(helper, door).equals(Appearance.DEFAULT), "the door took the trim's look: " + doorLook(helper, door).key());
        check(!helper.getLevel().getBlockState(door.anchor()).getValue(DoorBlock.OPEN), "restyling the trim opened the door");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pickOnEveryQuarterReturnsTheStyledTrim(GameTestHelper helper) {
        TrimLayout.Assembly trim = place(helper, "pick");
        ServerPlayer player = player(helper, "pick", GameType.CREATIVE, Direction.NORTH);
        use(helper, player, new ItemStack(Items.SPRUCE_PLANKS), ItemStack.EMPTY, side(trim), true);
        use(helper, player, new ItemStack(Items.CYAN_DYE), ItemStack.EMPTY, side(trim), false);
        Appearance look = SPRUCE.painted(DyeColor.CYAN);
        expect(helper, trim, look, "setup");
        for (TrimPart part : TrimPart.values()) {
            ItemStack picked = pick(helper, trim.pos(part));
            check(picked.is(ModItems.ROUND_TRIM.get()), part + ": picked " + picked);
            check(AppearanceStacks.read(picked).equals(look), part + ": picked look " + AppearanceStacks.read(picked).key());
        }
        helper.succeed();
    }
}
