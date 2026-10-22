package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceSamples;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.appearance.material.PlankMaterials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The showcase's sample looks and their sign labels (extracted from {@code ShowcaseCanvas} in 0.11.0,
 * dk-0dc49165), shared by the sections, through the canvas, and by the GameTest checks, so neither
 * re-derives the sets. Discovered on each call, so a tag or resource reload shows at the next build.
 */
public final class ShowcaseSamples {
    private ShowcaseSamples() {
    }

    /** Every vanilla wood, up to eight present modded planks, every paint on oak, the four masonry materials. */
    public static List<Appearance> all() {
        return AppearanceSamples.forFamily(AppearanceFamily.TRIM, PlankMaterials.present(), MasonryMaterials.blocks());
    }

    /** Every bare wood sample of {@code samples}: the vanilla woods, then the present modded planks. */
    public static List<Appearance> woods(List<Appearance> samples) {
        return samples.stream().filter(a -> a.isWood() && !a.isPainted()).toList();
    }

    /** One oak sample per dye, in dye order. */
    public static List<Appearance> paints(List<Appearance> samples) {
        return samples.stream().filter(Appearance::isPainted).toList();
    }

    public static List<Appearance> masonry(List<Appearance> samples) {
        return samples.stream().filter(a -> !a.isWood()).toList();
    }

    /** A short sign label for an appearance: the material's name, or the dye's. */
    public static String label(Appearance appearance) {
        if (appearance.paint().isPresent()) {
            String dye = appearance.paint().get().getName().replace('_', ' ');
            return Character.toUpperCase(dye.charAt(0)) + dye.substring(1) + " paint";
        }
        ResourceLocation id = appearance.material();
        return BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.get(id).getName().getString() : id.toString();
    }
}
