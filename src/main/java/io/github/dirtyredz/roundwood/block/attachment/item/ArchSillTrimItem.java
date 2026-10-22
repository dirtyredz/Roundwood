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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Arch Sill (0.10.0, {@code roundwood:arch_sill_trim}): the {@link TrimSlot#SILL} attachment, a bar
 * across the bottom of one face of an upper arch window, separately fitted, styled, removed and
 * dropped from that face's Universal Trim. An item only (no block). The window's router fits it (plain
 * click or sneaking; main hand first, the offhand only while the main hand is empty); the item's own
 * use does nothing, so a click is never fitted twice. Carries a trim appearance (wood, paint or one
 * of the four masonry materials).
 */
public class ArchSillTrimItem extends Item implements TrimAttachmentItem, AppearanceDefaults {
    /** The registry path. */
    public static final String ID = TrimSlot.SILL.itemId();

    public ArchSillTrimItem(Properties properties) {
        super(properties);
    }

    @Override
    public TrimSlot trimSlot() {
        return TrimSlot.SILL;
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.addAll(AppearanceTooltip.describe(AppearanceStacks.read(stack)));
        tooltip.add(Component.translatable("tooltip.roundwood.restyle").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.roundwood.arch_sill_trim").withStyle(ChatFormatting.DARK_GRAY));
    }
}
