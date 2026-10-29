package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowFaceIdentity;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.window.WindowRays;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.world.item.Item;
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

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.STONE_BRICKS;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.styled;

/**
 * Face trims on windows (0.8.0) through the production interaction path, with hits from real rays
 * clipped against each window's shape ({@link WindowRays}): all ten designs in centred, flush,
 * floor and ceiling poses take an independent trim on each face from the side clicked (no sneak),
 * an occupied face is a free no-op, shears remove only the aimed face, donors restyle only the aimed
 * face, the frame is restyled elsewhere with plain planks, masonry off the band is refused, the
 * offhand rule holds, looks save and reload, and loot gives one trim per face in its own look. Since
 * 0.9.0 the trim is the Universal Trim: every one of its ids fits, plain or sneaking, and shears and
 * loot give back the canonical {@code round_trim}.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowTrimGameTests {
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final WindowPose SOUTH = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
    /** Centred, flush both ways, floor and ceiling. */
    private static final List<WindowPose> POSES = List.of(SOUTH,
            WindowPose.upright(Direction.SOUTH, WindowDepth.NEGATIVE),
            WindowPose.upright(Direction.SOUTH, WindowDepth.POSITIVE),
            new WindowPose(Direction.UP, Direction.NORTH, WindowDepth.NEGATIVE),
            new WindowPose(Direction.DOWN, Direction.NORTH, WindowDepth.POSITIVE));

    private WindowTrimGameTests() {
    }

    public static BlockState window(WindowDesign design, WindowPose pose) {
        return ShowcaseWindow.of(design, pose, 0, 0, 0, Appearance.DEFAULT).state();
    }

    /** A pixel centre on {@code profile}'s trim band. */
    static double[] band(WindowProfile profile) {
        return switch (profile) {
            case BLANK -> throw new IllegalArgumentException("No trim band on blank pane");
            case ROUND -> new double[] {8.5, 0.5};
            case ARCH_LEFT -> new double[] {1.5, 1.5};
            case ARCH_RIGHT -> new double[] {14.5, 1.5};
            case BOTTOM_LEFT -> new double[] {0.5, 8.5};
            case BOTTOM_RIGHT -> new double[] {15.5, 8.5};
            case ARCH_BOTTOM_LEFT -> new double[] {1.5, 14.5};
            case ARCH_BOTTOM_RIGHT -> new double[] {14.5, 14.5};
            case TOP_LEFT, EDGE_LEFT -> new double[] {0.5, 8.5};
            case TOP_RIGHT, EDGE_RIGHT -> new double[] {15.5, 8.5};
            case EDGE_TOP -> new double[] {8.5, 15.5};
            case EDGE_BOTTOM -> new double[] {8.5, 0.5};
        };
    }

    private static BlockHitResult bandHit(GameTestHelper helper, ServerPlayer player, WindowProfile profile, Direction side) {
        double[] p = band(profile);
        return WindowRays.face(helper, player, AT, p[0], p[1], side);
    }

    public static RoundWindowBlockEntity entity(GameTestHelper helper, BlockPos pos) {
        return (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static AttachmentFace face(GameTestHelper helper, Direction side) {
        BlockState state = helper.getBlockState(AT);
        return WindowFaceIdentity.face(((WindowPaneBlock) state.getBlock()).pose(state), WindowPaneBlock.faceReversed(state), side);
    }

    private static ItemStack trims(Appearance look, int count) {
        ItemStack stack = styled(ModItems.ROUND_TRIM.get(), look);
        stack.setCount(count);
        return stack;
    }

    private static int returned(ServerPlayer player, Appearance look) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.ROUND_TRIM.get()) && AppearanceStacks.read(stack).equals(look)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * The owner's acceptance case, every window piece: a trim fits the face that was clicked, the other
     * face takes its own, an occupied face is refused for free, shears take off only the aimed face.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void everyDesignInEveryPoseTakesIndependentFaceTrims(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_faces", GameType.SURVIVAL, Direction.NORTH);
        for (WindowDesign design : io.github.dirtyredz.roundwood.registry.ModBlocks.WindowProducts.all()) {
            if (!design.profile().takesTrim()) continue;
            if (design.pattern() == io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern.CROSS) {
                continue; // retired: converted to a plain pane plus Cross Beams, never a live host
            }
            for (WindowPose pose : POSES) {
                io.github.dirtyredz.roundwood.fixture.window.WindowFixtures.setWindow(helper, AT, design, pose);
                String what = design.id() + " " + pose;
                Direction positive = Direction.fromAxisAndDirection(pose.normal(), Direction.AxisDirection.POSITIVE);
                Direction negative = positive.getOpposite();
                AttachmentFace plus = face(helper, positive);
                ItemStack spruce = trims(SPRUCE, 2);
                AppearanceFixtures.use(helper, player, spruce, ItemStack.EMPTY, bandHit(helper, player, design.profile(), positive), false);
                FaceTrims after = entity(helper, AT).trims();
                check(after.get(plus).equals(Optional.of(SPRUCE)) && after.count() == 1, what + ": first fit " + after);
                check(spruce.getCount() == 1, what + ": expected one trim used, holding " + spruce);
                check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), what + ": trimmed summary not set");
                AppearanceFixtures.use(helper, player, spruce, ItemStack.EMPTY, bandHit(helper, player, design.profile(), positive), false);
                check(spruce.getCount() == 1 && entity(helper, AT).trims().count() == 1, what + ": occupied face took a second trim");
                ItemStack brick = trims(BRICKS, 1);
                AppearanceFixtures.use(helper, player, brick, ItemStack.EMPTY, bandHit(helper, player, design.profile(), negative), false);
                after = entity(helper, AT).trims();
                check(after.get(plus.opposite()).equals(Optional.of(BRICKS)) && after.get(plus).equals(Optional.of(SPRUCE)),
                        what + ": other face " + after);
                player.getInventory().clearContent();
                ItemStack shears = new ItemStack(Items.SHEARS);
                AppearanceFixtures.use(helper, player, shears, ItemStack.EMPTY, bandHit(helper, player, design.profile(), positive), false);
                after = entity(helper, AT).trims();
                check(after.get(plus).isEmpty() && after.get(plus.opposite()).equals(Optional.of(BRICKS)), what + ": shears took " + after);
                check(returned(player, SPRUCE) == 1 && shears.getDamageValue() == 1, what + ": shears return/wear");
                check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), what + ": the back face still counts as trimmed");
                AppearanceFixtures.use(helper, player, shears, ItemStack.EMPTY, bandHit(helper, player, design.profile(), negative), false);
                check(!entity(helper, AT).trims().any() && !WindowPaneBlock.trimmed(helper.getBlockState(AT)), what + ": both off");
                AABB collision = helper.getBlockState(AT).getCollisionShape(helper.getLevel(), helper.absolutePos(AT)).bounds();
                AABB slab = new AABB(0, 0, 0, 1, 1, 1);
                check(slab.contains(collision.getCenter()), what + ": collision moved " + collision);
            }
        }
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Every Universal Trim id (canonical, the 0.8.0 window trim, 0.4.0 aliases in their historical
     * looks) fits the clicked face for one item, plainly or sneaking, places nothing; shears give back
     * one canonical {@code round_trim} in the same look.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void everyTrimIdFitsAWindowPlainOrSneaking(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_ids", GameType.SURVIVAL, Direction.NORTH);
        WindowDesign arch = WindowDesign.plain(WindowProfile.ARCH_RIGHT);
        for (Item id : AppearanceFixtures.universalTrimIds()) {
            for (boolean sneaking : new boolean[] {false, true}) {
                String what = id + (sneaking ? " sneaking" : "");
                io.github.dirtyredz.roundwood.fixture.window.WindowFixtures.setWindow(helper, AT, arch, SOUTH);
                AttachmentFace south = face(helper, Direction.SOUTH);
                ItemStack stack = new ItemStack(id, 2);
                Appearance look = AppearanceStacks.read(stack);
                check(AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, bandHit(helper, player, arch.profile(), Direction.SOUTH),
                        sneaking).consumesAction(), what + ": not handled");
                check(entity(helper, AT).trims().equals(FaceTrims.NONE.with(south, look)) && stack.getCount() == 1,
                        what + ": fitted " + entity(helper, AT).trims() + ", holding " + stack);
                check(helper.getBlockState(AT.south()).isAir(), what + ": something was placed");
                player.getInventory().clearContent();
                ItemStack shears = new ItemStack(Items.SHEARS);
                AppearanceFixtures.use(helper, player, shears, ItemStack.EMPTY, bandHit(helper, player, arch.profile(), Direction.SOUTH), sneaking);
                check(!entity(helper, AT).trims().any() && returned(player, look) == 1 && shears.getDamageValue() == 1,
                        what + ": shears did not give back one Universal Trim in " + look.key());
            }
        }
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    /** Donors on the band restyle that face's trim only, no sneak needed; plain planks elsewhere restyle the frame; masonry elsewhere is refused. */
    @GameTest(template = TEMPLATE)
    public static void donorsRestyleOnlyTheAimedFace(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_style", GameType.SURVIVAL, Direction.NORTH);
        WindowDesign round = WindowDesign.plain(WindowProfile.ROUND);
        io.github.dirtyredz.roundwood.fixture.window.WindowFixtures.setWindow(helper, AT, round, SOUTH);
        check(WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.FRONT, Appearance.DEFAULT), "front");
        check(WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.BACK, Appearance.DEFAULT), "back");
        AttachmentFace south = face(helper, Direction.SOUTH);
        ItemStack bricks = new ItemStack(Items.BRICKS, 4);
        check(AppearanceFixtures.use(helper, player, bricks, ItemStack.EMPTY, bandHit(helper, player, round.profile(), Direction.SOUTH), false)
                .consumesAction(), "bricks on the band not handled");
        check(entity(helper, AT).trims().get(south).equals(Optional.of(BRICKS)) && bricks.getCount() == 3, "bricks: " + entity(helper, AT).trims());
        check(entity(helper, AT).trims().get(south.opposite()).equals(Optional.of(Appearance.DEFAULT)), "the other face changed");
        check(helper.getBlockState(AT.south()).isAir(), "a brick was placed instead");
        ItemStack dye = new ItemStack(Items.RED_DYE, 3);
        AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY, bandHit(helper, player, round.profile(), Direction.SOUTH), false);
        check(dye.getCount() == 3 && entity(helper, AT).trims().get(south).equals(Optional.of(BRICKS)), "dye painted masonry or was paid for");
        AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY, bandHit(helper, player, round.profile(), Direction.NORTH), false);
        check(entity(helper, AT).trims().get(south.opposite()).equals(Optional.of(AppearanceFixtures.oakPainted(DyeColor.RED)))
                && dye.getCount() == 2, "dye on the north band: " + entity(helper, AT).trims());
        ItemStack stone = new ItemStack(Items.STONE_BRICKS, 2);
        AppearanceFixtures.use(helper, player, stone, ItemStack.EMPTY, bandHit(helper, player, round.profile(), Direction.NORTH), true);
        check(entity(helper, AT).trims().get(south.opposite()).equals(Optional.of(STONE_BRICKS)), "sneaking does the same");
        // Off the band: plain planks restyle the frame; masonry is refused and not placed.
        ItemStack spruce = new ItemStack(Items.SPRUCE_PLANKS, 2);
        BlockHitResult frame = WindowRays.face(helper, player, AT, 1.5, 1.5, Direction.SOUTH);
        AppearanceFixtures.use(helper, player, spruce, ItemStack.EMPTY, frame, false);
        check(entity(helper, AT).appearance().equals(SPRUCE) && spruce.getCount() == 1, "plain planks on the frame: " + entity(helper, AT).appearance());
        check(entity(helper, AT).trims().get(south).equals(Optional.of(BRICKS)), "frame restyling touched the trim");
        ItemStack more = new ItemStack(Items.BRICKS, 2);
        AppearanceFixtures.use(helper, player, more, ItemStack.EMPTY, frame, false);
        check(more.getCount() == 2 && helper.getBlockState(AT.south()).isAir() && entity(helper, AT).appearance().equals(SPRUCE),
                "masonry on the frame was not refused");
        helper.succeed();
    }

    /** The offhand acts only with an empty main hand, for fitting as for restyling. */
    @GameTest(template = TEMPLATE)
    public static void theOffhandActsOnlyWithAnEmptyMainHand(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_offhand", GameType.SURVIVAL, Direction.NORTH);
        WindowDesign arch = WindowDesign.plain(WindowProfile.ARCH_LEFT);
        io.github.dirtyredz.roundwood.fixture.window.WindowFixtures.setWindow(helper, AT, arch, SOUTH);
        ItemStack offTrim = trims(SPRUCE, 1);
        AppearanceFixtures.use(helper, player, new ItemStack(Items.STICK), offTrim, bandHit(helper, player, arch.profile(), Direction.SOUTH), false);
        check(!entity(helper, AT).trims().any() && offTrim.getCount() == 1, "offhand trim fitted with a full main hand");
        AppearanceFixtures.use(helper, player, ItemStack.EMPTY, offTrim, bandHit(helper, player, arch.profile(), Direction.SOUTH), false);
        check(entity(helper, AT).trims().count() == 1 && offTrim.isEmpty(), "offhand trim with an empty main hand: " + entity(helper, AT).trims());
        helper.succeed();
    }

    /** Breaking a window with two different face trims drops the window and one trim per face, each in its own look. */
    @GameTest(template = TEMPLATE)
    public static void eachFaceDropsItsOwnTrim(GameTestHelper helper) {
        ServerPlayer player = player(helper, "trim_drop", GameType.SURVIVAL, Direction.NORTH);
        Appearance frame = AppearanceFixtures.oakPainted(DyeColor.MAGENTA);
        Appearance front = AppearanceFixtures.oakPainted(DyeColor.CYAN);
        Appearance back = AppearanceFixtures.oakPainted(DyeColor.LIME);
        WindowDesign design = new WindowDesign(WindowProfile.BOTTOM_LEFT, WindowPattern.CROSS);
        io.github.dirtyredz.roundwood.fixture.window.WindowFixtures.setWindow(helper, AT, design, SOUTH);
        entity(helper, AT).setAppearance(frame);
        WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.FRONT, front);
        WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.BACK, back);
        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(AT));
        check(dropped(helper, ModItems.window(WindowDesign.plain(design.profile())), frame) == 1, "window drop");
        check(dropped(helper, ModItems.ROUND_TRIM.get(), front) == 1 && dropped(helper, ModItems.ROUND_TRIM.get(), back) == 1, "trim drops");
        check(dropped(helper, ModItems.ROUND_TRIM.get()) == 2 && dropped(helper, ModItems.window(WindowDesign.plain(design.profile()))) == 1, "extra drops");
        clearDrops(helper);
        // One oak face: one plain oak trim.
        io.github.dirtyredz.roundwood.fixture.window.WindowFixtures.setWindow(helper, AT, design, SOUTH);
        WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.BACK, Appearance.DEFAULT);
        player.gameMode.destroyBlock(helper.absolutePos(AT));
        check(dropped(helper, ModItems.ROUND_TRIM.get(), Appearance.DEFAULT) == 1 && dropped(helper, ModItems.ROUND_TRIM.get()) == 1,
                "one oak face drops one oak trim");
        clearDrops(helper);
        ServerPlayer creative = player(helper, "trim_drop_creative", GameType.CREATIVE, Direction.NORTH);
        io.github.dirtyredz.roundwood.fixture.window.WindowFixtures.setWindow(helper, AT, design, SOUTH);
        WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.FRONT, front);
        creative.gameMode.destroyBlock(helper.absolutePos(AT));
        check(dropped(helper, ModItems.ROUND_TRIM.get()) == 0 && dropped(helper, ModItems.window(WindowDesign.plain(design.profile()))) == 0, "creative break dropped items");
        helper.succeed();
    }

    /** Each face's look saves under its own key and reloads; update tags always carry the payload so removals sync. */
    @GameTest(template = TEMPLATE)
    public static void faceTrimsSaveReloadAndSync(GameTestHelper helper) {
        helper.setBlock(AT, window(WindowDesign.plain(WindowProfile.ARCH_LEFT), SOUTH));
        RoundWindowBlockEntity window = entity(helper, AT);
        window.setAppearance(SPRUCE);
        WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.BACK, BRICKS);
        CompoundTag saved = window.saveWithFullMetadata();
        check(saved.getCompound("WindowTrim").getInt("Version") == 2 && saved.getCompound("WindowTrim").contains("Back")
                && !saved.getCompound("WindowTrim").contains("Front"), "saved " + saved);
        RoundWindowBlockEntity copy = new RoundWindowBlockEntity(window.getType(), helper.absolutePos(AT), helper.getBlockState(AT));
        copy.load(saved);
        check(copy.appearance().equals(SPRUCE) && copy.trims().equals(FaceTrims.NONE.with(AttachmentFace.BACK, BRICKS)), "reloaded " + copy.trims());
        WindowAttachmentState.store(helper.getLevel(), helper.absolutePos(AT), window, TrimSlot.PERIMETER, FaceTrims.NONE);
        CompoundTag update = window.getUpdateTag();
        check(update.contains("WindowTrim") && update.getCompound("WindowTrim").getInt("Version") == 2, "a removal must be sent: " + update);
        copy.handleUpdateTag(update);
        check(!copy.trims().any(), "client kept a removed trim");
        ItemStack pick = AppearanceFixtures.pick(helper, helper.absolutePos(AT));
        check(pick.is(ModBlocks.window(WindowDesign.plain(WindowProfile.ARCH_LEFT)).asItem()) && !pick.getOrCreateTag().contains("WindowTrim"), "pick " + pick);
        helper.succeed();
    }

    /** A 0.4.0 alias window with trims keeps both faces through its conversion. */
    @GameTest(template = TEMPLATE)
    public static void faceTrimsOnAnAliasWindowSurviveConversion(GameTestHelper helper) {
        WindowPaneBlock alias = io.github.dirtyredz.roundwood.migration.LegacyContent.windows().get(1);
        helper.setBlock(AT, alias.defaultBlockState());
        WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.FRONT, BRICKS);
        WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), AttachmentFace.BACK, SPRUCE);
        Appearance historical = alias.defaultAppearance();
        helper.runAfterDelay(5, () -> {
            BlockState state = helper.getBlockState(AT);
            check(state.is(ModBlocks.roundWindow()), "alias not converted: " + state);
            check(entity(helper, AT).trims().equals(new FaceTrims(Optional.of(BRICKS), Optional.of(SPRUCE))),
                    "trims after conversion: " + entity(helper, AT).trims());
            check(WindowPaneBlock.trimmed(state) && !state.getValue(BlockStateProperties.WATERLOGGED), "state " + state);
            check(entity(helper, AT).appearance().equals(historical), "frame look after conversion");
            helper.succeed();
        });
    }
}
