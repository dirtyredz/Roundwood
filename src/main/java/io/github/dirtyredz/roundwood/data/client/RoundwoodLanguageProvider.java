package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.migration.LegacyAppearances;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * English names: the five canonical blocks, the hidden 0.4.0 aliases under their old names (old
 * stacks in chests still read "Spruce Round Door"), the creative tab, the appearance tooltip lines,
 * the restyling hints and the recipe-viewer preview note.
 */
public class RoundwoodLanguageProvider extends LanguageProvider {
    public RoundwoodLanguageProvider(PackOutput output) {
        super(output, Roundwood.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + Roundwood.MOD_ID, "Roundwood");
        add(ModBlocks.roundDoor(), "Round Door");
        add(ModBlocks.roundWindow(), "Round Window");
        add(ModBlocks.archLeft(), "Round Window Arch, Left");
        add(ModBlocks.archRight(), "Round Window Arch, Right");
        add(ModBlocks.roundTrim(), "Round Trim");
        for (LegacyAppearances.Entry entry : LegacyAppearances.ALL) {
            add("block." + Roundwood.MOD_ID + "." + entry.id(), title(entry.id()));
        }
        add("tooltip.roundwood.wood", "Wood: %s");
        add("tooltip.roundwood.masonry", "Masonry: %s");
        add("tooltip.roundwood.paint", "Paint: %s");
        add("tooltip.roundwood.unavailable", "Material %s is not installed (shown as oak)");
        add("tooltip.roundwood.restyle", "Restyle placed: sneak + planks, or dye");
        add("tooltip.roundwood.preview", "Preview of a possible look");
        add("tooltip.roundwood.preview_note", "Crafting gives oak; restyle it in the world");
        add("message.roundwood.aim_at_leaf", "Aim at the door leaf to restyle or paint it");
        add("message.roundwood.masonry_unpaintable", "Masonry trims cannot be painted");
    }

    /** {@code dark_oak_round_door} becomes "Dark Oak Round Door". */
    private static String title(String id) {
        return Arrays.stream(id.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
