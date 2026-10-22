package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.appearance.material.PlankMaterials;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.item.TrimAttachmentItem;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassActions;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;

import java.util.Optional;

/**
 * A window's right-click router (0.8.0): the clicked face and region ({@link WindowHitTarget}) and
 * the held item decide one action ({@link WindowUseRules}), carried out once on the server with the
 * usual permission and payment rules. No action needs sneaking; the sneak-use bridge asks
 * {@link #claims} (the same decision) so sneaking does the same. The main hand wins; the offhand acts
 * only while the main hand is empty. A handled or refused action never falls through to the item
 * (no donor block is placed against the window).
 */
public final class WindowInteraction {
    /** The decided action and where it applies. */
    public record Plan(WindowUseRules.Action action, WindowHitTarget target) {
    }

    private WindowInteraction() {
    }

    public static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                        BlockHitResult hit) {
        Optional<Plan> plan = plan(state, level, pos, player, hand, hit);
        if (plan.isEmpty()) {
            return InteractionResult.PASS;
        }
        WindowHitTarget target = plan.get().target();
        ItemStack stack = player.getItemInHand(hand);
        return switch (plan.get().action()) {
            case PASS -> InteractionResult.PASS;
            case FIT_TRIM -> WindowTrimActions.fit(level, pos, player, stack, TrimSlot.PERIMETER, target.face());
            case FIT_SILL -> WindowTrimActions.fit(level, pos, player, stack, TrimSlot.SILL, target.face());
            case RESTYLE_TRIM -> AppearanceGestures.donor(AppearanceFamily.TRIM, player, hand)
                    .map(gesture -> AppearanceActions.apply(level, player, gesture,
                            () -> window(level, pos).map(window -> WindowTrimActions.target(level, pos, window,
                                    target.region().slot(), target.face()))))
                    .orElse(InteractionResult.PASS);
            case RESTYLE_FRAME -> AppearanceGestures.donor(AppearanceFamily.WINDOW, player, hand)
                    .map(gesture -> AppearanceActions.apply(level, player, gesture,
                            () -> window(level, pos).map(AppearanceActions.Target.class::cast)))
                    .orElse(InteractionResult.PASS);
            case TINT_GLASS -> stack.getItem() instanceof DyeItem dye
                    ? WindowGlassActions.tint(level, pos, player, stack, dye.getDyeColor()) : InteractionResult.PASS;
            case REMOVE_TRIM -> WindowTrimActions.remove(level, pos, player, hand, stack, target.region().slot(), target.face());
            case REFUSE_MASONRY -> AppearanceActions.refuse(level, player, "message.roundwood.masonry_trim_only");
            case REFUSE_SILL -> AppearanceActions.refuse(level, player, "message.roundwood.sill_upper_arch_only");
        };
    }

    /**
     * True when this click is one of the window's own actions (anything but the item's ordinary use):
     * the sneak-use bridge lets exactly these reach {@link #use}.
     */
    public static boolean claims(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        return plan(state, level, pos, player, hand, hit)
                .map(plan -> plan.action() != WindowUseRules.Action.PASS)
                .orElse(false);
    }

    /** The action for this click, or empty when the hand, the item or the hit is nothing to a window. */
    public static Optional<Plan> plan(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                                      BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            return Optional.empty();
        }
        WindowUseRules.Item item = item(player.getItemInHand(hand));
        if (item == WindowUseRules.Item.OTHER) {
            return Optional.empty();
        }
        RoundWindowBlockEntity window = level.getBlockEntity(pos) instanceof RoundWindowBlockEntity found ? found : null;
        FaceTrims trims = window != null ? window.trims() : FaceTrims.NONE;
        FaceTrims sills = window != null ? window.trims(TrimSlot.SILL) : FaceTrims.NONE;
        boolean takesSill = state.getBlock() instanceof WindowPaneBlock block && block.profile().takesSill();
        return WindowHitTarget.of(state, pos, hit.getLocation(), hit.getDirection(), player.getEyePosition(), trims, sills)
                .map(target -> new Plan(WindowUseRules.decide(item, target.region(), takesSill), target));
    }

    /** What {@code stack} is to a window: each attachment item by its slot, then the restyling and tool items. */
    public static WindowUseRules.Item item(ItemStack stack) {
        if (stack.isEmpty()) {
            return WindowUseRules.Item.OTHER;
        }
        Optional<TrimSlot> slot = TrimAttachmentItem.slotOf(stack);
        if (slot.isPresent()) {
            return slot.get() == TrimSlot.SILL ? WindowUseRules.Item.ARCH_SILL : WindowUseRules.Item.UNIVERSAL_TRIM;
        }
        if (stack.getItem() instanceof ShearsItem || stack.is(Tags.Items.SHEARS)) {
            return WindowUseRules.Item.SHEARS;
        }
        if (stack.getItem() instanceof DyeItem) {
            return WindowUseRules.Item.DYE;
        }
        if (PlankMaterials.fromStack(stack).isPresent()) {
            return WindowUseRules.Item.PLANKS;
        }
        if (MasonryMaterials.fromStack(stack).isPresent()) {
            return WindowUseRules.Item.MASONRY;
        }
        return WindowUseRules.Item.OTHER;
    }

    private static Optional<RoundWindowBlockEntity> window(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }
}
