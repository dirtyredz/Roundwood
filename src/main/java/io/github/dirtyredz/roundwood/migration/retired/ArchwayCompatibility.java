package io.github.dirtyredz.roundwood.migration.retired;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.world.item.Item;
import java.util.List;

/** Frozen 0.13.3 identities for automatic world/stack replacement; never advertised or placeable. */
public final class ArchwayCompatibility {
    private ArchwayCompatibility() { }
    public static List<ArchwayBlock> blocks() { return ModBlocks.archways(); }
    public static List<Item> items() { return ModItems.archways(); }
}
