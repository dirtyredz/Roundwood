package io.github.dirtyredz.roundwood.appearance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A bounded set of example appearances for previews (recipe viewers) and the showcase: the eleven
 * vanilla woods, up to {@link #MAX_MODDED} other plank materials that are actually present (sorted
 * by id), every paint on oak, and for trims the four masonry materials. Never the wood x paint
 * product, and never a registration list: content does not depend on it.
 */
public final class AppearanceSamples {
    public static final int MAX_MODDED = 8;
    public static final List<ResourceLocation> VANILLA_WOODS = List.of(
            planks("oak"), planks("spruce"), planks("birch"), planks("jungle"), planks("acacia"), planks("dark_oak"),
            planks("mangrove"), planks("cherry"), planks("bamboo"), planks("crimson"), planks("warped"));

    private AppearanceSamples() {
    }

    /**
     * @param presentPlanks every plank material currently recognised (any order, may include vanilla)
     * @param masonry      the supported masonry materials, in display order
     */
    public static List<Appearance> forFamily(AppearanceFamily family, Collection<ResourceLocation> presentPlanks,
                                             List<ResourceLocation> masonry) {
        List<Appearance> out = new ArrayList<>();
        VANILLA_WOODS.forEach(id -> out.add(Appearance.wood(id)));
        modded(presentPlanks).forEach(id -> out.add(Appearance.wood(id)));
        for (DyeColor dye : DyeColor.values()) {
            out.add(Appearance.DEFAULT.painted(dye));
        }
        if (family.accepts(Appearance.Kind.MASONRY)) {
            masonry.forEach(id -> out.add(Appearance.masonry(id)));
        }
        if(family.accepts(Appearance.Kind.BLOCK_SURFACE))
            io.github.dirtyredz.roundwood.appearance.material.SurfaceMaterials.blocks().forEach(id->out.add(Appearance.surface(id)));
        return List.copyOf(out);
    }

    /** Up to {@link #MAX_MODDED} non-vanilla plank ids, sorted, without duplicates. */
    public static List<ResourceLocation> modded(Collection<ResourceLocation> presentPlanks) {
        return presentPlanks.stream().filter(id -> !VANILLA_WOODS.contains(id)).distinct().sorted()
                .limit(MAX_MODDED).toList();
    }

    private static ResourceLocation planks(String wood) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", wood + "_planks");
    }
}
