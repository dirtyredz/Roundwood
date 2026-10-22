package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gameplay tags cover the canonical blocks and the hidden 0.4.0 aliases (an unconverted alias still
 * behaves like a door for villagers and pathing, and mines with an axe); Roundwood's family tags
 * name only the canonical blocks (the doors: round and arched; the windows: all 26; the archways,
 * 0.12.0: both designs). Doors join the vanilla door tags; windows are deliberately
 * not glass panes, and trims and archways are not doors. Every family mines with an axe whatever its look.
 */
public class RoundwoodBlockTagsProvider extends BlockTagsProvider {
    public RoundwoodBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                      ExistingFileHelper existingFiles) {
        super(output, lookup, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.ROUND_DOORS).add(ModBlocks.doors().toArray(Block[]::new));
        tag(ModTags.ROUND_WINDOWS).add(ModBlocks.windows().toArray(Block[]::new));
        tag(ModTags.ARCHWAYS).add(ModBlocks.archways().toArray(Block[]::new));
        tag(ModTags.ROUND_TRIMS).add(ModBlocks.roundTrim());
        List<Block> doors = new ArrayList<>(ModBlocks.doors());
        doors.addAll(LegacyContent.doors());
        tag(BlockTags.DOORS).add(doors.toArray(Block[]::new));
        tag(BlockTags.WOODEN_DOORS).add(doors.toArray(Block[]::new));
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.canonical().toArray(Block[]::new))
                .add(LegacyContent.blocks().toArray(Block[]::new));
    }
}
