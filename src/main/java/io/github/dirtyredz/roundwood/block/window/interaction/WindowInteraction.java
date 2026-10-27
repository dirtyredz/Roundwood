package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.interaction.HeldTrimTool;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassActions;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;
import io.github.dirtyredz.roundwood.block.window.pane.RetiredCrossWindows;

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
        if (!level.isClientSide && RetiredCrossWindows.isRetired(state)) {
            if (RetiredCrossWindows.handler().convert(level, pos)
                    != RetiredCrossWindows.Result.CONVERTED) return InteractionResult.CONSUME;
            return use(level.getBlockState(pos), level, pos, player, hand, hit);
        }
        WindowHitTarget target = plan.get().target();
        ItemStack stack = player.getItemInHand(hand);
        return switch (plan.get().action()) {
            case PASS -> InteractionResult.PASS;
            case FIT_TRIM -> WindowTrimActions.fit(level, pos, player, stack, TrimSlot.PERIMETER, target.face());
            case FIT_BEAMS -> WindowBeamFitPlan.of(level, pos, player, hand, hit)
                    .map(fit -> WindowTrimActions.fit(level, pos, player, stack, TrimSlot.CROSS_BEAMS, fit.target().face()))
                    .orElse(InteractionResult.CONSUME);
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
            case REFUSE_FRAME -> AppearanceActions.refuse(level, player, "message.roundwood.frameless_no_frame");
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
        WindowUseRules.Item item = item(HeldTrimTool.of(player, hand));
        if (item == WindowUseRules.Item.OTHER) {
            return Optional.empty();
        }
        RoundWindowBlockEntity window = level.getBlockEntity(pos) instanceof RoundWindowBlockEntity found ? found : null;
        FaceTrims trims = window != null ? window.trims() : FaceTrims.NONE;
        FaceTrims sills = window != null ? window.trims(TrimSlot.SILL) : FaceTrims.NONE;
        boolean takesSill = state.getBlock() instanceof WindowPaneBlock block && block.profile().takesSill();
        return WindowHitTarget.of(state, pos, hit.getLocation(), hit.getDirection(), player.getEyePosition(), trims, sills, WindowPaneState.design(state, window))
                .map(target -> new Plan(WindowUseRules.decide(item, target.region(), takesSill,
                        !(state.getBlock() instanceof WindowPaneBlock block) || block.profile().takesTrim()), target));
    }

    /**
     * What the held tool is to a window: each attachment by its slot, shears, then a trim donor by its
     * kind (dye paints, planks re-wood, masonry is refused on the frame), else nothing.
     */
    public static WindowUseRules.Item item(HeldTrimTool tool) {
        return switch (tool.kind()) {
            case ATTACHMENT -> switch (tool.slot().orElseThrow()) {
                case SILL -> WindowUseRules.Item.ARCH_SILL;
                case PERIMETER -> WindowUseRules.Item.UNIVERSAL_TRIM;
                case CROSS_BEAMS -> WindowUseRules.Item.CROSS_BEAMS;
            };
            case SHEARS -> WindowUseRules.Item.SHEARS;
            case DONOR -> switch (tool.donor().orElseThrow().type()) {
                case PAINT -> WindowUseRules.Item.DYE;
                case WOOD, BLOCK_SURFACE -> WindowUseRules.Item.PLANKS;
                case MASONRY -> WindowUseRules.Item.MASONRY;
            };
            case OTHER -> WindowUseRules.Item.OTHER;
        };
    }

    private static Optional<RoundWindowBlockEntity> window(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }
}
