package io.github.dirtyredz.roundwood.block.window.glass;

import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.item.StackPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Glass colour on window item stacks (0.8.0): stored with {@link WindowGlassNbt} only when tinted, so
 * clear windows carry no glass tag and stack with crafted ones. Dropped and picked windows keep their
 * tint, placement installs it, and viewers and model caches tell windows apart by
 * {@link #key} (frame appearance plus glass).
 */
public final class WindowGlassStacks {
    private WindowGlassStacks() {
    }

    public static Optional<DyeColor> read(ItemStack stack) {
        return StackPayloads.readCustom(stack, WindowGlassNbt::read);
    }

    /** Stores {@code color} on {@code stack}, removing only that key (and emptied custom data) for clear glass. */
    public static void write(ItemStack stack, Optional<DyeColor> color) {
        if (color.isEmpty()) {
            StackPayloads.removeCustom(stack, WindowGlassNbt.TAG);
            return;
        }
        StackPayloads.editCustom(stack, tag -> WindowGlassNbt.write(tag, color));
    }

    /** The comparison identity of a window stack: its frame appearance key plus its glass colour. */
    public static String key(ItemStack stack) {
        return AppearanceStacks.read(stack).key() + read(stack).map(color -> "|glass/" + color.getName()).orElse("");
    }

    /** The tooltip line naming a tinted window's glass (none for clear glass). */
    public static Optional<Component> describe(ItemStack stack) {
        return read(stack).map(color -> Component.translatable("tooltip.roundwood.glass",
                Component.translatable("color.minecraft." + color.getName())).withStyle(ChatFormatting.GRAY));
    }
}
