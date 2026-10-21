package io.github.dirtyredz.roundwood.appearance.material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * The masonry a trim can be restyled in: exactly bricks, stone bricks, deepslate bricks and mud
 * bricks, each with Roundwood's own radial brick artwork (keyed by {@link Material#artwork}). Other
 * masonry is deliberately not accepted rather than drawn with a misleading radial approximation.
 * Server-safe.
 */
public final class MasonryMaterials {
    /** One supported material: its block id and the key of its generated artwork. */
    public record Material(ResourceLocation block, String artwork) {
    }

    public static final List<Material> ALL = List.of(
            material("bricks", "brick"),
            material("stone_bricks", "stone_brick"),
            material("deepslate_bricks", "deepslate_brick"),
            material("mud_bricks", "mud_brick"));

    private MasonryMaterials() {
    }

    public static List<ResourceLocation> blocks() {
        return ALL.stream().map(Material::block).toList();
    }

    public static Optional<Material> byBlock(ResourceLocation block) {
        return ALL.stream().filter(m -> m.block().equals(block)).findFirst();
    }

    /** The masonry block id {@code stack} would restyle a trim with, or empty. */
    public static Optional<ResourceLocation> fromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || DonorStacks.carriesPayload(stack)) {
            return Optional.empty();
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(item.getBlock());
        return byBlock(id).map(Material::block);
    }

    private static Material material(String block, String artwork) {
        return new Material(ResourceLocation.fromNamespaceAndPath("minecraft", block), artwork);
    }
}
