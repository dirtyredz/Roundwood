package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.world.item.Item;
import java.util.List;

/** Retired IDs retain 0.13.3 behavior; no conversion or payload-custody policy. */
public final class ArchwayCompatibility {
    private ArchwayCompatibility() { }
    public static List<ArchwayBlock> blocks() { return ModBlocks.archways(); }
    public static List<Item> items() { return ModItems.archways(); }
}
