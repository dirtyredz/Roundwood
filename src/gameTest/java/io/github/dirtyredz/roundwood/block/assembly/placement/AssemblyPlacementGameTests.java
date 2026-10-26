package io.github.dirtyredz.roundwood.block.assembly.placement;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.fixture.ArchPieceFixtures;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPlacement;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import io.github.dirtyredz.roundwood.fixture.ArchwayFixtures;
import io.github.dirtyredz.roundwood.fixture.DoorFixtures;
import io.github.dirtyredz.roundwood.fixture.TrimFixtures;
import io.github.dirtyredz.roundwood.fixture.assembly.DenyingPlayer;
import io.github.dirtyredz.roundwood.fixture.assembly.PlacementEffects;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/**
 * Whole-assembly placement as every family does it (characterization before the shared placement
 * skeleton, kept after it): a player refused at any one cell, anchor or not, places nothing and pays
 * nothing; a successful player placement installs the stack's look before its sound and its
 * {@code BLOCK_PLACE} game event, and charges one item (none in creative) only after both; a
 * player-less placement is silent and free; and what is written is exactly the plan's states, for the
 * item's plan and the player-less plan alike.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssemblyPlacementGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    /** How deep {@link AssemblyMutationBatch} lets batches nest (its {@code MAX_DEPTH}). */
    private static final int BATCH_DEPTH = 4;
    private static final Appearance LOOK = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "dark_oak_planks"));

    /** One family placed by a player with an item: its name, its item and its cells when anchored at {@link #ANCHOR}. */
    private enum Family {
        ROUND_DOOR(null), ARCHED_DOOR(null),
        TOP_1X1(ArchPieceDesign.TOP_1X1), TOP_1X2(ArchPieceDesign.TOP_1X2),
        TOP_2X3(ArchPieceDesign.TOP_2X3), SHOULDER(ArchPieceDesign.KEYHOLE_SHOULDER);

        private final ArchPieceDesign piece;
        Family(ArchPieceDesign piece) { this.piece = piece; }

        Item item() {
            if (piece != null) return ModBlocks.archPiece(piece).asItem();
            return this == ROUND_DOOR ? ModBlocks.roundDoor().asItem() : ModBlocks.archedDoor().asItem();
        }

        List<BlockPos> cells(GameTestHelper helper) {
            if (piece != null) return ArchPieceFixtures.assembly(helper, ANCHOR, Direction.NORTH, piece, Side.LEFT).positions();
            return door(helper, this == ROUND_DOOR ? DoorDesign.ROUND : DoorDesign.ARCHED).positions();
        }

        InteractionResult place(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
            return piece != null ? ArchPieceFixtures.place(helper, player, stack, ANCHOR, Side.LEFT)
                    : DoorFixtures.placeDoor(helper, player, stack, ANCHOR, DoorHingeSide.LEFT);
        }
    }

    private AssemblyPlacementGameTests() {
    }

    /** {@code mayUseItemAt} refused at one cell (each in turn, the anchor and every partner) refuses the whole placement. */
    @GameTest(template = TEMPLATE)
    public static void aRefusedCellRefusesTheWholePlacement(GameTestHelper helper) {
        floor(helper);
        for (Family family : Family.values()) {
            List<BlockPos> cells = family.cells(helper);
            for (int i = 0; i < cells.size(); i++) {
                String what = family + " refused at cell " + i;
                DenyingPlayer player = DenyingPlayer.create(helper, family + "_" + i, Direction.NORTH, Set.of(cells.get(i)));
                ItemStack stack = new ItemStack(family.item());
                InteractionResult result = family.place(helper, player, stack);
                check(!result.consumesAction(), what + ": placed anyway (" + result + ")");
                check(stack.getCount() == 1, what + ": the item was charged");
                for (BlockPos cell : cells) {
                    check(helper.getLevel().getBlockState(cell).isAir(), what + ": a cell was written at " + cell);
                }
            }
            // Control: refused nowhere, the same click places it.
            DenyingPlayer free = DenyingPlayer.create(helper, family + "_free", Direction.NORTH, Set.of());
            check(family.place(helper, free, new ItemStack(family.item())).consumesAction(), family + ": the control placement failed");
            clear(helper, cells);
        }
        helper.succeed();
    }

    /**
     * Installation, then the sound, then the {@code BLOCK_PLACE} game event, then one item charged (none
     * in creative): both effects already see the installed look and the uncharged stack.
     */
    @GameTest(template = TEMPLATE)
    public static void aSuccessfulPlacementInstallsAnnouncesThenCharges(GameTestHelper helper) {
        floor(helper);
        BlockPos anchor = helper.absolutePos(ANCHOR);
        for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
            for (Family family : Family.values()) {
                String what = family + " " + mode;
                ServerPlayer player = player(helper, "effects_" + family + "_" + mode, mode, Direction.NORTH);
                ItemStack stack = styled(family.item(), LOOK);
                stack.setCount(2);
                PlacementEffects.watch(anchor, () -> "count=" + stack.getCount() + " look=" + look(helper, anchor));
                InteractionResult result;
                List<String> effects;
                try {
                    result = family.place(helper, player, stack);
                } finally {
                    effects = PlacementEffects.stop(anchor);
                }
                check(result == InteractionResult.CONSUME, what + ": expected CONSUME, got " + result);
                String seen = "count=2 look=" + LOOK;
                check(effects.equals(List.of("sound " + seen, "place " + seen)), what + ": effects " + effects);
                int expected = mode == GameType.CREATIVE ? 2 : 1;
                check(stack.getCount() == expected, what + ": " + stack.getCount() + " items left, expected " + expected);
                check(look(helper, anchor).equals(LOOK.toString()), what + ": the look was not installed");
                clear(helper, family.cells(helper));
            }
        }
        helper.succeed();
    }

    /** The player-less writers place whole assemblies with no sound, no game event and nothing charged. */
    @GameTest(template = TEMPLATE)
    public static void playerlessPlacementIsSilent(GameTestHelper helper) {
        floor(helper);
        BlockPos anchor = helper.absolutePos(ANCHOR);
        for (DoorDesign design : DoorDesign.values()) {
            DoorLayout.Assembly door = door(helper, design);
            PlacementEffects.watch(anchor, () -> "");
            boolean placed = DoorPlacement.placeAt(helper.getLevel(), doorBlock(design), door, false, LOOK);
            List<String> effects = PlacementEffects.stop(anchor);
            check(placed, design + ": refused");
            check(effects.isEmpty(), design + ": effects " + effects);
            check(look(helper, anchor).equals(LOOK.toString()), design + ": the look was not installed");
            clear(helper, door.positions());
        }
        for (ArchwayDesign design : ArchwayDesign.values()) {
            ArchwayLayout.Assembly archway = ArchwayFixtures.assembly(helper, ANCHOR, Direction.NORTH, design);
            PlacementEffects.watch(anchor, () -> "");
            boolean placed = ArchwayPlacement.placeAt(helper.getLevel(), ArchwayFixtures.block(design), archway, LOOK);
            List<String> effects = PlacementEffects.stop(anchor);
            check(placed, design + ": refused");
            check(effects.isEmpty(), design + ": effects " + effects);
            check(look(helper, anchor).equals(LOOK.toString()), design + ": the look was not installed");
            clear(helper, archway.positions());
        }
        TrimLayout.Assembly frame = TrimFixtures.trimAssembly(helper, ANCHOR, Direction.NORTH);
        PlacementEffects.watch(anchor, () -> "");
        boolean placed = TrimPlacement.placeAt(helper.getLevel(), TrimFixtures.oakTrim(), frame, LOOK);
        List<String> effects = PlacementEffects.stop(anchor);
        check(placed, "frame: refused");
        check(effects.isEmpty(), "frame: effects " + effects);
        check(look(helper, anchor).equals(LOOK.toString()), "frame: the look was not installed");
        for (Quarter part : Quarter.values()) {
            check(helper.getLevel().getBlockState(frame.pos(part)).equals(frame.stateFor(part, TrimFixtures.oakTrim())),
                    "frame: " + part + " is not its layout state");
        }
        clear(helper, frame.positions());
        helper.succeed();
    }

    /** The item's plan and the player-less plan are written exactly, powered spots included (an open door). */
    @GameTest(template = TEMPLATE)
    public static void theWrittenStatesAreThePlansStates(GameTestHelper helper) {
        floor(helper);
        for (DoorDesign design : DoorDesign.values()) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                for (boolean powered : new boolean[] {false, true}) {
                    String what = design + " " + hinge + (powered ? " powered" : "");
                    if (powered) {
                        helper.setBlock(ANCHOR.west(), Blocks.REDSTONE_BLOCK);
                    }
                    ServerPlayer player = player(helper, "plan_" + design + "_" + hinge + "_" + powered, GameType.SURVIVAL, Direction.NORTH);
                    ItemStack stack = new ItemStack(doorBlock(design).asItem());
                    UseOnContext context = doorClick(helper, player, stack, hinge);
                    Optional<AssemblyPlacementPlan<DoorLayout.Part, DoorLayout.Assembly>> plan = DoorPlacement.of(doorBlock(design), new BlockPlaceContext(context));
                    check(plan.isPresent(), what + ": no plan");
                    check(plan.get().anchorState().getValue(net.minecraft.world.level.block.DoorBlock.OPEN) == powered, what + ": planned open " + plan.get().anchorState().getValue(net.minecraft.world.level.block.DoorBlock.OPEN));
                    check(stack.useOn(context).consumesAction(), what + ": placement failed");
                    for (DoorLayout.Part part : plan.get().assembly().parts()) {
                        check(helper.getLevel().getBlockState(plan.get().assembly().pos(part)).equals(plan.get().states().get(part)),
                                what + ": " + part + " is not the planned state");
                    }
                    clear(helper, plan.get().assembly().positions());
                    helper.setBlock(ANCHOR.west(), Blocks.AIR);
                }
            }
            DoorLayout.Assembly door = door(helper, design);
            Optional<AssemblyPlacementPlan<DoorLayout.Part, DoorLayout.Assembly>> at = DoorPlacement.at(helper.getLevel(), doorBlock(design), door, true);
            check(at.isPresent(), design + ": no player-less plan");
            check(DoorPlacement.placeAt(helper.getLevel(), doorBlock(design), door, true), design + ": player-less placement failed");
            for (DoorLayout.Part part : door.parts()) {
                check(helper.getLevel().getBlockState(door.pos(part)).equals(at.get().states().get(part)),
                        design + ": player-less " + part + " is not the planned state");
            }
            clear(helper, door.positions());
        }
        for (ArchwayDesign design : ArchwayDesign.values()) {
            ArchwayLayout.Assembly archway = ArchwayFixtures.assembly(helper, ANCHOR, Direction.NORTH, design);
            Optional<AssemblyPlacementPlan<ArchwayPart, ArchwayLayout.Assembly>> at = ArchwayPlacement.at(helper.getLevel(), ArchwayFixtures.block(design), archway);
            check(at.isPresent(), design + ": no player-less plan");
            check(ArchwayPlacement.placeAt(helper.getLevel(), ArchwayFixtures.block(design), archway, LOOK),
                    design + ": player-less placement failed");
            for (ArchwayPart part : ArchwayPart.values()) {
                check(helper.getLevel().getBlockState(archway.pos(part)).equals(at.get().states().get(part)),
                        design + ": player-less " + part + " is not the planned state");
            }
            clear(helper, archway.positions());
        }
        helper.succeed();
    }

    /**
     * A write refused part-way (the last cell already holds exactly its planned state) puts every cell
     * back: block states and block-entity data (a sign's text), nothing dropped, no archway left.
     */
    @GameTest(template = TEMPLATE)
    public static void aRefusedLaterWriteRollsEveryCellBack(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ArchwayLayout.Assembly archway = ArchwayFixtures.assembly(helper, ANCHOR, Direction.NORTH, ArchwayDesign.ORDINARY);
        var plan = ArchwayPlacement.at(level, ArchwayFixtures.block(ArchwayDesign.ORDINARY), archway).orElseThrow();
        BlockPos signed = archway.pos(ArchwayPart.LOWER_RIGHT); // on the floor, so the sign stands
        BlockPos taken = archway.pos(ArchwayPart.UPPER_RIGHT);
        level.setBlock(signed, Blocks.OAK_SIGN.defaultBlockState(), Block.UPDATE_CLIENTS);
        SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(signed);
        sign.updateText(text -> text.setMessage(0, Component.literal("kept")), true);
        CompoundTag signData = sign.saveWithoutMetadata();
        // The last cell already holds its planned state, so its write is refused (nothing changes there).
        AssemblyMutationBatch.run(level, List.of(taken), () -> level.setBlock(taken, plan.states().get(ArchwayPart.UPPER_RIGHT),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
        DoorFixtures.clearDrops(helper);

        check(!AssemblyInstallation.install(level, plan, LOOK, ArchwayBlockEntity.class), "a refused write reported success");
        check(level.getBlockState(archway.pos(ArchwayPart.LOWER_LEFT)).isAir(), "the anchor was not put back");
        check(level.getBlockState(archway.pos(ArchwayPart.UPPER_LEFT)).isAir(), "the upper left cell was not put back");
        check(level.getBlockState(signed).is(Blocks.OAK_SIGN), "the sign was not put back: " + level.getBlockState(signed));
        check(level.getBlockEntity(signed) instanceof SignBlockEntity restored && restored.saveWithoutMetadata().equals(signData),
                "the sign's text was not put back");
        check(level.getBlockState(taken).equals(plan.states().get(ArchwayPart.UPPER_RIGHT)), "the refused cell changed");
        check(DoorFixtures.dropped(helper, ArchwayFixtures.block(ArchwayDesign.ORDINARY).asItem()) == 0
                && DoorFixtures.dropped(helper, Items.OAK_SIGN) == 0, "the rollback dropped something");
        AssemblyMutationBatch.run(level, List.of(taken, signed), () -> {
            level.setBlock(taken, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(signed, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        });
        helper.succeed();
    }

    /** Nested too deep, during a snapshot restore, or with no anchor entity of the expected kind: refused, nothing changed. */
    @GameTest(template = TEMPLATE)
    public static void refusedInstallationsChangeNothing(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ArchwayLayout.Assembly archway = ArchwayFixtures.assembly(helper, ANCHOR, Direction.NORTH, ArchwayDesign.KEYHOLE);
        var plan = ArchwayPlacement.at(level, ArchwayFixtures.block(ArchwayDesign.KEYHOLE), archway).orElseThrow();

        boolean[] installed = {true};
        nested(level, BATCH_DEPTH, () -> installed[0] = AssemblyInstallation.install(level, plan, LOOK,
                ArchwayBlockEntity.class));
        check(!installed[0], "installed past the batch depth");
        assertAir(helper, archway.positions(), "batch depth");

        level.restoringBlockSnapshots = true;
        try {
            installed[0] = AssemblyInstallation.install(level, plan, LOOK, ArchwayBlockEntity.class);
        } finally {
            level.restoringBlockSnapshots = false;
        }
        check(!installed[0], "installed during a snapshot restore");
        assertAir(helper, archway.positions(), "snapshot restore");

        check(!AssemblyInstallation.install(level, plan, LOOK, RoundTrimBlockEntity.class), "installed without its anchor entity");
        assertAir(helper, archway.positions(), "missing anchor");
        helper.succeed();
    }

    /** A rollback inside an enclosing block-snapshot capture drops only its own snapshots and leaves capture on. */
    @GameTest(template = TEMPLATE)
    public static void aRollbackKeepsTheEnclosingSnapshots(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ArchwayLayout.Assembly archway = ArchwayFixtures.assembly(helper, ANCHOR, Direction.NORTH, ArchwayDesign.ORDINARY);
        var plan = ArchwayPlacement.at(level, ArchwayFixtures.block(ArchwayDesign.ORDINARY), archway).orElseThrow();
        BlockSnapshot outer = BlockSnapshot.create(level.dimension(), level, helper.absolutePos(new BlockPos(7, 1, 7)));
        level.captureBlockSnapshots = true;
        level.capturedBlockSnapshots.add(outer);
        boolean installed;
        List<BlockSnapshot> after;
        boolean stillCapturing;
        try {
            installed = AssemblyInstallation.install(level, plan, LOOK, RoundTrimBlockEntity.class);
        } finally {
            stillCapturing = level.captureBlockSnapshots;
            after = List.copyOf(level.capturedBlockSnapshots);
            level.captureBlockSnapshots = false;
            level.capturedBlockSnapshots.clear();
        }
        check(!installed, "installed without its anchor entity");
        check(stillCapturing, "the rollback switched the enclosing capture off");
        check(after.size() == 1 && after.get(0) == outer, "the enclosing snapshots changed: " + after.size());
        assertAir(helper, archway.positions(), "captured rollback");
        helper.succeed();
    }

    /** A player's placement whose installation is refused is FAIL: nothing written, charged, announced. */
    @GameTest(template = TEMPLATE)
    public static void aRefusedPlayerPlacementHasNoSuccessEffects(GameTestHelper helper) {
        floor(helper);
        BlockPos anchor = helper.absolutePos(ANCHOR);
        for (Family family : Family.values()) {
            ServerPlayer player = player(helper, "refused_" + family, GameType.SURVIVAL, Direction.NORTH);
            ItemStack stack = styled(family.item(), LOOK);
            InteractionResult[] result = {null};
            PlacementEffects.watch(anchor, () -> "");
            List<String> effects;
            try {
                nested(helper.getLevel(), BATCH_DEPTH, () -> result[0] = family.place(helper, player, stack));
            } finally {
                effects = PlacementEffects.stop(anchor);
            }
            check(result[0] != null && !result[0].consumesAction(), family + ": expected a refusal, got " + result[0]);
            check(stack.getCount() == 1, family + ": the item was charged");
            check(effects.isEmpty(), family + ": success effects " + effects);
            assertAir(helper, family.cells(helper), family.toString());
        }
        helper.succeed();
    }

    /** Runs {@code inner} inside {@code depth} nested mutation batches over a cell no test uses. */
    private static void nested(ServerLevel level, int depth, Runnable inner) {
        if (depth == 0) {
            inner.run();
            return;
        }
        AssemblyMutationBatch.run(level, List.of(BlockPos.ZERO), () -> nested(level, depth - 1, inner));
    }

    private static void assertAir(GameTestHelper helper, List<BlockPos> cells, String what) {
        for (BlockPos cell : cells) {
            check(helper.getLevel().getBlockState(cell).isAir(), what + ": a cell was left written at " + cell);
        }
    }

    private static DoorLayout.Assembly door(GameTestHelper helper, DoorDesign design) {
        return DoorFixtures.assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT, design);
    }

    private static AssemblyDoorBlock doorBlock(DoorDesign design) {
        return switch (design) {
            case ROUND -> ModBlocks.roundDoor();
            case ARCHED -> ModBlocks.archedDoor();
            case DOUBLE_ARCHED -> ModBlocks.doubleArchedDoor();
            case TALL_DOUBLE_ARCHED -> ModBlocks.tallDoubleArchedDoor();
        };
    }

    /** The floor click {@code DoorFixtures.placeDoor} makes for {@code hinge}, as a context (the stack in the main hand). */
    private static UseOnContext doorClick(GameTestHelper helper, ServerPlayer player, ItemStack stack, DoorHingeSide hinge) {
        Direction across = player.getDirection().getClockWise();
        double along = hinge == DoorHingeSide.LEFT ? 0.25 : 0.75;
        BlockPos floor = helper.absolutePos(ANCHOR.below());
        Vec3 hit = Vec3.atCenterOf(floor).add(0, 0.5, 0)
                .add(across.getStepX() * (along - 0.5), 0, across.getStepZ() * (along - 0.5));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, floor, false));
    }

    /** The look held by whatever anchor entity is at {@code anchor} (absolute), or {@code none}. */
    private static String look(GameTestHelper helper, BlockPos anchor) {
        BlockEntity entity = helper.getLevel().getBlockEntity(anchor);
        if (entity instanceof RoundDoorBlockEntity door) {
            return door.appearance().toString();
        }
        if (entity instanceof ArchPieceBlockEntity piece) return piece.appearance().toString();
        if (entity instanceof ArchwayBlockEntity archway) {
            return archway.appearance().toString();
        }
        if (entity instanceof RoundTrimBlockEntity frame) {
            return frame.appearance().toString();
        }
        return "none";
    }

    /** Removes an assembly silently (the anchor cleared; its partners follow through teardown) and checks it is gone. */
    private static void clear(GameTestHelper helper, List<BlockPos> cells) {
        helper.getLevel().setBlock(cells.get(0), Blocks.AIR.defaultBlockState(), 3);
        for (BlockPos cell : cells) {
            if (!helper.getLevel().getBlockState(cell).isAir()) {
                helper.getLevel().setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}
