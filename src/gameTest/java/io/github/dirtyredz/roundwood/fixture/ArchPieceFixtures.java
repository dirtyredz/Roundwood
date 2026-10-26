package io.github.dirtyredz.roundwood.fixture;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/** Production-path placement hits and assertions; all positions passed here are relative. */
public final class ArchPieceFixtures {
    private ArchPieceFixtures() {}
    public static ArchPieceLayout.Assembly assembly(GameTestHelper helper, BlockPos anchor, Direction facing, ArchPieceDesign design, Side side) {
        return new ArchPieceLayout.Assembly(helper.absolutePos(anchor), facing, design, side);
    }
    public static BlockHitResult floorHit(GameTestHelper helper, ServerPlayer player, BlockPos anchor, Side side) {
        BlockPos floor = helper.absolutePos(anchor.below());
        Direction right = player.getDirection().getClockWise();
        double offset = side == Side.LEFT ? -.25 : .25;
        return new BlockHitResult(Vec3.atCenterOf(floor).add(right.getStepX() * offset, .5, right.getStepZ() * offset),
                Direction.UP, floor, false);
    }
    public static BlockPlaceContext context(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos anchor, Side side) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, floorHit(helper, player, anchor, side));
    }
    public static InteractionResult place(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos anchor, Side side) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, floorHit(helper, player, anchor, side)));
    }
    public static void assertPiece(GameTestHelper helper, ArchPieceLayout.Assembly assembly) {
        check(assembly.complete(helper.getLevel(), ModBlocks.archPiece(assembly.design())), "incomplete " + assembly);
        for (var cell : assembly.parts()) {
            check(ArchPieceLayout.of(assembly.pos(cell), helper.getLevel().getBlockState(assembly.pos(cell))).equals(assembly), "wrong owner " + cell);
            check((helper.getLevel().getBlockEntity(assembly.pos(cell)) != null) == (cell.column == 0 && cell.row == 0), "entity outside anchor " + cell);
        }
    }
    public static void assertAir(GameTestHelper helper, ArchPieceLayout.Assembly assembly) {
        for (BlockPos pos : assembly.positions()) check(helper.getLevel().getBlockState(pos).isAir(), "cell changed/left behind: " + pos);
    }
}
