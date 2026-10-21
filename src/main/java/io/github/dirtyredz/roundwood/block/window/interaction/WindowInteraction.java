package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimItem;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;

import java.util.Optional;

/**
 * A window's right-click router, in order:
 * <ol>
 *     <li>a window trim item: passed on, so the item's own use fits it ({@link WindowTrimItem});</li>
 *     <li>on a fitted trim's band ({@link WindowTrimTarget#hitsBand}), no sneak needed: shears remove
 *     the trim, planks / masonry / dye restyle the trim;</li>
 *     <li>anywhere else: the pane's restyling gesture (sneak + planks, or dye) on the wood and beams;</li>
 *     <li>everything else (buckets included): the item's own use.</li>
 * </ol>
 * The main hand wins; the offhand acts only while the main hand is empty.
 */
public final class WindowInteraction {
    private WindowInteraction() {
    }

    public static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                        BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getItem() instanceof WindowTrimItem) {
            return InteractionResult.PASS;
        }
        if (WindowTrimTarget.hitsBand(state, pos, hit.getLocation())) {
            if (stack.getItem() instanceof ShearsItem || stack.is(Tags.Items.SHEARS)) {
                return WindowTrimActions.remove(level, pos, player, hand, stack);
            }
            Optional<AppearanceGestures.Gesture> donor = AppearanceGestures.donor(AppearanceFamily.TRIM, player, hand);
            if (donor.isPresent()) {
                return AppearanceActions.apply(level, player, donor.get(), () -> window(level, pos).map(WindowTrimTarget::of));
            }
            return InteractionResult.PASS;
        }
        Optional<AppearanceGestures.Gesture> gesture = AppearanceGestures.of(AppearanceFamily.WINDOW, player, hand);
        if (gesture.isEmpty()) {
            return InteractionResult.PASS;
        }
        return AppearanceActions.apply(level, player, gesture.get(), () -> window(level, pos).map(AppearanceActions.Target.class::cast));
    }

    private static Optional<RoundWindowBlockEntity> window(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }
}
