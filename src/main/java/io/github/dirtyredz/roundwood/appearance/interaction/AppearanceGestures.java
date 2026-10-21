package io.github.dirtyredz.roundwood.appearance.interaction;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceChanges;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.appearance.material.PlankMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Recognises a restyling gesture from what a hand holds:
 * <ul>
 *     <li>sneak + planks: set the wood (every family);</li>
 *     <li>dye, sneaking or not: paint (every family; masonry refuses);</li>
 *     <li>sneak + one of the four masonry blocks: trims only.</li>
 * </ul>
 * The main hand wins: the offhand is only considered while the main hand is empty. Anything else is
 * no gesture, and the block's ordinary use (or the item's) proceeds. {@link #donor} recognises the
 * same donors without the sneak requirement, for targets where a plain click means restyling (a
 * window trim's band, 0.7.0).
 */
public final class AppearanceGestures {
    public enum Type {
        WOOD, PAINT, MASONRY
    }

    /** A recognised gesture: what it applies and the stack that pays for it. */
    public record Gesture(Type type, ResourceLocation material, DyeColor dye, InteractionHand hand, ItemStack stack) {
        public AppearanceChanges.Change apply(Appearance current) {
            return switch (type) {
                case WOOD -> AppearanceChanges.wood(current, material);
                case PAINT -> AppearanceChanges.paint(current, dye);
                case MASONRY -> AppearanceChanges.masonry(current, material);
            };
        }
    }

    private AppearanceGestures() {
    }

    /** The gesture {@code player}'s {@code hand} makes on a {@code family} block, if any. */
    public static Optional<Gesture> of(AppearanceFamily family, Player player, InteractionHand hand) {
        return recognise(family, player, hand, player.isSecondaryUseActive());
    }

    /** The donor {@code player}'s {@code hand} holds for a {@code family} target, sneaking or not. */
    public static Optional<Gesture> donor(AppearanceFamily family, Player player, InteractionHand hand) {
        return recognise(family, player, hand, true);
    }

    private static Optional<Gesture> recognise(AppearanceFamily family, Player player, InteractionHand hand, boolean materials) {
        if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            return Optional.empty();
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof DyeItem dye) {
            return Optional.of(new Gesture(Type.PAINT, null, dye.getDyeColor(), hand, stack));
        }
        if (!materials) {
            return Optional.empty();
        }
        Optional<ResourceLocation> planks = PlankMaterials.fromStack(stack);
        if (planks.isPresent()) {
            return Optional.of(new Gesture(Type.WOOD, planks.get(), null, hand, stack));
        }
        if (family.accepts(Appearance.Kind.MASONRY)) {
            return MasonryMaterials.fromStack(stack).map(id -> new Gesture(Type.MASONRY, id, null, hand, stack));
        }
        return Optional.empty();
    }
}
