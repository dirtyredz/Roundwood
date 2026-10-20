package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** Item tags copied from the block tags. */
public class RoundwoodItemTagsProvider extends ItemTagsProvider {
    public RoundwoodItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                     CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFiles) {
        super(output, lookup, blockTags, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        copy(ModTags.ROUND_DOORS, ModTags.ROUND_DOOR_ITEMS);
        copy(ModTags.ROUND_WINDOWS, ModTags.ROUND_WINDOW_ITEMS);
        copy(BlockTags.DOORS, ItemTags.DOORS);
        copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
    }
}
