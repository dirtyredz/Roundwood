package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Roundwood's own family tags, for packs and scripts. */
public final class ModTags {
    public static final TagKey<Block> ROUND_DOORS = BlockTags.create(id("round_doors"));
    public static final TagKey<Block> ROUND_WINDOWS = BlockTags.create(id("round_windows"));
    public static final TagKey<Item> ROUND_DOOR_ITEMS = ItemTags.create(id("round_doors"));
    public static final TagKey<Item> ROUND_WINDOW_ITEMS = ItemTags.create(id("round_windows"));
    /** The archways (0.12.0): their own family, never doors. */
    public static final TagKey<Block> ARCHWAYS = BlockTags.create(id("archways"));
    public static final TagKey<Item> ARCHWAY_ITEMS = ItemTags.create(id("archways"));
    public static final TagKey<Block> ROUND_TRIMS = BlockTags.create(id("round_trims"));
    public static final TagKey<Item> ROUND_TRIM_ITEMS = ItemTags.create(id("round_trims"));

    public static final TagKey<Block> ARCH_PIECES = BlockTags.create(id("arch_pieces"));
    public static final TagKey<Item> ARCH_PIECE_ITEMS = ItemTags.create(id("arch_pieces"));

    private ModTags() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, path);
    }
}
