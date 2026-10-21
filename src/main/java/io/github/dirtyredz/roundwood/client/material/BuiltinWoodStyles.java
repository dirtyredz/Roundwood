package io.github.dirtyredz.roundwood.client.material;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * Artwork bindings kept for the eleven vanilla woods (artwork, not a content list): Roundwood's own
 * full-height door boards ({@code door_panel_<wood>}, unchanged since 0.4.0) and the stripped log
 * the wooden trims have always shown. Any other wood gets its donor's plank sprite (windows, trims)
 * or the neutral boards tinted to the donor's colour (doors).
 */
public final class BuiltinWoodStyles {
    /** The two artwork bindings of a vanilla wood. */
    public record Style(ResourceLocation doorPanel, ResourceLocation trimLog) {
    }

    /** Roundwood's neutral boards, tinted per donor for unfamiliar woods. */
    public static final ResourceLocation NEUTRAL_DOOR_PANEL = roundwood("block/door_panel_neutral");
    public static final ResourceLocation OAK_DOOR_PANEL = roundwood("block/door_panel_oak");
    public static final ResourceLocation OAK_PLANKS = vanilla("block/oak_planks");
    public static final ResourceLocation OAK_TRIM_LOG = vanilla("block/stripped_oak_log");

    private static final Map<ResourceLocation, Style> STYLES = Map.ofEntries(
            style("oak", "stripped_oak_log"),
            style("spruce", "stripped_spruce_log"),
            style("birch", "stripped_birch_log"),
            style("jungle", "stripped_jungle_log"),
            style("acacia", "stripped_acacia_log"),
            style("dark_oak", "stripped_dark_oak_log"),
            style("mangrove", "stripped_mangrove_log"),
            style("cherry", "stripped_cherry_log"),
            style("bamboo", "stripped_bamboo_block"),
            style("crimson", "stripped_crimson_stem"),
            style("warped", "stripped_warped_stem"));

    private BuiltinWoodStyles() {
    }

    /** The bindings for a plank block id, if it is one of the vanilla woods. */
    public static Optional<Style> of(ResourceLocation planks) {
        return Optional.ofNullable(STYLES.get(planks));
    }

    private static Map.Entry<ResourceLocation, Style> style(String wood, String log) {
        return Map.entry(ResourceLocation.fromNamespaceAndPath("minecraft", wood + "_planks"),
                new Style(roundwood("block/door_panel_" + wood), vanilla("block/" + log)));
    }

    private static ResourceLocation roundwood(String path) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, path);
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
