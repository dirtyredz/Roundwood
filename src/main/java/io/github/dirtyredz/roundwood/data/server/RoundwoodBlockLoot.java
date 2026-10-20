package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

/**
 * Every block drops itself (explosion rules apply). A door assembly still yields one item: only the
 * part whose removal started the teardown is evaluated; partners are cleared with drops suppressed.
 * Windows drop themselves without Silk Touch.
 */
public class RoundwoodBlockLoot extends BlockLootSubProvider {
    public RoundwoodBlockLoot() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        ModBlocks.all().forEach(this::dropSelf);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.all();
    }
}
