package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import io.github.dirtyredz.roundwood.fixture.DoorFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowRays;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BIRCH;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.WARPED;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.pick;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.windowLook;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.useItemOn;

/**
 * Restyling a window's frame with planks and dye (0.8.0: no sneak needed, sneaking does the same),
 * aimed at the frame wood by a real ray, including what must NOT change: its pane state, its water,
 * its neighbours' connections and ordinary item use on it. Glass tinting is in WindowGlassGameTests.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowAppearanceGameTests {
    private WindowAppearanceGameTests() {
    }

    /** Places a window by real item use as a player facing north: axis x, front face south. */
    private static BlockPos place(GameTestHelper helper, String name, ItemStack stack, BlockPos rel) {
        ServerPlayer placer = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        check(useItemOn(helper, placer, stack, rel.below(), Direction.UP).consumesAction(), name + ": placement at " + rel);
        BlockPos abs = helper.absolutePos(rel);
        check(helper.getLevel().getBlockState(abs).getBlock() instanceof RoundWindowBlock, name + ": no window at " + rel);
        return abs;
    }

    private static BlockPos place(GameTestHelper helper, String name, BlockPos rel) {
        return place(helper, name, new ItemStack(ModItems.ROUND_WINDOW.get()), rel);
    }

    /** A real ray from the south onto the frame wood (face pixel 1.5, 1.5) of the window at {@code abs}. */
    private static BlockHitResult front(GameTestHelper helper, BlockPos abs) {
        ServerPlayer viewer = player(helper, "frame_ray", GameType.SURVIVAL, Direction.NORTH);
        return WindowRays.face(helper, viewer, DoorFixtures.rel(helper, abs), 1.5, 1.5, Direction.SOUTH);
    }

    private static BlockState state(GameTestHelper helper, BlockPos abs) {
        return helper.getLevel().getBlockState(abs);
    }

    private static void expect(GameTestHelper helper, BlockPos abs, Appearance look, String step) {
        check(windowLook(helper, abs).equals(look), step + ": window shows " + windowLook(helper, abs).key() + ", expected " + look.key());
    }

    @GameTest(template = TEMPLATE)
    public static void woodPaintAndUnpaintKeepTheStateAndConsumeOneEach(GameTestHelper helper) {
        floor(helper);
        BlockPos abs = place(helper, "style", new BlockPos(4, 1, 4));
        BlockState before = state(helper, abs);
        ServerPlayer player = player(helper, "style", GameType.SURVIVAL, Direction.NORTH);

        ItemStack birch = new ItemStack(Items.BIRCH_PLANKS, 2);
        check(use(helper, player, birch, ItemStack.EMPTY, front(helper, abs), true).consumesAction(), "sneak + birch not handled");
        expect(helper, abs, BIRCH, "birch");
        check(birch.getCount() == 1, "expected one plank consumed, left " + birch.getCount());
        check(state(helper, abs) == before, "the pane state changed: " + before + " -> " + state(helper, abs));

        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        check(use(helper, player, dye, ItemStack.EMPTY, front(helper, abs), false).consumesAction(), "dye not handled");
        expect(helper, abs, BIRCH.painted(DyeColor.RED), "painted");
        check(dye.getCount() == 1, "expected one dye consumed, left " + dye.getCount());

        ItemStack again = new ItemStack(Items.BIRCH_PLANKS, 2);
        check(use(helper, player, again, ItemStack.EMPTY, front(helper, abs), true).consumesAction(), "sneak + birch over paint not handled");
        expect(helper, abs, BIRCH, "unpainted");
        check(again.getCount() == 1, "unpainting should consume one plank, left " + again.getCount());
        check(state(helper, abs) == before, "the pane state changed after unpainting");
        helper.succeed();
    }

    /** A flush window restyles like a centred one: only the look changes, the position (and its absent connections) stay. */
    @GameTest(template = TEMPLATE)
    public static void aFlushWindowRestylesWithoutMoving(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "flush_style_placer", GameType.SURVIVAL, Direction.NORTH);
        BlockPos rel = new BlockPos(4, 1, 4);
        check(WindowFixtures.place(placer, new ItemStack(ModItems.ROUND_WINDOW.get()),
                WindowFixtures.zone(helper, rel.below(), Direction.UP, WindowTargetZones.Zone.V_MIN)).consumesAction(), "flush placement");
        helper.setBlock(rel.east(), Blocks.GLASS_PANE);
        BlockPos abs = helper.absolutePos(rel);
        BlockState before = state(helper, abs);
        check(before.getValue(RoundWindowBlock.POSITION) == WindowPosition.NORTH && !before.getValue(RoundWindowBlock.EAST), "setup " + before);
        ServerPlayer player = player(helper, "flush_style", GameType.SURVIVAL, Direction.NORTH);
        check(use(helper, player, new ItemStack(Items.BIRCH_PLANKS), ItemStack.EMPTY, front(helper, abs), true).consumesAction(), "sneak + birch");
        check(use(helper, player, new ItemStack(Items.RED_DYE), ItemStack.EMPTY, front(helper, abs), true).consumesAction(), "sneaking dye");
        expect(helper, abs, BIRCH.painted(DyeColor.RED), "flush restyle");
        check(state(helper, abs) == before, "restyling changed the flush window's state: " + before + " -> " + state(helper, abs));
        use(helper, player, new ItemStack(Items.WATER_BUCKET), ItemStack.EMPTY, front(helper, abs), false);
        check(state(helper, abs).getValue(RoundWindowBlock.POSITION) == WindowPosition.NORTH, "a bucket moved the window");
        helper.setBlock(rel, Blocks.AIR);
        helper.succeed();
    }

    /** Masonry is trims only: refused on the frame, sneaking or not, nothing used or placed. Plain planks restyle (0.8.0). */
    @GameTest(template = TEMPLATE)
    public static void masonryIsRefusedAndPlainPlanksRestyleAWindow(GameTestHelper helper) {
        floor(helper);
        BlockPos abs = place(helper, "refuse", new BlockPos(4, 1, 3));
        BlockState before = state(helper, abs);
        ServerPlayer player = player(helper, "refuse", GameType.SURVIVAL, Direction.NORTH);
        for (boolean sneaking : new boolean[] {true, false}) {
            ItemStack bricks = new ItemStack(Items.BRICKS, 2);
            use(helper, player, bricks, ItemStack.EMPTY, front(helper, abs), sneaking);
            expect(helper, abs, Appearance.DEFAULT, "bricks, sneaking " + sneaking);
            check(bricks.getCount() == 2 && !helper.getLevel().getBlockState(abs.south()).is(Blocks.BRICKS), "bricks used or placed");
        }
        ItemStack oak = new ItemStack(Items.OAK_PLANKS, 2);
        use(helper, player, oak, ItemStack.EMPTY, front(helper, abs), false);
        expect(helper, abs, Appearance.DEFAULT, "plain oak planks (unchanged, free)");
        check(oak.getCount() == 2, "oak on an oak frame was paid for");
        ItemStack spruce = new ItemStack(Items.SPRUCE_PLANKS, 2);
        use(helper, player, spruce, ItemStack.EMPTY, front(helper, abs), false);
        expect(helper, abs, SPRUCE, "plain spruce planks");
        check(spruce.getCount() == 1 && !helper.getLevel().getBlockState(abs.south()).is(Blocks.SPRUCE_PLANKS), "spruce cost or placement");
        check(state(helper, abs) == before, "the window state changed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aWaterloggedWindowKeepsItsLookAndBucketsStillWork(GameTestHelper helper) {
        floor(helper);
        BlockPos rel = new BlockPos(4, 1, 2);
        BlockPos abs = place(helper, "water", rel);
        ServerPlayer player = player(helper, "water", GameType.SURVIVAL, Direction.NORTH);
        use(helper, player, new ItemStack(Items.BIRCH_PLANKS, 2), ItemStack.EMPTY, front(helper, abs), true);
        expect(helper, abs, BIRCH, "birch before water");
        check(!state(helper, abs).getValue(BlockStateProperties.WATERLOGGED), "the window started waterlogged");

        // Stand south of the window, eyes level with its middle, and use the bucket straight at it.
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.5, Direction.NORTH.toYRot(), 24);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        player.gameMode.useItem(player, helper.getLevel(), bucket, InteractionHand.MAIN_HAND);
        check(state(helper, abs).getValue(BlockStateProperties.WATERLOGGED), "the water bucket did not waterlog the window");
        expect(helper, abs, BIRCH, "birch after water");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void restylingOneOfTwoWindowsLeavesTheOtherAlone(GameTestHelper helper) {
        floor(helper);
        BlockPos left = place(helper, "pair_a", new BlockPos(3, 1, 4));
        BlockPos right = place(helper, "pair_b", new BlockPos(4, 1, 4));
        BlockState leftBefore = state(helper, left);
        BlockState rightBefore = state(helper, right);
        ServerPlayer player = player(helper, "pair", GameType.SURVIVAL, Direction.NORTH);
        use(helper, player, new ItemStack(Items.BIRCH_PLANKS, 2), ItemStack.EMPTY, front(helper, left), true);
        expect(helper, left, BIRCH, "left");
        expect(helper, right, Appearance.DEFAULT, "right");
        check(state(helper, left) == leftBefore && state(helper, right) == rightBefore, "the neighbouring states changed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pickReturnsTheStyledWindow(GameTestHelper helper) {
        floor(helper);
        BlockPos abs = place(helper, "pick", new BlockPos(4, 1, 5));
        ServerPlayer player = player(helper, "pick", GameType.CREATIVE, Direction.NORTH);
        use(helper, player, new ItemStack(Items.BIRCH_PLANKS), ItemStack.EMPTY, front(helper, abs), true);
        use(helper, player, new ItemStack(Items.LIME_DYE), ItemStack.EMPTY, front(helper, abs), false);
        Appearance look = BIRCH.painted(DyeColor.LIME);
        expect(helper, abs, look, "setup");
        ItemStack picked = pick(helper, abs);
        check(picked.is(ModItems.ROUND_WINDOW.get()), "picked " + picked);
        check(AppearanceStacks.read(picked).equals(look), "picked look " + AppearanceStacks.read(picked).key());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aStyledWindowItemPlacesItsLook(GameTestHelper helper) {
        floor(helper);
        ItemStack warped = AppearanceStacks.create(ModItems.ROUND_WINDOW.get(), WARPED);
        BlockPos abs = place(helper, "item", warped, new BlockPos(2, 1, 2));
        expect(helper, abs, WARPED, "placed from a warped stack");
        check(AppearanceStacks.read(pick(helper, abs)).equals(WARPED), "pick of a warped window lost the look");
        helper.succeed();
    }
}
