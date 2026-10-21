package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceTooltip;
import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The window trim: not a block, an attachment. Since 0.8.0 one item trims one face: used on any
 * Roundwood window (plain click or sneaking) the window's router fits the face that was clicked in
 * the stack's look ({@code interaction/WindowInteraction}; sneaking reaches it through the sneak-use
 * bridge); a face that already has a trim is a handled no-op. It carries a trim appearance (wood,
 * paint or one of the four masonry materials). Used on anything else it does nothing.
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.addAll(AppearanceTooltip.describe(AppearanceStacks.read(stack)));
        tooltip.add(Component.translatable("tooltip.roundwood.window_trim").withStyle(ChatFormatting.DARK_GRAY));
    }
}
