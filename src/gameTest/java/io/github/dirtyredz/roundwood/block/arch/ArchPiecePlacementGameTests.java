package io.github.dirtyredz.roundwood.block.arch;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyInstallation;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.fixture.assembly.DenyingPlayer;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.Set;
import static io.github.dirtyredz.roundwood.fixture.ArchPieceFixtures.*;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.*;

/** Real item use and shared installer tests; no client or copied placement implementation. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchPiecePlacementGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private ArchPiecePlacementGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void proposalsMatchEveryProductFacingAndShoulderSide(GameTestHelper helper) {
        floor(helper);
        for (var design : ArchPieceDesign.values()) for (Direction facing : Direction.Plane.HORIZONTAL)
            for (Side side : design.shoulder() ? Side.values() : new Side[]{Side.LEFT}) {
                var player = player(helper, "arch_plan_" + design + facing + side, GameType.SURVIVAL, facing);
                var stack = new ItemStack(ModItems.archPiece(design), 2);
                var proposal = ArchPiecePlacement.of(ModBlocks.archPiece(design), context(helper, player, stack, ANCHOR, side)).orElseThrow();
                assertAir(helper, proposal.assembly());
                check(stack.getCount() == 2, "proposal charged stack");
                check(place(helper, player, stack, ANCHOR, side).consumesAction(), "item placement refused");
                check(stack.getCount() == 1, "wrong payment");
                assertPiece(helper, proposal.assembly());
                proposal.states().forEach((cell, state) -> check(helper.getLevel().getBlockState(proposal.assembly().pos(cell)).equals(state), "proposal disagrees at " + cell));
                helper.setBlock(ANCHOR, Blocks.AIR);
                assertAir(helper, proposal.assembly());
            }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyOccupiedWetOrProtectedCellRefusesWithoutWritingOrCharging(GameTestHelper helper) {
        floor(helper);
        for (var design : ArchPieceDesign.values()) {
            var assembly = assembly(helper, ANCHOR, Direction.NORTH, design, Side.LEFT);
            var block = ModBlocks.archPiece(design);
            for (BlockPos cell : assembly.positions()) {
                for (var obstacle : List.of(Blocks.STONE, Blocks.WATER)) {
                    helper.getLevel().setBlockAndUpdate(cell, obstacle.defaultBlockState());
                    var player = player(helper, "arch_blocked", GameType.SURVIVAL, Direction.NORTH);
                    var stack = new ItemStack(ModItems.archPiece(design));
                    check(ArchPiecePlacement.at(helper.getLevel(), block, assembly).isEmpty(), "playerless proposal ignored obstacle");
                    check(ArchPiecePlacement.of(block, context(helper, player, stack, ANCHOR, Side.LEFT)).isEmpty(), "player proposal ignored obstacle");
                    check(!place(helper, player, stack, ANCHOR, Side.LEFT).consumesAction(), "occupied/wet placement succeeded");
                    check(stack.getCount() == 1, "refusal charged item");
                    check(helper.getLevel().getBlockState(cell).is(obstacle), "obstacle changed");
                    for (BlockPos other : assembly.positions()) if (!other.equals(cell)) check(helper.getLevel().getBlockState(other).isAir(), "partial write");
                    helper.getLevel().setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
                }
                var player = DenyingPlayer.create(helper, "arch_denied_" + design + cell, Direction.NORTH, Set.of(cell));
                var stack = new ItemStack(ModItems.archPiece(design));
                check(!place(helper, player, stack, ANCHOR, Side.LEFT).consumesAction(), "protected cell was placed");
                check(stack.getCount() == 1, "permission refusal charged item");
                assertAir(helper, assembly);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void spanningPiecesNeedNoFloorAndOutOfWorldProposalsRefuse(GameTestHelper helper) {
        var level = helper.getLevel();
        var design = ArchPieceDesign.TOP_2X3;
        var block = ModBlocks.archPiece(design);
        var assembly = assembly(helper, ANCHOR.above(), Direction.NORTH, design, Side.LEFT);
        check(ArchPiecePlacement.placeAt(level, block, assembly, Appearance.DEFAULT), "floating top refused");
        for (BlockPos pos : assembly.positions()) if (pos.getY() == assembly.anchor().getY()) check(level.getBlockState(pos.below()).isAir(), "fixture has a floor");
        BlockPos base = assembly.anchor();
        for (BlockPos invalid : List.of(new BlockPos(base.getX(), level.getMaxBuildHeight() - 1, base.getZ()),
                new BlockPos(base.getX(), level.getMinBuildHeight() - 1, base.getZ()), base.offset(1_000_000, 0, 1_000_000),
                new BlockPos(30_000_000, base.getY(), 30_000_000))) {
            var outside = new ArchPieceLayout.Assembly(invalid, Direction.NORTH, design, Side.LEFT);
            check(ArchPiecePlacement.at(level, block, outside).isEmpty(), "out-of-world proposal accepted");
        }
        helper.runAfterDelay(3, () -> { assertPiece(helper, assembly); helper.succeed(); });
    }

    @GameTest(template = TEMPLATE)
    public static void entityObstructionUsesShoulderCollisionButNotTopCorners(GameTestHelper helper) {
        floor(helper);
        var pig = helper.spawn(EntityType.PIG, new Vec3(3.2, 1, 3.5));
        try {
            var shoulder = assembly(helper, ANCHOR, Direction.NORTH, ArchPieceDesign.KEYHOLE_SHOULDER, Side.LEFT);
            check(ArchPiecePlacement.at(helper.getLevel(), ModBlocks.archPiece(shoulder.design()), shoulder).isEmpty(), "pig intersects shoulder");
            var top = assembly(helper, ANCHOR, Direction.NORTH, ArchPieceDesign.TOP_1X1, Side.LEFT);
            check(ArchPiecePlacement.at(helper.getLevel(), ModBlocks.archPiece(top.design()), top).isPresent(), "noncolliding top obstructed");
        } finally { pig.discard(); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancellationRestoresEveryProductAndPayment(GameTestHelper helper) {
        floor(helper);
        var player = player(helper, "cancelled", GameType.SURVIVAL, Direction.NORTH);
        for (var design : ArchPieceDesign.values()) {
            var stack = new ItemStack(ModItems.archPiece(design));
            place(helper, player, stack, ANCHOR, Side.LEFT);
            check(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "cancelled placement charged item");
            assertAir(helper, assembly(helper, ANCHOR, Direction.NORTH, design, Side.LEFT));
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void failedAnchorInstallationRollsBackAndKeepsEnclosingSnapshots(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var assembly = assembly(helper, ANCHOR, Direction.NORTH, ArchPieceDesign.TOP_2X3, Side.LEFT);
        helper.setBlock(ANCHOR, Blocks.SNOW);
        var plan = ArchPiecePlacement.at(level, ModBlocks.archPiece(assembly.design()), assembly).orElseThrow();
        BlockSnapshot outer = BlockSnapshot.create(level.dimension(), level, helper.absolutePos(new BlockPos(7, 1, 7)));
        level.captureBlockSnapshots = true;
        level.capturedBlockSnapshots.add(outer);
        try {
            check(!AssemblyInstallation.install(level, plan, Appearance.DEFAULT, RoundTrimBlockEntity.class), "wrong anchor type accepted");
            check(level.captureBlockSnapshots, "outer capture disabled");
            check(level.capturedBlockSnapshots.size() == 1 && level.capturedBlockSnapshots.get(0) == outer, "outer snapshots changed");
            check(level.getBlockState(assembly.anchor()).is(Blocks.SNOW), "replaced snow not restored");
            for (BlockPos cell : assembly.positions()) if (!cell.equals(assembly.anchor())) check(level.getBlockState(cell).isAir(), "rollback left a cell");
            check(!(level.getBlockEntity(assembly.anchor()) instanceof ArchPieceBlockEntity), "rollback left anchor entity");
        } finally {
            level.captureBlockSnapshots = false;
            level.capturedBlockSnapshots.clear();
        }
        helper.succeed();
    }
}
