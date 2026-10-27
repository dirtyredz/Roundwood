package io.github.dirtyredz.roundwood.appearance.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The tooltip lines naming a look's material and paint, shared by the family block items and the
 * window trim (an appearance item that is not a block item).
 */
public final class AppearanceTooltip {
    private AppearanceTooltip() {
    }

    public static List<Component> describe(Appearance appearance) {
        String kind = switch (appearance.kind()) { case WOOD -> "tooltip.roundwood.wood"; case MASONRY -> "tooltip.roundwood.masonry"; case BLOCK_SURFACE -> "tooltip.roundwood.surface"; };
        if (!BuiltInRegistries.BLOCK.containsKey(appearance.material())) {
            return List.of(Component.translatable("tooltip.roundwood.unavailable", appearance.material().toString())
                    .withStyle(ChatFormatting.RED));
        }
        Component material = BuiltInRegistries.BLOCK.get(appearance.material()).getName();
        Component line = Component.translatable(kind, material).withStyle(ChatFormatting.GRAY);
        return appearance.paint()
                .map(dye -> List.of(line, Component.translatable("tooltip.roundwood.paint",
                        Component.translatable("color.minecraft." + dye.getName())).withStyle(ChatFormatting.GRAY)))
                .orElse(List.of(line));
    }
}
