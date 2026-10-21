package io.github.dirtyredz.roundwood.appearance.item;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * tooltip naming the stored look. The canonical items place their canonical block; the hidden 0.4.0
 * aliases ({@code alias} true) place the same canonical block in their historical look, keep their
 * old names, and never claim the canonical block's item mapping.
 */
public class AppearanceBlockItem extends BlockItem implements AppearanceDefaults {
    private final AppearanceFamily family;
    private final Appearance fallback;
    @Nullable
    private final String aliasId;

    /**
     * @param aliasId the 0.4.0 registry path of a hidden alias, null for the canonical item
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

    /** Aliases keep their 0.4.0 names ("Spruce Round Door"). */
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
        tooltip.addAll(describe(AppearanceStacks.read(stack)));
        tooltip.add(Component.translatable("tooltip.roundwood.restyle").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Tooltip lines naming an appearance's material and paint. */
    public static List<Component> describe(Appearance appearance) {
        String kind = appearance.isWood() ? "tooltip.roundwood.wood" : "tooltip.roundwood.masonry";
        Component material;
        if (BuiltInRegistries.BLOCK.containsKey(appearance.material())) {
            material = BuiltInRegistries.BLOCK.get(appearance.material()).getName();
        } else {
            return List.of(Component.translatable("tooltip.roundwood.unavailable", appearance.material().toString())
                    .withStyle(ChatFormatting.RED));
        }
        Component line = Component.translatable(kind, material).withStyle(ChatFormatting.GRAY);
        return appearance.paint()
                .map(dye -> List.of(line, Component.translatable("tooltip.roundwood.paint",
                        Component.translatable("color.minecraft." + dye.getName())).withStyle(ChatFormatting.GRAY)))
                .orElse(List.of(line));
    }
}
