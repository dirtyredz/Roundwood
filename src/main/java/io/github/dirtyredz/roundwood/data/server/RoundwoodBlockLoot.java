package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Every part drops its family's canonical item (explosion rules apply); the block puts the broken
 * object's appearance on it ({@code appearance/item/AppearanceDrops}). A door or trim still yields one
 * item: only the part whose removal started the teardown is evaluated, partners are cleared with
 * drops suppressed. A hidden 0.4.0 alias that is broken before converting drops the canonical item
 * in its historical look.
 */
public class RoundwoodBlockLoot extends BlockLootSubProvider {
    public RoundwoodBlockLoot() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        ModBlocks.canonical().forEach(this::dropSelf);
        LegacyContent.doors().forEach(block -> dropOther(block, ModItems.ROUND_DOOR.get()));
        LegacyContent.windows().forEach(block -> dropOther(block, ModItems.ROUND_WINDOW.get()));
        LegacyContent.trims().forEach(block -> dropOther(block, ModItems.ROUND_TRIM.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(ModBlocks.canonical().stream(), LegacyContent.blocks().stream()).toList();
    }
}
