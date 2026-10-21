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

import java.util.concurrent.CompletableFuture;

/**
 * Gameplay tags cover the canonical blocks and the hidden 0.4.0 aliases (an unconverted alias still
 * behaves like a door for villagers and pathing, and mines with an axe); Roundwood's family tags
 * name only the canonical blocks (the window family: all ten windows). Doors join the vanilla door tags; windows are deliberately
 * not glass panes and trims are not doors. Every family mines with an axe whatever its look.
 */
public class RoundwoodBlockTagsProvider extends BlockTagsProvider {
    public RoundwoodBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                      ExistingFileHelper existingFiles) {
        super(output, lookup, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.ROUND_DOORS).add(ModBlocks.roundDoor());
        tag(ModTags.ROUND_WINDOWS).add(ModBlocks.windows().toArray(Block[]::new));
        tag(ModTags.ROUND_TRIMS).add(ModBlocks.roundTrim());
        Block[] doors = concat(ModBlocks.roundDoor(), LegacyContent.doors().toArray(Block[]::new));
        tag(BlockTags.DOORS).add(doors);
        tag(BlockTags.WOODEN_DOORS).add(doors);
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.canonical().toArray(Block[]::new))
                .add(LegacyContent.blocks().toArray(Block[]::new));
    }

    private static Block[] concat(Block first, Block[] rest) {
        Block[] out = new Block[rest.length + 1];
        out[0] = first;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
    }
}
