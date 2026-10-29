package io.github.dirtyredz.roundwood.fixture.window;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import io.github.dirtyredz.roundwood.fixture.door.DoorFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Window placement for GameTests: hits at chosen face coordinates (the zone grid's u and v) on the
 * originally clicked cell, players at chosen spots (their eye picks an arch's front), and the state
 * the preview path predicts for a hit, to compare with what placement actually wrote.
 */
public final class WindowFixtures {
    private WindowFixtures() {
    }

    /** The face coordinates in the middle of each zone. */
    public static double[] zonePoint(WindowTargetZones.Zone zone) {
        return switch (zone) {
            case CENTER -> new double[] {0.5, 0.5};
            case U_MIN -> new double[] {0.1, 0.5};
            case U_MAX -> new double[] {0.9, 0.5};
            case V_MIN -> new double[] {0.5, 0.1};
            case V_MAX -> new double[] {0.5, 0.9};
        };
    }

    /** A hit at face coordinates (u, v) on the {@code face} side of the relative cell {@code clicked}. */
    public static BlockHitResult hit(GameTestHelper helper, BlockPos clicked, Direction face, double u, double v) {
        double[] p = WindowTargetZones.toCell(face, u, v);
        return hit(helper, clicked, face, p[0], p[1], p[2]);
    }

    /** A hit on {@code face} of the relative cell {@code clicked} at cell-local point (x, y, z) (block units). */
    public static BlockHitResult hit(GameTestHelper helper, BlockPos clicked, Direction face, double x, double y, double z) {
        BlockPos abs = helper.absolutePos(clicked);
        return new BlockHitResult(new Vec3(abs.getX() + x, abs.getY() + y, abs.getZ() + z), face, abs, false);
    }

    /** A hit in the middle of {@code zone} on {@code face} of {@code clicked}. */
    public static BlockHitResult zone(GameTestHelper helper, BlockPos clicked, Direction face, WindowTargetZones.Zone zone) {
        double[] uv = zonePoint(zone);
        return hit(helper, clicked, face, uv[0], uv[1]);
    }

    /** A fake player with feet at the relative point {@code feet}, facing {@code facing}, looking {@code pitch} degrees down. */
    public static ServerPlayer playerAt(GameTestHelper helper, String name, GameType mode, Vec3 feet, Direction facing, float pitch) {
        ServerPlayer player = DoorFixtures.player(helper, name, mode, facing);
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.moveTo(origin.getX() + feet.x, origin.getY() + feet.y, origin.getZ() + feet.z, facing.toYRot(), pitch);
        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
        player.setXRot(pitch);
        return player;
    }

    /** Uses {@code stack} from the main hand on {@code hit} through {@link ItemStack#useOn} (Forge's placement capture). */
    public static InteractionResult place(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    /**
     * What placing {@code stack} on {@code hit} would write, computed the preview's way (a fresh
     * placement context and the block's own state for it) before anything is placed; null when the
     * destination is not replaceable.
     */
    @Nullable
    public static BlockState predict(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
        if (!context.canPlace()) {
            return null;
        }
        if (stack.getItem() instanceof io.github.dirtyredz.roundwood.migration.item.RetiredCrossWindowItem retired) {
            return io.github.dirtyredz.roundwood.migration.item.CrossWindowPlacement.plan(retired, context).map(
                    io.github.dirtyredz.roundwood.migration.item.CrossWindowPlacement.Plan::state).orElse(null);
        }
        return ((BlockItem) stack.getItem()).getBlock().getStateForPlacement(context);
    }

    /** Canonical pane plus attachments, the same building path as the showcase. */
    public static void setWindow(GameTestHelper helper, BlockPos pos,
            io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign design, WindowPose pose) {
        var sample = io.github.dirtyredz.roundwood.showcase.ShowcaseWindow.of(design, pose, 0, 0, 0,
                io.github.dirtyredz.roundwood.appearance.Appearance.DEFAULT);
        helper.setBlock(pos, sample.state());
        if (sample.beams().isPresent()) {
            DoorFixtures.check(io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions.install(helper.getLevel(), helper.absolutePos(pos),
                    io.github.dirtyredz.roundwood.block.attachment.TrimSlot.CROSS_BEAMS, io.github.dirtyredz.roundwood.block.attachment.AttachmentFace.FRONT,
                    sample.beams().get()), "fixture beams refused");
        }
    }

    /** The pose of a window state. */
    public static WindowPose pose(BlockState state) {
        return ((WindowPaneBlock) state.getBlock()).pose(state);
    }
}
