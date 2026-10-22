package io.github.dirtyredz.roundwood.block.archway.appearance;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayBlockEntities;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimActions;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimTarget;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.item.TrimAttachmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;

import java.util.Optional;

/**
 * An archway's right-click router (0.12.0), following the window and trim convention: no action needs
 * sneaking and sneaking does the same (the sneak-use bridge asks {@link #claims}, the same decision);
 * the main hand wins, the offhand acts only while the main hand is empty; a handled or refused click
 * never falls through to the item (no plank is placed against the archway). Where the hit lands is
 * {@link ArchwayTrimTarget}'s decision:
 * <table>
 *     <tr><th>Item</th><th>Where</th><th>Action</th></tr>
 *     <tr><td>Universal Trim (any id)</td><td>anywhere</td><td>fit the nearer face's casing (occupied: free no-op)</td></tr>
 *     <tr><td>Arch Sill</td><td>anywhere</td><td>refused with a hint, nothing used</td></tr>
 *     <tr><td>shears</td><td>a face</td><td>remove that face's casing (none: free no-op)</td></tr>
 *     <tr><td>planks, masonry, dye</td><td>a face with a casing</td><td>restyle that casing</td></tr>
 *     <tr><td>planks, masonry, dye</td><td>elsewhere</td><td>restyle the archway</td></tr>
 *     <tr><td>anything else</td><td>anywhere</td><td>the item's ordinary use</td></tr>
 * </table>
 * Archways have no glass, tint, swing, camouflage or redstone action.
 */
public final class ArchwayInteraction {
    /** What a click does. */
    public enum Action {
        FIT_TRIM, REFUSE_SILL, REMOVE_TRIM, RESTYLE_TRIM, RESTYLE_ARCHWAY, PASS
    }

    private ArchwayInteraction() {
    }

    public static InteractionResult use(ArchwayBlock block, BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hit) {
        ArchwayTrimTarget.Hit target = ArchwayTrimTarget.of(state, pos, hit);
        ItemStack stack = player.getItemInHand(hand);
        return switch (decide(state, level, pos, player, hand, target)) {
            case FIT_TRIM -> ArchwayTrimActions.fit(level, pos, state, player, stack, target.face());
            case REFUSE_SILL -> AppearanceActions.refuse(level, player, "message.roundwood.sill_upper_arch_only");
            case REMOVE_TRIM -> ArchwayTrimActions.remove(level, pos, state, player, hand, stack, target.face());
            case RESTYLE_TRIM -> AppearanceGestures.donor(AppearanceFamily.TRIM, player, hand)
                    .map(gesture -> AppearanceActions.apply(level, player, gesture,
                            () -> ArchwayTrimActions.target(level, pos, state, target.face())))
                    .orElse(InteractionResult.PASS);
            case RESTYLE_ARCHWAY -> AppearanceGestures.donor(AppearanceFamily.TRIM, player, hand)
                    .map(gesture -> AppearanceActions.apply(level, player, gesture,
                            () -> ArchwayAppearance.target(block, state, level, pos)))
                    .orElse(InteractionResult.PASS);
            case PASS -> InteractionResult.PASS;
        };
    }

    /** True when this click is one of the archway's own actions: the sneak-use bridge lets exactly these reach {@link #use}. */
    public static boolean claims(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        return decide(state, level, pos, player, hand, ArchwayTrimTarget.of(state, pos, hit)) != Action.PASS;
    }

    /** The action for this click. */
    public static Action decide(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                                ArchwayTrimTarget.Hit target) {
        if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            return Action.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return Action.PASS;
        }
        Optional<TrimSlot> slot = TrimAttachmentItem.slotOf(stack);
        if (slot.isPresent()) {
            return slot.get() == TrimSlot.PERIMETER ? Action.FIT_TRIM : Action.REFUSE_SILL;
        }
        boolean onFace = target.region() == ArchwayTrimTarget.Region.FACE;
        if (stack.getItem() instanceof ShearsItem || stack.is(Tags.Items.SHEARS)) {
            return onFace ? Action.REMOVE_TRIM : Action.PASS;
        }
        if (AppearanceGestures.donor(AppearanceFamily.TRIM, player, hand).isEmpty()) {
            return Action.PASS;
        }
        boolean trimmed = onFace && ArchwayBlockEntities.anchor(level, pos, state)
                .map(anchor -> anchor.trims().has(target.face())).orElse(false);
        return trimmed ? Action.RESTYLE_TRIM : Action.RESTYLE_ARCHWAY;
    }
}
