package io.github.dirtyredz.roundwood.appearance.item;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The block item shared by the three families: it carries the family and default appearance, and a
 * tooltip naming the stored look. The canonical items place their canonical block; the hidden
 * compatibility items ({@link #isAlias}: the 0.4.0 aliases and, since 0.9.0, the 0.8.0 window trim)
 * place or attach like their canonical item, in their historical look, keep their old names, and
 * never claim the canonical block's item mapping.
 */
public class AppearanceBlockItem extends BlockItem implements AppearanceDefaults {
    private final AppearanceFamily family;
    private final Appearance fallback;
    @Nullable
    private final String aliasId;

    /**
     * @param aliasId the registry path of a hidden compatibility item, null for the canonical item
     */
    public AppearanceBlockItem(Block block, Properties properties, AppearanceFamily family, Appearance fallback,
                               @Nullable String aliasId) {
        super(block, properties);
        this.family = family;
        this.fallback = fallback;
        this.aliasId = aliasId;
    }

    @Override
    public AppearanceFamily appearanceFamily() {
        return family;
    }

    @Override
    public Appearance defaultAppearance() {
        return fallback;
    }

    public boolean isAlias() {
        return aliasId != null;
    }

    /** Compatibility items keep their old names ("Spruce Round Door", "Window Trim"). */
    @Override
    public String getDescriptionId() {
        return aliasId != null ? "block." + Roundwood.MOD_ID + "." + aliasId : super.getDescriptionId();
    }

    /** Only the canonical item maps the canonical block back to an item. */
    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        if (aliasId == null) {
            super.registerBlocks(map, item);
        }
    }

    @Override
    public void removeFromBlockToItemMap(Map<Block, Item> map, Item item) {
        if (aliasId == null) {
            super.removeFromBlockToItemMap(map, item);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.addAll(AppearanceTooltip.describe(AppearanceStacks.read(stack)));
        tooltip.add(Component.translatable("tooltip.roundwood.restyle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
