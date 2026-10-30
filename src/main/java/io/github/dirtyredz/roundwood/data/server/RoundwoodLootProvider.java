package io.github.dirtyredz.roundwood.data.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Wires the block loot sub-provider into a loot table provider. */
public final class RoundwoodLootProvider {
    private RoundwoodLootProvider() {
    }

    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(RoundwoodBlockLoot::new, LootContextParamSets.BLOCK)), registries);
    }
}
