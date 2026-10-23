package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPlacement;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import io.github.dirtyredz.roundwood.migration.item.CrossWindowPlacement;
import io.github.dirtyredz.roundwood.migration.item.RetiredCrossWindowItem;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What placing the held window would do at the crosshair, computed through the same path the server
 * takes: a {@link BlockPlaceContext} for the held stack and the (door-resolved) hit, vanilla's
 * destination, {@link WindowPlacement} and the block's own {@code getStateForPlacement} (pose;
 * no connections). The pane is only proposed when the local checks vanilla placement makes would pass
 * (replaceable destination, a state, survival, no entity in the way); nothing here calls a block's
 * or item's use, so whatever an interactive block does with the click is not predicted.
 */
public final class WindowPlacementPreview {
    private WindowPlacementPreview() {
    }

    /**
     * @param clicked  the cell whose face the zones are on
     * @param face     that face
     * @param zone     the targeted zone
     * @param proposal the placed state and where, when placement would pass the local checks
     */
    public record Preview(BlockPos clicked, Direction face, Vec3 hit, WindowTargetZones.Zone zone,
                          Optional<Proposal> proposal) {
    }

    public record Proposal(BlockPos pos, BlockState state, @Nullable VoxelShape fittedShape) {
        public Proposal(BlockPos pos, BlockState state) { this(pos, state, null); }
    }

    /** The preview for {@code player} aiming at {@code hit}, or empty when no window is held or the hit is refused. */
    public static Optional<Preview> of(Player player, Level level, BlockHitResult hit) {
        InteractionHand hand = windowHand(player);
        if (hand == null) {
            return Optional.empty();
        }
        Optional<BlockHitResult> resolved = DoorHitResolver.resolve(level, hit);
        if (resolved.isEmpty()) {
            return Optional.empty(); // the open leaf's projection: the click is refused
        }
        ItemStack stack = player.getItemInHand(hand);
        BlockItem item = (BlockItem) stack.getItem();
        BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, resolved.get());
        WindowPlacement.Request request = WindowPlacement.of(context);
        return Optional.of(new Preview(request.clicked(), request.face(), context.getClickLocation(), request.zone(),
                proposal(player, level, item, context)));
    }

    /** The main hand's window, else the offhand's when the main hand is empty; null when neither holds one. */
    @Nullable
    static InteractionHand windowHand(Player player) {
        if (isWindow(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getMainHandItem().isEmpty() && isWindow(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isWindow(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof WindowPaneBlock;
    }

    private static Optional<Proposal> proposal(Player player, Level level, BlockItem item, BlockPlaceContext context) {
        if (item instanceof RetiredCrossWindowItem retired) {
            return CrossWindowPlacement.plan(retired, context)
                    .map(plan -> new Proposal(plan.pos(), plan.state(), plan.selection()));
        }
        if (!context.canPlace()) {
            return Optional.empty();
        }
        BlockPos pos = context.getClickedPos();
        if (!level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) {
            return Optional.empty();
        }
        BlockState state = item.getBlock().getStateForPlacement(context);
        if (state == null || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.of(player))) {
            return Optional.empty();
        }
        return Optional.of(new Proposal(pos, state));
    }
}
