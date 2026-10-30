package io.github.dirtyredz.roundwood.block.attachment.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceTooltip;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Shared non-block attachment item for Arch Sill and Cross Beams. Carries a TRIM-family look and
 * names its slot. Host routers own fitting; item use is inert so one click cannot fit twice.
 */
public class StyledAttachmentItem extends Item implements TrimAttachmentItem, AppearanceDefaults {
    private final TrimSlot slot;

    public StyledAttachmentItem(TrimSlot slot, Properties properties) {
        super(properties);
        this.slot = slot;
    }

    @Override
    public TrimSlot trimSlot() {
        return slot;
    }

    @Override
    public AppearanceFamily appearanceFamily() {
        return AppearanceFamily.TRIM;
    }

    @Override
    public Appearance defaultAppearance() {
        return Appearance.DEFAULT;
    }

    /** Windows fit in their own router; anywhere else the item does nothing. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.addAll(AppearanceTooltip.describe(AppearanceStacks.read(stack)));
        tooltip.add(Component.translatable("tooltip.roundwood.restyle").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.roundwood." + slot.itemId()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
