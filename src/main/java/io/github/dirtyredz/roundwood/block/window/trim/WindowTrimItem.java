package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceTooltip;
import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The window trim (0.7.0): not a block, an attachment. Used on any Roundwood window (plain click or
 * sneaking) it fits that window's profile and frames both faces in the stack's look; it carries a
 * trim appearance (wood, paint or one of the four masonry materials) like the round trim. Blocks
 * call nothing for it: the window's router passes the click on, so this item's own use does the fit.
 */
public class WindowTrimItem extends Item implements AppearanceDefaults {
    /** The registry path (the registry registers under it; removal looks the item up by it). */
    public static final String ID = "window_trim";

    public WindowTrimItem(Properties properties) {
        super(properties);
    }

    /** The registered window trim item. */
    public static Item item() {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, ID));
    }

    @Override
    public AppearanceFamily appearanceFamily() {
        return AppearanceFamily.TRIM;
    }

    @Override
    public Appearance defaultAppearance() {
        return Appearance.DEFAULT;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof WindowPaneBlock)) {
            return InteractionResult.PASS;
        }
        return WindowTrimActions.fit(context.getLevel(), context.getClickedPos(), player, context.getItemInHand());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.addAll(AppearanceTooltip.describe(AppearanceStacks.read(stack)));
        tooltip.add(Component.translatable("tooltip.roundwood.window_trim").withStyle(ChatFormatting.DARK_GRAY));
    }
}
