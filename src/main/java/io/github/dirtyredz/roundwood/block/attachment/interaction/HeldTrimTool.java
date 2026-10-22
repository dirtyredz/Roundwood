package io.github.dirtyredz.roundwood.block.attachment.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.item.TrimAttachmentItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraftforge.common.Tags;

import java.util.Optional;

/**
 * What the acting hand holds, as every trim host's click router sees it (dk-ff89f8e5): the one place
 * the acting-hand rule and the item tests live, so a router and the sneak-use bridge ({@code claims})
 * can never disagree. Classification only: what each kind does on a host (fit, refuse, remove,
 * restyle, the door's toggle) is the host router's.
 * <ol>
 *     <li>The offhand acts only while the main hand is empty; otherwise, and for an empty hand, {@link Kind#OTHER}.</li>
 *     <li>{@link Kind#ATTACHMENT}: a face-trim attachment item, with its {@link #slot}.</li>
 *     <li>{@link Kind#SHEARS}: shears by class or by the {@code forge:shears} tag.</li>
 *     <li>{@link Kind#DONOR}: a restyling donor for a trim (planks, dye or one of the four masonry
 *     blocks, sneaking or not), with its {@link #donor} gesture: the kind (wood, paint, masonry), the
 *     hand and the stack that pays.</li>
 *     <li>Anything else: {@link Kind#OTHER}.</li>
 * </ol>
 */
public record HeldTrimTool(Kind kind, InteractionHand hand, ItemStack stack, Optional<TrimSlot> slot,
                           Optional<AppearanceGestures.Gesture> donor) {
    public enum Kind {
        ATTACHMENT, SHEARS, DONOR, OTHER
    }

    /** What {@code player}'s {@code hand} holds for a trim host. */
    public static HeldTrimTool of(Player player, InteractionHand hand) {
        ItemStack stack = acting(player, hand);
        if (stack.isEmpty()) {
            return other(hand, stack);
        }
        Optional<TrimSlot> slot = TrimAttachmentItem.slotOf(stack);
        if (slot.isPresent()) {
            return new HeldTrimTool(Kind.ATTACHMENT, hand, stack, slot, Optional.empty());
        }
        if (stack.getItem() instanceof ShearsItem || stack.is(Tags.Items.SHEARS)) {
            return new HeldTrimTool(Kind.SHEARS, hand, stack, Optional.empty(), Optional.empty());
        }
        Optional<AppearanceGestures.Gesture> donor = AppearanceGestures.donor(AppearanceFamily.TRIM, player, hand);
        if (donor.isPresent()) {
            return new HeldTrimTool(Kind.DONOR, hand, stack, Optional.empty(), donor);
        }
        return other(hand, stack);
    }

    /** True when either hand acts with an attachment (the main hand, or the offhand while the main hand is empty). */
    public static boolean holdsAttachment(Player player) {
        return TrimAttachmentItem.slotOf(acting(player, InteractionHand.MAIN_HAND)).isPresent()
                || TrimAttachmentItem.slotOf(acting(player, InteractionHand.OFF_HAND)).isPresent();
    }

    /** The stack {@code hand} acts with: empty for the offhand while the main hand holds something. */
    private static ItemStack acting(Player player, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty() ? ItemStack.EMPTY : player.getItemInHand(hand);
    }

    public boolean is(Kind other) {
        return kind == other;
    }

    /** True for an attachment of {@code wanted}'s slot. */
    public boolean attachment(TrimSlot wanted) {
        return slot.filter(found -> found == wanted).isPresent();
    }

    private static HeldTrimTool other(InteractionHand hand, ItemStack stack) {
        return new HeldTrimTool(Kind.OTHER, hand, stack, Optional.empty(), Optional.empty());
    }
}
