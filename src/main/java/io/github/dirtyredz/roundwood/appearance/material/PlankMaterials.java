package io.github.dirtyredz.roundwood.appearance.material;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.TreeSet;

/**
 * Which items restyle wood, and as what: any block item tagged {@code minecraft:planks} or
 * {@code roundwood:appearance_planks} (empty by default, for datapacks), identified by its block's
 * registry id. Only the look is used: never the stack's NBT, contents or the donor's behaviour, and a
 * stack carrying block-entity or block-state data is refused. Server-safe (no client classes).
 */
public final class PlankMaterials {
    public static final TagKey<Item> EXTENSION = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID,
            "appearance_planks"));

    private PlankMaterials() {
    }

    /** The plank block id {@code stack} would restyle with, or empty if it is not a plank donor. */
    public static Optional<ResourceLocation> fromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)
                || !(stack.is(ItemTags.PLANKS) || stack.is(EXTENSION)) || DonorStacks.carriesPayload(stack)) {
            return Optional.empty();
        }
        Block block = item.getBlock();
        if (block.defaultBlockState().hasBlockEntity()) {
            return Optional.empty();
        }
        return Optional.of(BuiltInRegistries.BLOCK.getKey(block));
    }

    /** Every plank material currently recognised (both tags), as sorted block ids. */
    public static TreeSet<ResourceLocation> present() {
        TreeSet<ResourceLocation> out = new TreeSet<>();
        for (TagKey<Item> tag : java.util.List.of(ItemTags.PLANKS, EXTENSION)) {
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
                if (holder.value() instanceof BlockItem item && !item.getBlock().defaultBlockState().hasBlockEntity()) {
                    out.add(BuiltInRegistries.BLOCK.getKey(item.getBlock()));
                }
            });
        }
        return out;
    }
}
