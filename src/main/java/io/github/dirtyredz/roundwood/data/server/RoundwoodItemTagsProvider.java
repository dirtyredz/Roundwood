package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;
import io.github.dirtyredz.roundwood.migration.CrossWindowCompatibility;

/**
 * Item tags name only the canonical items (the window trim is a trim item) (the hidden 0.4.0 alias items are never advertised).
 * {@code roundwood:appearance_planks} is deliberately not generated: a datapack creates it to let
 * plank-like items restyle wood without joining {@code minecraft:planks}.
 */
public class RoundwoodItemTagsProvider extends ItemTagsProvider {
    public RoundwoodItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                     CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFiles) {
        super(output, lookup, blockTags, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.ROUND_DOOR_ITEMS).add(ModItems.ROUND_DOOR.get(), ModItems.ARCHED_DOOR.get(), ModItems.DOUBLE_ARCHED_DOOR.get());
        tag(ModTags.ROUND_WINDOW_ITEMS).add(ModItems.windows().toArray(Item[]::new))
                .add(CrossWindowCompatibility.items().toArray(Item[]::new));
        tag(ModTags.ARCH_PIECE_ITEMS).add(ModItems.archPieces().toArray(Item[]::new));
        tag(ModTags.ARCHWAY_ITEMS).add(ModItems.archways().toArray(Item[]::new));
        tag(ModTags.ROUND_TRIM_ITEMS).add(ModItems.ROUND_TRIM.get(), LegacyContent.windowTrim());
        tag(ItemTags.DOORS).add(ModItems.ROUND_DOOR.get(), ModItems.ARCHED_DOOR.get(), ModItems.DOUBLE_ARCHED_DOOR.get());
        tag(ItemTags.WOODEN_DOORS).add(ModItems.ROUND_DOOR.get(), ModItems.ARCHED_DOOR.get(), ModItems.DOUBLE_ARCHED_DOOR.get());
    }
}
