package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.STONE_BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.windowLook;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/**
 * The window trim (0.7.0) in a running server: fitting it with an ordinary click on any of the ten
 * windows, restyling it on its band with planks, masonry or dye (no sneak needed, sneaking too),
 * restyling the pane beside it, removing it with shears, the offhand rule, water, saving, and loot
 * (pane and trim drop separately, each in its own look).
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowTrimGameTests {
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    /** Centred, front south: the trim's front face is at z 10/16, its band includes face pixel (8, 1). */
    private static final WindowPose SOUTH = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);

    private WindowTrimGameTests() {
    }

    private static BlockState window(WindowDesign design) {
        return ShowcaseWindow.of(design, SOUTH, 0, 0, 0, Appearance.DEFAULT).state();
    }

    /** A hit on the south (front) side of the trim of {@code design}, on a pixel of its band. */
    private static BlockHitResult bandHit(GameTestHelper helper, WindowDesign design) {
        int[] p = switch (design.profile()) {
            case ROUND -> new int[] {8, 1};
            case ARCH_LEFT -> new int[] {1, 1};
            case ARCH_RIGHT -> new int[] {14, 1};
            case BOTTOM_LEFT -> new int[] {0, 8};
            case BOTTOM_RIGHT -> new int[] {15, 8};
        };
        return WindowFixtures.hit(helper, AT, Direction.SOUTH, (p[0] + 0.5) / 16, (p[1] + 0.5) / 16, 10 / 16.0);
    }

    /** A hit on the round window's own wood outside its band: the lower-left corner (inside the band on other profiles). */
    private static BlockHitResult paneHit(GameTestHelper helper) {
        return WindowFixtures.hit(helper, AT, Direction.SOUTH, 1.5 / 16, 1.5 / 16, 9 / 16.0);
    }

    private static RoundWindowBlockEntity entity(GameTestHelper helper) {
        return (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(AT));
    }

    private static ItemStack trims(Appearance look, int count) {
        ItemStack stack = styled(ModItems.WINDOW_TRIM.get(), look);
        stack.setCount(count);
        return stack;
    }

    /** An ordinary click fits a trim on every window design; a second trim is refused and kept. */
    @GameTest(template = TEMPLATE)
    public static void anOrdinaryClickFitsTheTrimOnAllTenWindows(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_fit", GameType.SURVIVAL, Direction.NORTH);
        for (WindowDesign design : WindowDesign.all()) {
            helper.setBlock(AT, window(design));
            ItemStack stack = trims(SPRUCE, 2);
            AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, paneHit(helper), false);
            BlockState state = helper.getBlockState(AT);
            check(WindowPaneBlock.trimmed(state), design.id() + ": not trimmed: " + state);
            check(entity(helper).trim().equals(Optional.of(SPRUCE)), design.id() + ": trim look " + entity(helper).trim());
            check(entity(helper).appearance().equals(Appearance.DEFAULT), design.id() + ": the pane changed");
            check(player.getMainHandItem().getCount() == 1, design.id() + ": expected one trim used, holding " + player.getMainHandItem());
            AppearanceFixtures.use(helper, player, player.getMainHandItem(), ItemStack.EMPTY, paneHit(helper), true);
            check(player.getMainHandItem().getCount() == 1, design.id() + ": a second trim was used on a trimmed window");
            AABB outline = state.getShape(helper.getLevel(), helper.absolutePos(AT)).bounds();
            check(outline.minZ <= 6 / 16.0 + 1e-6 && outline.maxZ >= 10 / 16.0 - 1e-6, design.id() + ": outline misses the trim " + outline);
            AABB collision = state.getCollisionShape(helper.getLevel(), helper.absolutePos(AT)).bounds();
            check(collision.minZ >= 7 / 16.0 - 1e-6 && collision.maxZ <= 9 / 16.0 + 1e-6, design.id() + ": the trim collides " + collision);
        }
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    /** Planks, masonry and dye on the band restyle the trim only, sneaking or not; the pane keeps its own gesture. */
    @GameTest(template = TEMPLATE)
    public static void donorsOnTheBandRestyleTheTrimAndThePaneStaysApart(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_style", GameType.SURVIVAL, Direction.NORTH);
        WindowDesign round = WindowDesign.all().get(0);
        helper.setBlock(AT, window(round));
        AppearanceFixtures.use(helper, player, trims(Appearance.DEFAULT, 1), ItemStack.EMPTY, paneHit(helper), false);
        BlockHitResult band = bandHit(helper, round);
        ItemStack bricks = new ItemStack(Items.BRICKS, 4);
        check(AppearanceFixtures.use(helper, player, bricks, ItemStack.EMPTY, band, false).consumesAction(), "bricks on the band not handled");
        check(entity(helper).trim().equals(Optional.of(BRICKS)) && bricks.getCount() == 3, "plain bricks: " + entity(helper).trim() + ", " + bricks);
        check(helper.getBlockState(AT.south()).isAir(), "a brick was placed instead");
        ItemStack stone = new ItemStack(Items.STONE_BRICKS, 2);
        AppearanceFixtures.use(helper, player, stone, ItemStack.EMPTY, band, true);
        check(entity(helper).trim().equals(Optional.of(STONE_BRICKS)) && stone.getCount() == 1, "sneak + stone bricks: " + entity(helper).trim());
        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY, band, false);
        check(entity(helper).trim().equals(Optional.of(STONE_BRICKS)) && dye.getCount() == 2, "dye painted masonry or was paid for");
        ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 2);
        AppearanceFixtures.use(helper, player, planks, ItemStack.EMPTY, band, false);
        check(entity(helper).trim().equals(Optional.of(SPRUCE)) && planks.getCount() == 1, "planks on the band: " + entity(helper).trim());
        AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY, band, false);
        check(entity(helper).trim().equals(Optional.of(SPRUCE.painted(DyeColor.RED))) && dye.getCount() == 1, "dye on the band: " + entity(helper).trim());
        check(entity(helper).appearance().equals(Appearance.DEFAULT), "trim restyling recoloured the pane");
        // Outside the band: the pane's own gesture, and the trim stays.
        ItemStack birch = new ItemStack(Items.BIRCH_PLANKS, 2);
        AppearanceFixtures.use(helper, player, birch, ItemStack.EMPTY, paneHit(helper), true);
        check(entity(helper).appearance().equals(AppearanceFixtures.BIRCH), "sneak + planks on the pane: " + entity(helper).appearance());
        check(entity(helper).trim().equals(Optional.of(SPRUCE.painted(DyeColor.RED))), "pane restyling touched the trim");
        // The offhand acts only with an empty main hand.
        ItemStack offBricks = new ItemStack(Items.BRICKS, 2);
        AppearanceFixtures.use(helper, player, new ItemStack(Items.STICK), offBricks, band, false);
        // (vanilla may then place the offhand bricks as a block, as with any block item; the trim is untouched)
        check(entity(helper).trim().equals(Optional.of(SPRUCE.painted(DyeColor.RED))), "offhand restyled with a full main hand");
        helper.setBlock(AT.south(), Blocks.AIR);
        offBricks.setCount(2);
        AppearanceFixtures.use(helper, player, ItemStack.EMPTY, offBricks, band, false);
        check(entity(helper).trim().equals(Optional.of(BRICKS)) && offBricks.getCount() == 1, "offhand bricks with an empty main hand: " + entity(helper).trim());
        helper.succeed();
    }

    /** Shears on the band take the trim off: one trim item back in its look, one point of wear; creative gets nothing back. */
    @GameTest(template = TEMPLATE)
    public static void shearsRemoveTheTrimAndReturnItInItsLook(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_shear", GameType.SURVIVAL, Direction.NORTH);
        WindowDesign archRight = WindowDesign.all().get(4);
        helper.setBlock(AT, window(archRight).setValue(BlockStateProperties.WATERLOGGED, true));
        AppearanceFixtures.use(helper, player, trims(BRICKS, 1), ItemStack.EMPTY, paneHit(helper), false);
        check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), "not trimmed");
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.getInventory().clearContent();
        AppearanceFixtures.use(helper, player, shears, ItemStack.EMPTY, bandHit(helper, archRight), false);
        BlockState state = helper.getBlockState(AT);
        check(!WindowPaneBlock.trimmed(state) && state.getValue(BlockStateProperties.WATERLOGGED), "shears: " + state);
        check(entity(helper).trim().isEmpty(), "trim look left behind");
        check(shears.getDamageValue() == 1, "shears wear " + shears.getDamageValue());
        int returned = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.WINDOW_TRIM.get()) && AppearanceStacks.read(stack).equals(BRICKS)) {
                returned += stack.getCount();
            }
        }
        check(returned == 1, "expected one brick trim back, got " + returned);
        // Creative: removed, nothing returned. Shears on an untrimmed pane do nothing.
        ServerPlayer creative = player(helper, "trim_shear_creative", GameType.CREATIVE, Direction.NORTH);
        AppearanceFixtures.use(helper, creative, trims(SPRUCE, 1), ItemStack.EMPTY, paneHit(helper), false);
        creative.getInventory().clearContent();
        AppearanceFixtures.use(helper, creative, new ItemStack(Items.SHEARS), ItemStack.EMPTY, bandHit(helper, archRight), false);
        check(!WindowPaneBlock.trimmed(helper.getBlockState(AT)), "creative shears did not remove");
        check(creative.getInventory().items.stream().noneMatch(s -> s.is(ModItems.WINDOW_TRIM.get())), "creative got a trim back");
        helper.succeed();
    }

    /** Breaking a trimmed window drops its pane and its trim as two items, each in its own look; a creative break drops nothing. */
    @GameTest(template = TEMPLATE)
    public static void aTrimmedWindowDropsPaneAndTrimSeparately(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_drop", GameType.SURVIVAL, Direction.NORTH);
        Appearance pane = AppearanceFixtures.oakPainted(DyeColor.MAGENTA);
        Appearance trim = AppearanceFixtures.oakPainted(DyeColor.CYAN);
        WindowDesign design = WindowDesign.all().get(7);
        helper.setBlock(AT, window(design));
        entity(helper).setAppearance(pane);
        AppearanceFixtures.use(helper, player, trims(trim, 1), ItemStack.EMPTY, paneHit(helper), false);
        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(AT));
        check(dropped(helper, ModItems.window(design), pane) == 1, "pane drop");
        check(dropped(helper, ModItems.WINDOW_TRIM.get(), trim) == 1, "trim drop");
        check(dropped(helper, ModItems.WINDOW_TRIM.get()) == 1 && dropped(helper, ModItems.window(design)) == 1, "extra drops");
        ItemStack picked = AppearanceFixtures.pick(helper, helper.absolutePos(AT.east()));
        check(picked.isEmpty() || !picked.is(ModItems.WINDOW_TRIM.get()), "pick bundled the trim");
        clearDrops(helper);
        ServerPlayer creative = player(helper, "trim_drop_creative", GameType.CREATIVE, Direction.NORTH);
        helper.setBlock(AT, window(design));
        AppearanceFixtures.use(helper, creative, trims(trim, 1), ItemStack.EMPTY, paneHit(helper), false);
        creative.gameMode.destroyBlock(helper.absolutePos(AT));
        check(dropped(helper, ModItems.WINDOW_TRIM.get()) == 0 && dropped(helper, ModItems.window(design)) == 0, "creative break dropped items");
        helper.succeed();
    }

    /** The trim's look saves under its own tag and reloads; the pane's look and pick are independent of it. */
    @GameTest(template = TEMPLATE)
    public static void theTrimSavesAndLoadsWithoutTouchingThePane(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_save", GameType.CREATIVE, Direction.NORTH);
        helper.setBlock(AT, window(WindowDesign.all().get(2)));
        entity(helper).setAppearance(SPRUCE);
        AppearanceFixtures.use(helper, player, trims(BRICKS, 1), ItemStack.EMPTY, paneHit(helper), false);
        CompoundTag saved = entity(helper).saveWithFullMetadata();
        check(saved.contains("WindowTrim") && saved.contains("RoundwoodAppearance"), "saved " + saved);
        RoundWindowBlockEntity copy = new RoundWindowBlockEntity(entity(helper).getType(), helper.absolutePos(AT), helper.getBlockState(AT));
        copy.load(saved);
        check(copy.appearance().equals(SPRUCE) && copy.trim().equals(Optional.of(BRICKS)), "reloaded " + copy.appearance() + " " + copy.trim());
        ItemStack pick = AppearanceFixtures.pick(helper, helper.absolutePos(AT));
        check(pick.is(ModBlocks.window(WindowDesign.all().get(2)).asItem()) && AppearanceStacks.read(pick).equals(SPRUCE)
                && !pick.getOrCreateTag().contains("WindowTrim"), "pick " + pick);
        check(SPRUCE.equals(windowLook(helper, helper.absolutePos(AT))), "pane look");
        helper.succeed();
    }

    /** A trim fitted to a 0.4.0 alias window before it converts survives the conversion: one attachment, both looks kept. */
    @GameTest(template = TEMPLATE)
    public static void aTrimOnAnAliasWindowSurvivesConversion(GameTestHelper helper) {
        WindowPaneBlock alias = io.github.dirtyredz.roundwood.migration.LegacyContent.windows().get(1);
        helper.setBlock(AT, alias.defaultBlockState().setValue(WindowPaneBlock.TRIMMED, true));
        entity(helper).setTrim(BRICKS);
        Appearance historical = alias.defaultAppearance();
        helper.runAfterDelay(5, () -> {
            BlockState state = helper.getBlockState(AT);
            check(state.is(ModBlocks.roundWindow()), "alias not converted: " + state);
            check(WindowPaneBlock.trimmed(state), "conversion lost the trim: " + state);
            check(entity(helper).trim().equals(Optional.of(BRICKS)), "trim look after conversion: " + entity(helper).trim());
            check(entity(helper).appearance().equals(historical), "pane look after conversion: " + entity(helper).appearance());
            helper.succeed();
        });
    }
}
