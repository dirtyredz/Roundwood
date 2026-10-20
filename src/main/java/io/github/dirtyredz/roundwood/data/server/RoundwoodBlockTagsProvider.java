package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModTags;
import io.github.dirtyredz.roundwood.window.RoundWindowBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Doors join the vanilla door tags (villager door use, pathing, furnace fuel); every block is axe
 * mineable. Windows are deliberately not tagged as glass panes.
 */
public class RoundwoodBlockTagsProvider extends BlockTagsProvider {
    public RoundwoodBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                      ExistingFileHelper existingFiles) {
        super(output, lookup, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        RoundDoorBlock[] doors = ModBlocks.doors().toArray(RoundDoorBlock[]::new);
        RoundWindowBlock[] windows = ModBlocks.windows().toArray(RoundWindowBlock[]::new);
        tag(ModTags.ROUND_DOORS).add(doors);
        tag(ModTags.ROUND_WINDOWS).add(windows);
        tag(BlockTags.DOORS).add(doors);
        tag(BlockTags.WOODEN_DOORS).add(doors);
        tag(BlockTags.MINEABLE_WITH_AXE).add(doors).add(windows);
    }
}
