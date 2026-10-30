package io.github.dirtyredz.roundwood.block.attachment.interaction;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceUseTarget;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimActions;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimActions;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.door.DoorFixtures;
import io.github.dirtyredz.roundwood.fixture.door.DoorRays;
import io.github.dirtyredz.roundwood.fixture.window.WindowRays;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Supplier;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.styled;

/**
 * Characterization of the held-item rules the archway, door and window routers share, pinned before
 * they are extracted into one classifier (acting hand, then attachment with its slot, then shears,
 * then a TRIM donor, then anything else). Every assertion goes through the real routers
 * ({@code BlockState.use} and the block's sneak-use {@code claimsUse}) with real rays, and checks the
 * resulting trims, look, stack and tool wear. Covered: the main hand suppresses the offhand on all
 * three hosts; a door's main hand passes to an offhand trim or restyling gesture; every Universal Trim
 * id fits an archway; the Arch Sill's different refusals; an open door's fit only on a ring target;
 * forge:shears-tagged tools (test tag: carrot on a stick) acting as shears on all three hosts;
 * donor stacks carrying contents or state are no donors; and the door's selection shape under a held
 * trim.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HeldTrimToolGameTests {
    @GameTest(template = TEMPLATE)
    public static void crossBeamsAreRefusedByDoors(GameTestHelper helper) {
        Scene door = door(helper, "beam_door", false);
        var stack = new ItemStack(ModItems.CROSS_BEAMS.get());
        door.hold(stack, ItemStack.EMPTY);
        check(!door.claims(MAIN) && door.use(MAIN) == InteractionResult.FAIL, "door accepted beams");
        check(stack.getCount() == 1 && !door.trims.get().any(), "door fitted or charged beams");
        helper.succeed();
    }

    /** Its own test: a door and an archway cannot share the anchor cell. */
    @GameTest(template = TEMPLATE)
    public static void crossBeamsAreRefusedByArchways(GameTestHelper helper) {
        var stack = new ItemStack(ModItems.CROSS_BEAMS.get());
        Scene archway = archway(helper, "beam_arch", false);
        archway.hold(stack, ItemStack.EMPTY);
        check(archway.claims(MAIN) && archway.use(MAIN).consumesAction(), "archway did not claim refusal");
        check(stack.getCount() == 1 && !archway.trims.get().any(), "archway fitted or charged beams");
        helper.succeed();
    }
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final WindowPose SOUTH = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
    /** Across the archway's left jamb face (half a pixel in), and up. */
    private static final double JAMB_A = 0.5 / 16;
    private static final double JAMB_H = 0.5;
    /** A pixel on the door ring's bottom (across 16.5 px, up 1.5 px). */
    private static final double RING_A = 16.5 / 16;
    private static final double RING_H = 1.5 / 16;
    /** A trim-band pixel of the round window. */
    private static final double[] BAND = {8.5, 0.5};
    private static final InteractionHand MAIN = InteractionHand.MAIN_HAND;
    private static final InteractionHand OFF = InteractionHand.OFF_HAND;

    private HeldTrimToolGameTests() {
    }

    /** One host with a player aiming at its trim target; the aim is recomputed per click (installed trims move the ray's hit). */
    private record Scene(GameTestHelper helper, ServerPlayer player, Supplier<BlockHitResult> aim, Supplier<FaceTrims> trims,
                         Supplier<Appearance> look) {
        void hold(ItemStack main, ItemStack off) {
            DoorFixtures.hold(player, main, off);
        }

        boolean claims(InteractionHand hand) {
            BlockHitResult hit = aim.get();
            BlockState state = helper.getLevel().getBlockState(hit.getBlockPos());
            return ((AppearanceUseTarget) state.getBlock()).claimsUse(state, helper.getLevel(), hit.getBlockPos(), player, hand, hit);
        }

        InteractionResult use(InteractionHand hand) {
            BlockHitResult hit = aim.get();
            return io.github.dirtyredz.roundwood.fixture.BlockUses.use(helper.getLevel().getBlockState(hit.getBlockPos()), helper.getLevel(), player, hand, hit);
        }
    }

    private static io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout.Assembly write(
            GameTestHelper helper, BlockPos anchor, Direction facing, ArchwayDesign ignored, Appearance look) {
        var piece = new io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout.Assembly(helper.absolutePos(anchor), facing,
                io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign.TOP_1X2,
                io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.LEFT);
        check(io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement.placeAt(helper.getLevel(),
                ModBlocks.archPiece(piece.design()), piece, look), "arch top placement refused");
        return piece;
    }
    private static io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity anchorEntity(
            GameTestHelper helper, io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout.Assembly piece) {
        return (io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity) helper.getLevel().getBlockEntity(piece.anchor());
    }
    private static BlockHitResult through(GameTestHelper helper, ServerPlayer player,
            io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout.Assembly piece, double across, double up, boolean front) {
        return io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.through(helper, player,
                new ArchwayLayout.Assembly(piece.anchor(), piece.facing(), ArchwayDesign.ORDINARY), across, up, front);
    }

    private static Scene archway(GameTestHelper helper, String name, boolean installed) {
        floor(helper);
        io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.ORDINARY, Appearance.DEFAULT);
        if (installed) {
            check(io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimActions.install(helper.getLevel(), archway.anchor(), AttachmentFace.FRONT, Appearance.DEFAULT), "casing not installed");
        }
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        return new Scene(helper, player, () -> through(helper, player, archway, JAMB_A, JAMB_H, true),
                () -> anchorEntity(helper, archway).trims(), () -> anchorEntity(helper, archway).appearance());
    }

    private static DoorLayout.Assembly placedDoor(GameTestHelper helper, String name) {
        floor(helper);
        ServerPlayer placer = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), name + ": placement failed");
        return assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT);
    }

    private static Scene door(GameTestHelper helper, String name, boolean installed) {
        DoorLayout.Assembly door = placedDoor(helper, name);
        if (installed) {
            check(DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.FRONT, Appearance.DEFAULT), "ring not installed");
        }
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        return new Scene(helper, player, () -> DoorRays.through(helper, player, door, RING_A, RING_H, true),
                () -> DoorFixtures.anchorEntity(helper, door).trims(), () -> DoorFixtures.anchorEntity(helper, door).appearance());
    }

    private static RoundWindowBlockEntity windowEntity(GameTestHelper helper) {
        return (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(AT));
    }

    /** A round window; installed gives it a trim on both faces, so removing one leaves a count of one. */
    private static Scene window(GameTestHelper helper, String name, boolean installed) {
        helper.setBlock(AT, ShowcaseWindow.of(WindowDesign.plain(WindowProfile.ROUND), SOUTH, 0, 0, 0, Appearance.DEFAULT).state());
        if (installed) {
            for (AttachmentFace face : AttachmentFace.values()) {
                check(WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), face, Appearance.DEFAULT), "trim not installed");
            }
        }
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        return new Scene(helper, player, () -> WindowRays.face(helper, player, AT, BAND[0], BAND[1], Direction.SOUTH),
                () -> windowEntity(helper).trims(), () -> windowEntity(helper).appearance());
    }

    private static ItemStack trim(Appearance look, int count) {
        ItemStack stack = styled(ModItems.ROUND_TRIM.get(), look);
        stack.setCount(count);
        return stack;
    }

    private static ItemStack sill() {
        return styled(ModItems.ARCH_SILL_TRIM.get(), BRICKS);
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

    // ---- 1. main hand versus offhand ------------------------------------------------------------------------------

    /**
     * An occupied main hand suppresses an offhand shears, donor and trim (not claimed; a non-door host passes);
     * an empty main hand lets the offhand shears take the trim off and then fit a trim from the offhand.
     */
    private static void offhandPriority(Scene s, boolean directUse) {
        FaceTrims installed = s.trims.get();
        Appearance look = s.look.get();
        ItemStack shears = new ItemStack(Items.SHEARS);
        ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 2);
        ItemStack offTrim = trim(SPRUCE, 2);
        for (ItemStack off : List.of(shears, planks, offTrim)) {
            s.hold(new ItemStack(Items.STICK), off);
            check(!s.claims(OFF), off + ": claimed with an occupied main hand");
            if (directUse) {
                check(s.use(OFF) == InteractionResult.PASS, off + ": the offhand did not pass with an occupied main hand");
            }
            check(s.trims.get().equals(installed) && s.look.get().equals(look), off + ": something changed");
        }
        check(shears.getDamageValue() == 0 && planks.getCount() == 2 && offTrim.getCount() == 2, "an offhand item was used or worn");
        s.hold(ItemStack.EMPTY, shears);
        check(s.claims(OFF) && s.use(OFF).consumesAction(), "the offhand shears did not act with an empty main hand");
        check(s.trims.get().count() == installed.count() - 1 && shears.getDamageValue() == 1, "offhand shears: " + s.trims.get());
        s.hold(ItemStack.EMPTY, offTrim);
        check(s.claims(OFF) && s.use(OFF).consumesAction(), "the offhand trim did not act with an empty main hand");
        check(s.trims.get().count() == installed.count() && offTrim.getCount() == 1, "offhand trim: " + s.trims.get() + ", holding " + offTrim);
    }

    @GameTest(template = TEMPLATE)
    public static void archwayMainHandOccupiedSuppressesTheOffhand(GameTestHelper helper) {
        offhandPriority(archway(helper, "arch_priority", true), true);
        helper.succeed();
    }

    /** On a door the occupied main hand's click toggles it (not asserted here); only the claim and the unchanged ring are pinned. */
    @GameTest(template = TEMPLATE)
    public static void doorMainHandOccupiedSuppressesTheOffhand(GameTestHelper helper) {
        offhandPriority(door(helper, "door_priority", true), false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void windowMainHandOccupiedSuppressesTheOffhand(GameTestHelper helper) {
        offhandPriority(window(helper, "window_priority", true), true);
        helper.succeed();
    }

    // ---- 2. a door's main hand passes to the offhand ----------------------------------------------------------------

    /**
     * With an empty main hand, an offhand Universal Trim, Arch Sill, sneak-planks or dye makes the main-hand use PASS
     * (the offhand's turn) without toggling; plain planks in the offhand are no gesture, so the main hand toggles, as
     * it does with an empty offhand.
     */
    @GameTest(template = TEMPLATE)
    public static void doorMainHandPassesToAnOffhandTrimOrGesture(GameTestHelper helper) {
        DoorLayout.Assembly door = placedDoor(helper, "door_pass");
        ServerPlayer player = player(helper, "door_pass", GameType.SURVIVAL, Direction.NORTH);
        BlockHitResult leaf = AppearanceFixtures.leaf(door, false);
        for (ItemStack off : List.of(trim(SPRUCE, 1), sill(), new ItemStack(Items.RED_DYE))) {
            DoorFixtures.hold(player, ItemStack.EMPTY, off);
            check(useAt(helper, player, leaf) == InteractionResult.PASS, off + ": the main hand did not pass");
            check(!open(helper, door), off + ": the door toggled");
        }
        DoorFixtures.hold(player, ItemStack.EMPTY, new ItemStack(Items.SPRUCE_PLANKS));
        player.setShiftKeyDown(true);
        check(useAt(helper, player, leaf) == InteractionResult.PASS && !open(helper, door),
                "sneaking offhand planks (a restyle gesture) did not pass");
        player.setShiftKeyDown(false);
        check(!DoorFixtures.anchorEntity(helper, door).trims().any(), "a trim was fitted by a pass");
        check(useAt(helper, player, leaf).consumesAction() && open(helper, door),
                "plain offhand planks are no gesture: the main hand should toggle the door open");
        DoorFixtures.hold(player, ItemStack.EMPTY, ItemStack.EMPTY);
        check(useAt(helper, player, leaf).consumesAction() && !open(helper, door),
                "an empty offhand: the main hand should toggle the door closed");
        helper.succeed();
    }

    /** The main-hand use on the cell's current state (a toggled door has new states). */
    private static InteractionResult useAt(GameTestHelper helper, ServerPlayer player, BlockHitResult hit) {
        return io.github.dirtyredz.roundwood.fixture.BlockUses.use(helper.getLevel().getBlockState(hit.getBlockPos()), helper.getLevel(), player, MAIN, hit);
    }

    private static boolean open(GameTestHelper helper, DoorLayout.Assembly door) {
        return helper.getLevel().getBlockState(door.anchor()).getValue(DoorBlock.OPEN);
    }

    // ---- 3. legacy trim ids ------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void everyUniversalTrimIdFitsAnArchwayFace(GameTestHelper helper) {
        floor(helper);
        for (Item id : AppearanceFixtures.universalTrimIds()) {
            io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.ORDINARY, Appearance.DEFAULT);
            ServerPlayer player = player(helper, "arch_ids", GameType.SURVIVAL, Direction.NORTH);
            ItemStack stack = new ItemStack(id, 2);
            Appearance look = AppearanceStacks.read(stack);
            BlockHitResult hit = through(helper, player, archway, JAMB_A, JAMB_H, true);
            check(AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, hit, false).consumesAction(), id + ": not handled");
            check(anchorEntity(helper, archway).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, look)) && stack.getCount() == 1,
                    id + ": fitted " + anchorEntity(helper, archway).trims() + ", holding " + stack);
            check(anchorEntity(helper, archway).appearance().equals(Appearance.DEFAULT), id + ": the archway changed");
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    // ---- 4. the Arch Sill on hosts that cannot take it -------------------------------------------------------------

    /** A door does not claim the sill and fails the click (its fit face is empty); nothing is used or changed. */
    @GameTest(template = TEMPLATE)
    public static void archSillOnADoorIsNotClaimedAndFails(GameTestHelper helper) {
        Scene s = door(helper, "sill_door", false);
        ItemStack sill = sill();
        s.hold(sill, ItemStack.EMPTY);
        check(!s.claims(MAIN), "a door claimed the arch sill");
        check(s.use(MAIN) == InteractionResult.FAIL, "the door's use of the arch sill is not FAIL");
        check(sill.getCount() == 1 && !s.trims.get().any() && s.look.get().equals(Appearance.DEFAULT), "the sill was used or the door changed");
        helper.succeed();
    }

    /** An archway claims the sill and refuses it with a hint (CONSUME on the server); nothing is used. */
    @GameTest(template = TEMPLATE)
    public static void archSillOnAnArchwayIsClaimedAndRefused(GameTestHelper helper) {
        Scene s = archway(helper, "sill_arch", false);
        ItemStack sill = sill();
        s.hold(sill, ItemStack.EMPTY);
        check(s.claims(MAIN), "an archway did not claim the arch sill");
        check(s.use(MAIN) == InteractionResult.CONSUME, "the archway's refusal is not CONSUME");
        check(sill.getCount() == 1 && !s.trims.get().any() && s.look.get().equals(Appearance.DEFAULT), "the sill was used or the archway changed");
        helper.succeed();
    }

    /** A window whose profile takes no sill (round) claims the sill and refuses it (REFUSE_SILL, CONSUME); nothing is used. */
    @GameTest(template = TEMPLATE)
    public static void archSillOnAWindowWithoutSillIsClaimedAndRefused(GameTestHelper helper) {
        Scene s = window(helper, "sill_window", false);
        ItemStack sill = sill();
        s.hold(sill, ItemStack.EMPTY);
        check(s.claims(MAIN), "a round window did not claim the arch sill");
        check(s.use(MAIN) == InteractionResult.CONSUME, "the window's refusal is not CONSUME");
        check(sill.getCount() == 1 && !s.trims.get().any() && !windowEntity(helper).trims(TrimSlot.SILL).any(), "the sill was used or fitted");
        helper.succeed();
    }

    // ---- 5. an open door ---------------------------------------------------------------------------------------------

    /** An open door fits a held trim only on a ring target: the open doorway's middle fails and is not claimed. */
    @GameTest(template = TEMPLATE)
    public static void anOpenDoorFitsATrimOnlyOnARingTarget(GameTestHelper helper) {
        DoorLayout.Assembly door = placedDoor(helper, "open_fit");
        ServerPlayer player = player(helper, "open_fit", GameType.SURVIVAL, Direction.NORTH);
        check(AppearanceFixtures.use(helper, player, ItemStack.EMPTY, ItemStack.EMPTY, AppearanceFixtures.leaf(door, false), false)
                .consumesAction() && open(helper, door), "the door did not open");
        ItemStack stack = trim(SPRUCE, 2);
        DoorFixtures.hold(player, stack, ItemStack.EMPTY);
        BlockHitResult off = DoorFixtures.frontHit(door, 1.0, 1.0);
        BlockState state = helper.getLevel().getBlockState(off.getBlockPos());
        check(!claims(helper, player, state, off), "an open door claimed a trim aimed off the ring");
        check(io.github.dirtyredz.roundwood.fixture.BlockUses.use(state, helper.getLevel(), player, MAIN, off) == InteractionResult.FAIL, "an off-ring fit on an open door is not FAIL");
        check(!DoorFixtures.anchorEntity(helper, door).trims().any() && stack.getCount() == 2 && open(helper, door), "off-ring click changed things");
        BlockHitResult ring = DoorRays.through(helper, player, door, RING_A, RING_H, true);
        BlockState ringState = helper.getLevel().getBlockState(ring.getBlockPos());
        check(claims(helper, player, ringState, ring), "an open door did not claim a trim aimed at a ring target");
        check(io.github.dirtyredz.roundwood.fixture.BlockUses.use(ringState, helper.getLevel(), player, MAIN, ring).consumesAction(), "the ring-target fit was not handled");
        check(DoorFixtures.anchorEntity(helper, door).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, SPRUCE))
                && stack.getCount() == 1 && open(helper, door), "ring-target fit: " + DoorFixtures.anchorEntity(helper, door).trims());
        helper.succeed();
    }

    private static boolean claims(GameTestHelper helper, ServerPlayer player, BlockState state, BlockHitResult hit) {
        return ((AppearanceUseTarget) state.getBlock()).claimsUse(state, helper.getLevel(), hit.getBlockPos(), player, MAIN, hit);
    }

    // ---- 6. tagged shears --------------------------------------------------------------------------------------------

    /**
     * A tool tagged forge:shears (the test tag: carrot on a stick, damageable and no ShearsItem) takes the trim off
     * like real shears: claimed, the trim given back in survival, one point of wear.
     */
    private static void taggedShearsRemove(Scene s) {
        ItemStack tool = new ItemStack(Items.CARROT_ON_A_STICK);
        check(!(tool.getItem() instanceof ShearsItem) && tool.is(Tags.Items.TOOLS_SHEAR), "the test tag forge:shears is not loaded");
        int before = s.trims.get().count();
        s.hold(tool, ItemStack.EMPTY);
        check(s.claims(MAIN), "tagged shears were not claimed");
        check(s.use(MAIN).consumesAction(), "tagged shears were not handled");
        check(s.trims.get().count() == before - 1, "tagged shears left " + s.trims.get());
        check(returned(s.player, Appearance.DEFAULT) == 1, "no Universal Trim given back");
        check(tool.getDamageValue() == 1, "tool wear " + tool.getDamageValue());
    }

    @GameTest(template = TEMPLATE)
    public static void taggedShearsRemoveAnArchwayCasing(GameTestHelper helper) {
        taggedShearsRemove(archway(helper, "tag_arch", true));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void taggedShearsRemoveADoorRingTrim(GameTestHelper helper) {
        taggedShearsRemove(door(helper, "tag_door", true));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void taggedShearsRemoveAWindowTrim(GameTestHelper helper) {
        taggedShearsRemove(window(helper, "tag_window", true));
        helper.succeed();
    }

    // ---- 7. rejected donor stacks ----------------------------------------------------------------------------------

    /**
     * A plank stack carrying native block-entity data or blockstate overrides is no donor: not claimed and no restyle (a non-door host
     * passes). Plain planks restyle the installed trim and cost one.
     */
    private static void rejectedDonors(Scene s, boolean directUse) {
        FaceTrims before = s.trims.get();
        Appearance look = s.look.get();
        for (String key : List.of("block_entity_data", "block_state")) {
            ItemStack stack = new ItemStack(Items.SPRUCE_PLANKS, 2);
            if (key.equals("block_state")) stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                    net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X));
            else BlockItem.setBlockEntityData(stack, net.minecraft.world.level.block.entity.BlockEntityType.CHEST, new CompoundTag());
            s.hold(stack, ItemStack.EMPTY);
            check(!s.claims(MAIN), key + ": claimed as a donor");
            if (directUse) {
                check(s.use(MAIN) == InteractionResult.PASS, key + ": not a pass");
            }
            check(s.trims.get().equals(before) && s.look.get().equals(look) && stack.getCount() == 2, key + ": restyled or paid");
        }
        ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 2);
        s.hold(planks, ItemStack.EMPTY);
        check(s.claims(MAIN) && s.use(MAIN).consumesAction(), "real planks did not restyle");
        check(!s.trims.get().equals(before) && planks.getCount() == 1, "real planks: " + s.trims.get() + ", holding " + planks);
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedDonorStacksDoNotRestyleAnArchwayCasing(GameTestHelper helper) {
        rejectedDonors(archway(helper, "donor_arch", true), true);
        helper.succeed();
    }

    /** On a door the rejected stack's plain click is the door's own business (not asserted); only the claim and the unchanged ring are pinned. */
    @GameTest(template = TEMPLATE)
    public static void rejectedDonorStacksDoNotRestyleADoorRing(GameTestHelper helper) {
        rejectedDonors(door(helper, "donor_door", true), false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedDonorStacksDoNotRestyleAWindowTrim(GameTestHelper helper) {
        rejectedDonors(window(helper, "donor_window", true), true);
        helper.succeed();
    }

    // ---- 8. the door's target shape under a held trim ---------------------------------------------------------------

    /** Whether a ray across the ring's footprint, from the front or the back, hits any cell's target shape as {@code player} sees it. */
    private static boolean reaches(GameTestHelper helper, ServerPlayer player, DoorLayout.Assembly door, boolean fromFront) {
        Vec3 from = DoorFixtures.point(door, RING_A, RING_H, fromFront ? -0.75 : 1.75);
        Vec3 to = DoorFixtures.point(door, RING_A, RING_H, fromFront ? 1.75 : -0.75);
        for (BlockPos cell : door.positions()) {
            if (helper.getLevel().getBlockState(cell).getShape(helper.getLevel(), cell, CollisionContext.of(player)).clip(from, to, cell) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * An open, untrimmed door: with nothing, or an offhand trim behind an occupied main hand, neither ring is a target;
     * a trim in the main hand, in the offhand with an empty main hand, or an Arch Sill (any attachment) makes both targets.
     */
    @GameTest(template = TEMPLATE)
    public static void aHeldTrimMakesBothRingsTargetableOnAnOpenDoor(GameTestHelper helper) {
        DoorLayout.Assembly door = placedDoor(helper, "shape");
        ServerPlayer player = player(helper, "shape", GameType.SURVIVAL, Direction.NORTH);
        check(AppearanceFixtures.use(helper, player, ItemStack.EMPTY, ItemStack.EMPTY, AppearanceFixtures.leaf(door, false), false)
                .consumesAction() && open(helper, door), "the door did not open");
        ItemStack stick = new ItemStack(Items.STICK);
        List<ItemStack[]> hands = List.of(
                new ItemStack[] {ItemStack.EMPTY, ItemStack.EMPTY}, new ItemStack[] {trim(SPRUCE, 1), ItemStack.EMPTY},
                new ItemStack[] {ItemStack.EMPTY, trim(SPRUCE, 1)}, new ItemStack[] {stick, trim(SPRUCE, 1)},
                new ItemStack[] {trim(SPRUCE, 1), stick}, new ItemStack[] {sill(), ItemStack.EMPTY});
        List<Boolean> targeted = List.of(false, true, true, false, true, true);
        for (int i = 0; i < hands.size(); i++) {
            DoorFixtures.hold(player, hands.get(i)[0], hands.get(i)[1]);
            String what = "main " + hands.get(i)[0] + ", off " + hands.get(i)[1];
            check(reaches(helper, player, door, true) == targeted.get(i), what + ": front ring target " + !targeted.get(i));
            check(reaches(helper, player, door, false) == targeted.get(i), what + ": back ring target " + !targeted.get(i));
        }
        helper.succeed();
    }
}
