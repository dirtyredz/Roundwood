package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModTags;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.variant.TrimMaterial;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Doors join the vanilla door tags (villager door use, pathing, furnace fuel); doors, windows and
 * wooden frames are axe mineable, brick frames pickaxe mineable. Windows are deliberately not tagged
 * as glass panes, and frames are not doors.
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
        for (TrimMaterial material : FinishCatalog.TRIMS) {
            RoundTrimBlock trim = ModBlocks.trim(material);
            tag(ModTags.ROUND_TRIMS).add(trim);
            tag(material.masonry() ? BlockTags.MINEABLE_WITH_PICKAXE : BlockTags.MINEABLE_WITH_AXE).add(trim);
        }
    }
}
