package io.github.dirtyredz.roundwood.variant;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.List;
import java.util.Optional;

/**
 * The authoritative list of finishes, in creative-tab order. Registration, recipes, models and names
 * all iterate this list; nothing else enumerates woods or paints.
 */
public final class FinishCatalog {
    public static final List<Finish> NATURAL = List.of(
            natural("oak", "Oak", BlockSetType.OAK, Blocks.OAK_DOOR, Items.OAK_PLANKS, "oak_planks", "stripped_oak_log"),
            natural("spruce", "Spruce", BlockSetType.SPRUCE, Blocks.SPRUCE_DOOR, Items.SPRUCE_PLANKS, "spruce_planks", "stripped_spruce_log"),
            natural("birch", "Birch", BlockSetType.BIRCH, Blocks.BIRCH_DOOR, Items.BIRCH_PLANKS, "birch_planks", "stripped_birch_log"),
            natural("jungle", "Jungle", BlockSetType.JUNGLE, Blocks.JUNGLE_DOOR, Items.JUNGLE_PLANKS, "jungle_planks", "stripped_jungle_log"),
            natural("acacia", "Acacia", BlockSetType.ACACIA, Blocks.ACACIA_DOOR, Items.ACACIA_PLANKS, "acacia_planks", "stripped_acacia_log"),
            natural("dark_oak", "Dark Oak", BlockSetType.DARK_OAK, Blocks.DARK_OAK_DOOR, Items.DARK_OAK_PLANKS, "dark_oak_planks", "stripped_dark_oak_log"),
            natural("mangrove", "Mangrove", BlockSetType.MANGROVE, Blocks.MANGROVE_DOOR, Items.MANGROVE_PLANKS, "mangrove_planks", "stripped_mangrove_log"),
            natural("cherry", "Cherry", BlockSetType.CHERRY, Blocks.CHERRY_DOOR, Items.CHERRY_PLANKS, "cherry_planks", "stripped_cherry_log"),
            natural("bamboo", "Bamboo", BlockSetType.BAMBOO, Blocks.BAMBOO_DOOR, Items.BAMBOO_PLANKS, "bamboo_planks", "stripped_bamboo_block"),
            natural("crimson", "Crimson", BlockSetType.CRIMSON, Blocks.CRIMSON_DOOR, Items.CRIMSON_PLANKS, "crimson_planks", "stripped_crimson_stem"),
            natural("warped", "Warped", BlockSetType.WARPED, Blocks.WARPED_DOOR, Items.WARPED_PLANKS, "warped_planks", "stripped_warped_stem"));

    public static final List<Finish> PAINTED = List.of(
            painted(DyeColor.GREEN, "Green"),
            painted(DyeColor.YELLOW, "Yellow"),
            painted(DyeColor.RED, "Red"),
            painted(DyeColor.BLUE, "Blue"));

    public static final List<Finish> ALL = concat(NATURAL, PAINTED);

    /** Painted finishes are built on oak: the paint recipe starts from this finish. */
    public static final Finish PAINT_BASE = NATURAL.get(0);

    private FinishCatalog() {
    }

    private static Finish natural(String id, String name, BlockSetType type, Block door, Item planks,
                                  String planksTexture, String trimTexture) {
        return new Finish(id, name, type, door, planks, vanilla(planksTexture), vanilla(trimTexture), Optional.empty());
    }

    private static Finish painted(DyeColor color, String name) {
        String id = color.getName();
        return new Finish(id, name, BlockSetType.OAK, Blocks.OAK_DOOR, Items.OAK_PLANKS,
                ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "block/panel_" + id), vanilla("stripped_oak_log"),
                Optional.of(color));
    }

    private static ResourceLocation vanilla(String texture) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "block/" + texture);
    }

    private static List<Finish> concat(List<Finish> a, List<Finish> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }
}
