package io.github.dirtyredz.roundwood.block.window.trim;

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
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowRays;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.STONE_BRICKS;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/**
 * Arch sills (0.10.0) through the production interaction path, with hits from real rays
 * ({@link WindowRays}): on both upper arches, plain and crossed, in centred, flush, floor and ceiling
 * poses, an arch sill fits the clicked face (plainly or sneaking), takes its own restyle and dye
 * without touching the glass or the perimeter trim beside it, and shears take off only the sill. Other
 * windows refuse it for free. A window with two trims and two sills drops five items; a sill-only
 * window saves an explicit empty perimeter payload, so reloading never invents the 0.7.0 oak trim; an
 * unknown sill version blocks only sill edits; pick-block gives the pane alone.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowSillGameTests {
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final WindowPose SOUTH = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
    private static final List<WindowPose> POSES = List.of(SOUTH,
            WindowPose.upright(Direction.SOUTH, WindowDepth.NEGATIVE),
            WindowPose.upright(Direction.SOUTH, WindowDepth.POSITIVE),
            new WindowPose(Direction.UP, Direction.NORTH, WindowDepth.NEGATIVE),
            new WindowPose(Direction.DOWN, Direction.NORTH, WindowDepth.POSITIVE));
    /** A sill pixel of both upper arches (left glazes u 2..16, right 0..14, along the bottom rows). */
    private static final double[] SILL = {10.5, 0.5};
    /** A perimeter band pixel of each upper arch, beside the sill. */
    private static final double[] LEFT_BAND = {0.5, 0.5};
    private static final double[] RIGHT_BAND = {15.5, 0.5};

    private WindowSillGameTests() {
    }

    private static BlockState window(WindowDesign design, WindowPose pose) {
        return ShowcaseWindow.of(design, pose, 0, 0, 0, Appearance.DEFAULT).state();
    }

    private static RoundWindowBlockEntity entity(GameTestHelper helper) {
        return (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(AT));
    }

    private static AttachmentFace face(GameTestHelper helper, Direction side) {
        BlockState state = helper.getBlockState(AT);
        return WindowFaceIdentity.face(((WindowPaneBlock) state.getBlock()).pose(state), WindowPaneBlock.faceReversed(state), side);
    }

    private static BlockHitResult hit(GameTestHelper helper, ServerPlayer player, double[] at, Direction side) {
        return WindowRays.face(helper, player, AT, at[0], at[1], side);
    }

    private static ItemStack sills(Appearance look, int count) {
        ItemStack stack = styled(ModItems.ARCH_SILL_TRIM.get(), look);
        stack.setCount(count);
        return stack;
    }

    private static int returned(ServerPlayer player, Appearance look) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.ARCH_SILL_TRIM.get()) && AppearanceStacks.read(stack).equals(look)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void upperArchSillsFitRestyleAndComeOffOnTheirOwn(GameTestHelper helper) {
        ServerPlayer player = player(helper, "sill_faces", GameType.SURVIVAL, Direction.NORTH);
        for (WindowProfile profile : List.of(WindowProfile.ARCH_LEFT, WindowProfile.ARCH_RIGHT)) {
            double[] band = profile == WindowProfile.ARCH_LEFT ? LEFT_BAND : RIGHT_BAND;
            for (WindowPattern pattern : WindowPattern.values()) {
                for (WindowPose pose : POSES) {
                    WindowDesign design = new WindowDesign(profile, pattern);
                    helper.setBlock(AT, Blocks.AIR); // a new window, not the last one restated (its entity would keep its attachments)
                    io.github.dirtyredz.roundwood.fixture.WindowFixtures.setWindow(helper, AT, design, pose);
                    entity(helper).setGlass(Optional.of(DyeColor.LIGHT_BLUE));
                    String what = design.id() + " " + pose;
                    Direction positive = Direction.fromAxisAndDirection(pose.normal(), Direction.AxisDirection.POSITIVE);
                    Direction negative = positive.getOpposite();
                    AttachmentFace plus = face(helper, positive);
                    boolean sneaking = pose.depth() == WindowDepth.POSITIVE;
                    ItemStack spruce = sills(SPRUCE, 2);
                    check(AppearanceFixtures.use(helper, player, spruce, ItemStack.EMPTY, hit(helper, player, SILL, positive), sneaking)
                            .consumesAction(), what + ": sill fit not handled");
                    FaceTrims after = entity(helper).trims(TrimSlot.SILL);
                    check(after.get(plus).equals(Optional.of(SPRUCE)) && after.count() == 1 && spruce.getCount() == 1,
                            what + ": first sill " + after + ", holding " + spruce);
                    check(!entity(helper).trims().any(), what + ": a sill fitted a perimeter trim");
                    check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), what + ": trimmed summary not set by a sill");
                    AppearanceFixtures.use(helper, player, spruce, ItemStack.EMPTY, hit(helper, player, SILL, positive), sneaking);
                    check(spruce.getCount() == 1 && entity(helper).trims(TrimSlot.SILL).count() == 1, what + ": occupied sill took another");
                    // a perimeter trim beside the sill on the same face, in another look
                    ItemStack trim = styled(ModItems.ROUND_TRIM.get(), STONE_BRICKS);
                    AppearanceFixtures.use(helper, player, trim, ItemStack.EMPTY, hit(helper, player, band, positive), false);
                    check(entity(helper).trims().get(plus).equals(Optional.of(STONE_BRICKS)), what + ": trim beside the sill");
                    // the other face's sill, then a dye on this face's sill: the sill is painted, the glass and trim are not
                    AppearanceFixtures.use(helper, player, sills(BRICKS, 1), ItemStack.EMPTY, hit(helper, player, SILL, negative), false);
                    check(entity(helper).trims(TrimSlot.SILL).get(plus.opposite()).equals(Optional.of(BRICKS)), what + ": back sill");
                    ItemStack dye = new ItemStack(Items.RED_DYE, 2);
                    AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY, hit(helper, player, SILL, positive), false);
                    check(entity(helper).trims(TrimSlot.SILL).get(plus).equals(Optional.of(SPRUCE.painted(DyeColor.RED))) && dye.getCount() == 1,
                            what + ": dye on the sill " + entity(helper).trims(TrimSlot.SILL));
                    check(entity(helper).glass().equals(Optional.of(DyeColor.LIGHT_BLUE)), what + ": the sill's dye tinted the glass");
                    check(entity(helper).trims().get(plus).equals(Optional.of(STONE_BRICKS)), what + ": the sill's dye reached the trim");
                    // shears on the sill take only the sill, giving back an Arch Sill
                    player.getInventory().clearContent();
                    ItemStack shears = new ItemStack(Items.SHEARS);
                    AppearanceFixtures.use(helper, player, shears, ItemStack.EMPTY, hit(helper, player, SILL, positive), false);
                    check(entity(helper).trims(TrimSlot.SILL).get(plus).isEmpty() && entity(helper).trims().get(plus).isPresent()
                                    && entity(helper).trims(TrimSlot.SILL).get(plus.opposite()).isPresent(),
                            what + ": shears took " + entity(helper).trims(TrimSlot.SILL) + " / " + entity(helper).trims());
                    check(returned(player, SPRUCE.painted(DyeColor.RED)) == 1 && shears.getDamageValue() == 1, what + ": sill refund/wear");
                }
            }
        }
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    /** Every other window refuses an arch sill: nothing fitted, nothing used, nothing placed. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void otherWindowsRefuseTheSill(GameTestHelper helper) {
        ServerPlayer player = player(helper, "sill_refused", GameType.SURVIVAL, Direction.NORTH);
        for (WindowDesign design : WindowDesign.all()) {
            if (design.profile().takesSill() || design.pattern() == io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern.CROSS) {
                continue;
            }
            io.github.dirtyredz.roundwood.fixture.WindowFixtures.setWindow(helper, AT, design, SOUTH);
            ItemStack sill = sills(BRICKS, 1);
            AppearanceFixtures.use(helper, player, sill, ItemStack.EMPTY, WindowRays.face(helper, player, AT, 8, 8, Direction.SOUTH), false);
            check(sill.getCount() == 1 && !entity(helper).trims(TrimSlot.SILL).any() && !WindowPaneBlock.trimmed(helper.getBlockState(AT)),
                    design.id() + ": took a sill");
            check(helper.getBlockState(AT.south()).isAir(), design.id() + ": something was placed");
        }
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    /** Two trims and two sills: the window and four attachments drop, each in its own look; creative drops nothing. */
    @GameTest(template = TEMPLATE)
    public static void twoTrimsTwoSillsAndBeamsDropSixItems(GameTestHelper helper) {
        ServerPlayer player = player(helper, "sill_drop", GameType.SURVIVAL, Direction.NORTH);
        Appearance frame = AppearanceFixtures.oakPainted(DyeColor.BROWN);
        Appearance trimFront = AppearanceFixtures.oakPainted(DyeColor.PURPLE);
        Appearance trimBack = AppearanceFixtures.oakPainted(DyeColor.PINK);
        Appearance sillFront = AppearanceFixtures.oakPainted(DyeColor.ORANGE);
        Appearance sillBack = AppearanceFixtures.oakPainted(DyeColor.GRAY);
        WindowDesign design = new WindowDesign(WindowProfile.ARCH_RIGHT, WindowPattern.CROSS);
        io.github.dirtyredz.roundwood.fixture.WindowFixtures.setWindow(helper, AT, design, SOUTH);
        entity(helper).setAppearance(frame);
        BlockPos abs = helper.absolutePos(AT);
        check(WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.PERIMETER, AttachmentFace.FRONT, trimFront)
                && WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.PERIMETER, AttachmentFace.BACK, trimBack)
                && WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.SILL, AttachmentFace.FRONT, sillFront)
                && WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.SILL, AttachmentFace.BACK, sillBack), "installs");
        ItemStack pick = AppearanceFixtures.pick(helper, abs);
        check(pick.is(ModItems.window(WindowDesign.plain(design.profile()))) && AppearanceStacks.read(pick).equals(frame) && !pick.getOrCreateTag().contains("WindowSillTrim"),
                "pick-block gives the pane only: " + pick);
        clearDrops(helper);
        player.gameMode.destroyBlock(abs);
        check(dropped(helper, ModItems.window(WindowDesign.plain(design.profile())), frame) == 1, "window drop");
        check(dropped(helper, ModItems.ROUND_TRIM.get(), trimFront) == 1 && dropped(helper, ModItems.ROUND_TRIM.get(), trimBack) == 1,
                "trim drops");
        check(dropped(helper, ModItems.ARCH_SILL_TRIM.get(), sillFront) == 1 && dropped(helper, ModItems.ARCH_SILL_TRIM.get(), sillBack) == 1,
                "sill drops");
        check(dropped(helper, ModItems.CROSS_BEAMS.get(), Appearance.DEFAULT) == 1, "whole pane beam drop");
        check(dropped(helper, ModItems.ARCH_SILL_TRIM.get()) == 2 && dropped(helper, ModItems.ROUND_TRIM.get()) == 2, "extra drops");
        clearDrops(helper);
        ServerPlayer creative = player(helper, "sill_drop_creative", GameType.CREATIVE, Direction.NORTH);
        io.github.dirtyredz.roundwood.fixture.WindowFixtures.setWindow(helper, AT, design, SOUTH);
        WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.SILL, AttachmentFace.FRONT, sillFront);
        creative.gameMode.destroyBlock(abs);
        check(dropped(helper, ModItems.ARCH_SILL_TRIM.get()) == 0, "creative break dropped a sill");
        helper.succeed();
    }

    /**
     * A sill-only window keeps writing its explicit empty perimeter payload, so its true {@code trimmed}
     * summary never reads as the 0.7.0 omitted-oak trim; the sill reloads and syncs; an unknown sill
     * version is kept, reads as none and refuses sill edits while the perimeter stays editable.
     */
    @GameTest(template = TEMPLATE)
    public static void sillOnlyWindowsSaveWithoutFalseUpliftAndUnknownSillsFailClosed(GameTestHelper helper) {
        ServerPlayer player = player(helper, "sill_save", GameType.SURVIVAL, Direction.NORTH);
        helper.setBlock(AT, window(WindowDesign.plain(WindowProfile.ARCH_LEFT), SOUTH));
        BlockPos abs = helper.absolutePos(AT);
        RoundWindowBlockEntity window = entity(helper);
        check(WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.SILL, AttachmentFace.FRONT, BRICKS), "sill");
        check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), "a sill sets the summary");
        CompoundTag saved = window.saveWithFullMetadata();
        check(saved.getCompound("WindowTrim").getInt("Version") == 2 && !saved.getCompound("WindowTrim").contains("Front")
                && !saved.getCompound("WindowTrim").contains("Back"), "explicit empty perimeter payload: " + saved);
        check(saved.getCompound("WindowSillTrim").getInt("Version") == 1, "sill payload: " + saved);
        RoundWindowBlockEntity copy = new RoundWindowBlockEntity(window.getType(), abs, helper.getBlockState(AT));
        copy.load(saved);
        check(!copy.trims().any(), "a sill-only window reloaded with a perimeter trim: " + copy.trims());
        check(copy.trims(TrimSlot.SILL).equals(FaceTrims.NONE.with(AttachmentFace.FRONT, BRICKS)), "sill reloaded " + copy.trims(TrimSlot.SILL));
        WindowAttachmentState.store(helper.getLevel(), abs, window, TrimSlot.SILL, FaceTrims.NONE);
        check(!WindowPaneBlock.trimmed(helper.getBlockState(AT)), "summary cleared with the last sill");
        copy.handleUpdateTag(window.getUpdateTag());
        check(!copy.trims(TrimSlot.SILL).any(), "client kept a removed sill");

        CompoundTag future = window.saveWithFullMetadata();
        CompoundTag payload = new CompoundTag();
        payload.putInt("Version", 9);
        payload.put("Front", new CompoundTag());
        future.put("WindowSillTrim", payload);
        window.load(future);
        check(!window.trims(TrimSlot.SILL).any() && !window.trimsEditable(TrimSlot.SILL) && window.trimsEditable(), "unknown sill custody");
        ItemStack sill = sills(SPRUCE, 1);
        AppearanceFixtures.use(helper, player, sill, ItemStack.EMPTY, hit(helper, player, SILL, Direction.SOUTH), false);
        check(sill.getCount() == 1, "a sill was fitted over an unknown payload");
        check(WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.PERIMETER, AttachmentFace.FRONT, SPRUCE),
                "the perimeter stays editable beside an unknown sill");
        check(window.saveWithFullMetadata().getCompound("WindowSillTrim").equals(payload), "unknown sill payload not kept");
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    /** Shears take the last sill and then the last trim through real clicks: only the last attachment clears {@code trimmed}. */
    @GameTest(template = TEMPLATE)
    public static void removingTheLastAttachmentClearsTheTrimmedSummary(GameTestHelper helper) {
        ServerPlayer player = player(helper, "sill_last", GameType.SURVIVAL, Direction.NORTH);
        helper.setBlock(AT, Blocks.AIR);
        helper.setBlock(AT, window(WindowDesign.plain(WindowProfile.ARCH_LEFT), SOUTH));
        BlockPos abs = helper.absolutePos(AT);
        check(!WindowPaneBlock.trimmed(helper.getBlockState(AT)), "a fresh window is trimmed");
        check(WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.SILL, face(helper, Direction.SOUTH), BRICKS), "sill");
        check(WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.PERIMETER, face(helper, Direction.SOUTH), SPRUCE), "trim");
        check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), "attachments did not set the summary");

        AppearanceFixtures.use(helper, player, new ItemStack(Items.SHEARS), ItemStack.EMPTY, hit(helper, player, SILL, Direction.SOUTH), false);
        check(!entity(helper).trims(TrimSlot.SILL).any() && entity(helper).trims().any(), "shears did not take only the sill");
        check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), "the summary cleared while a trim remained");

        AppearanceFixtures.use(helper, player, new ItemStack(Items.SHEARS), ItemStack.EMPTY, hit(helper, player, LEFT_BAND, Direction.SOUTH), false);
        check(!entity(helper).trims().any() && !entity(helper).trims(TrimSlot.SILL).any(), "shears did not take the last trim");
        check(!WindowPaneBlock.trimmed(helper.getBlockState(AT)), "the summary stayed set after the last attachment went");
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    /** A sill-only window is trimmed, saves an explicit empty perimeter payload, and syncs the sill alongside it. */
    @GameTest(template = TEMPLATE)
    public static void aSillOnlyWindowIsTrimmedAndSavesAnExplicitEmptyPerimeter(GameTestHelper helper) {
        ServerPlayer player = player(helper, "sill_only", GameType.SURVIVAL, Direction.NORTH);
        helper.setBlock(AT, Blocks.AIR);
        helper.setBlock(AT, window(WindowDesign.plain(WindowProfile.ARCH_RIGHT), SOUTH));
        ItemStack sill = sills(SPRUCE, 1);
        AppearanceFixtures.use(helper, player, sill, ItemStack.EMPTY, hit(helper, player, SILL, Direction.SOUTH), false);
        RoundWindowBlockEntity window = entity(helper);
        check(sill.getCount() == 0 && window.trims(TrimSlot.SILL).count() == 1 && !window.trims().any(), "sill fit " + window.trims(TrimSlot.SILL));
        check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), "a sill-only window is not trimmed");
        CompoundTag saved = window.saveWithoutMetadata();
        check(saved.getCompound("WindowTrim").getInt("Version") == 2 && saved.getCompound("WindowTrim").getAllKeys().size() == 1,
                "perimeter payload " + saved.getCompound("WindowTrim"));
        check(saved.getCompound("WindowSillTrim").contains("Front"), "sill payload " + saved);
        CompoundTag update = window.getUpdateTag();
        check(update.getCompound("WindowTrim").getInt("Version") == 2 && update.getCompound("WindowSillTrim").contains("Front"),
                "update tag " + update);
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    /** While the perimeter payload is of an unknown version, sill edits succeed but never reconcile {@code trimmed}. */
    @GameTest(template = TEMPLATE)
    public static void sillEditsBesideAnUnknownPerimeterDoNotReconcileTheSummary(GameTestHelper helper) {
        unknownPayloadLeavesTheSummaryAlone(helper, "WindowTrim", 99, TrimSlot.SILL);
        helper.succeed();
    }

    /** While the sill payload is of an unknown version, perimeter edits succeed but never reconcile {@code trimmed}. */
    @GameTest(template = TEMPLATE)
    public static void perimeterEditsBesideAnUnknownSillDoNotReconcileTheSummary(GameTestHelper helper) {
        unknownPayloadLeavesTheSummaryAlone(helper, "WindowSillTrim", 99, TrimSlot.PERIMETER);
        helper.succeed();
    }

    /**
     * Holds {@code key} at an unknown {@code version}, then edits {@code edited} (the other slot) through the production
     * actions: untrimmed state, install: stays untrimmed; trimmed state, remove: stays trimmed.
     */
    private static void unknownPayloadLeavesTheSummaryAlone(GameTestHelper helper, String key, int version, TrimSlot edited) {
        helper.setBlock(AT, Blocks.AIR);
        helper.setBlock(AT, window(WindowDesign.plain(WindowProfile.ARCH_LEFT), SOUTH));
        BlockPos abs = helper.absolutePos(AT);
        RoundWindowBlockEntity window = entity(helper);
        CompoundTag future = window.saveWithFullMetadata();
        CompoundTag payload = new CompoundTag();
        payload.putInt("Version", version);
        payload.putString("Mystery", "kept");
        future.put(key, payload);
        window.load(future);
        check(!window.attachmentsEditable(), key + ": the unknown payload is not held");
        check(!WindowPaneBlock.trimmed(helper.getBlockState(AT)), key + ": loading changed the summary");

        AttachmentFace front = face(helper, Direction.SOUTH);
        check(WindowTrimActions.install(helper.getLevel(), abs, edited, front, SPRUCE), key + ": " + edited + " install refused");
        check(window.trims(edited).count() == 1, key + ": " + edited + " not fitted");
        check(!WindowPaneBlock.trimmed(helper.getBlockState(AT)), key + ": the summary was reconciled to set while a payload is unknown");

        helper.getLevel().setBlock(abs, helper.getBlockState(AT).setValue(WindowPaneBlock.TRIMMED, true), 3);
        check(entity(helper) == window, key + ": the entity was replaced by a state change");
        check(WindowAttachmentState.store(helper.getLevel(), abs, window, edited, FaceTrims.NONE), key + ": " + edited + " removal refused");
        check(!window.trims(edited).any(), key + ": " + edited + " not removed");
        check(WindowPaneBlock.trimmed(helper.getBlockState(AT)), key + ": the summary was reconciled to clear while a payload is unknown");
        check(window.saveWithFullMetadata().getCompound(key).equals(payload), key + ": the unknown payload was not kept");
        helper.setBlock(AT, Blocks.AIR);
    }
}
