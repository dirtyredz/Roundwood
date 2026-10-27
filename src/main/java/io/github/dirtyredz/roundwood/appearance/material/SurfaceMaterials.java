package io.github.dirtyredz.roundwood.appearance.material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;

/** Cosmetic full-block finishes. Wax is normalized; these stored looks never oxidize. */
public final class SurfaceMaterials {
    private static final List<String> METALS=List.of("iron_block","gold_block","diamond_block","emerald_block","netherite_block");
    private static final List<String> COPPER=List.of("copper_block","exposed_copper","weathered_copper","oxidized_copper");
    private static final List<String> QUARTZ=List.of("quartz_block","smooth_quartz","chiseled_quartz_block","quartz_bricks","quartz_pillar");
    private SurfaceMaterials() {}
    public static List<ResourceLocation> blocks() {
        return java.util.stream.Stream.of(METALS,COPPER,QUARTZ).flatMap(List::stream)
                .map(name->ResourceLocation.fromNamespaceAndPath("minecraft",name)).toList();
    }
    public static Optional<ResourceLocation> normalize(ResourceLocation block) {
        if(!block.getNamespace().equals("minecraft")) return Optional.empty();
        String name=block.getPath();
        if(name.startsWith("waxed_") && COPPER.contains(name.substring(6))) name=name.substring(6);
        return METALS.contains(name) || COPPER.contains(name) || QUARTZ.contains(name)
                ? Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft",name)) : Optional.empty();
    }
    public static Optional<ResourceLocation> fromStack(ItemStack stack) {
        if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || DonorStacks.carriesPayload(stack)) return Optional.empty();
        return normalize(BuiltInRegistries.BLOCK.getKey(item.getBlock()));
    }
}
