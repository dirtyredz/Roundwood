package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static io.github.dirtyredz.roundwood.data.ResolvedModels.Resolved;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.array;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test helper: what the generated models should contain, listed independently of the mod's
 * catalogue (a catalogue mistake shows up as a diff), plus the shared item-display assertion.
 */
final class ModelExpectations {
    /** The oak look every canonical model ships with; the client retextures these at runtime. */
    static final String DOOR_PANEL_OAK = "roundwood:block/door_panel_oak";
    static final String BRASS = "roundwood:block/brass";
    static final String GLASS = "roundwood:block/window_glass";
    static final String OAK_PLANKS = "minecraft:block/oak_planks";
    static final String STRIPPED_OAK_LOG = "minecraft:block/stripped_oak_log";
    static final List<String> PARTS = List.of("lower_left", "upper_left", "lower_right", "upper_right");

    /** The eleven natural wood finishes of 0.4.0 (alias ids only). */
    static final List<String> LEGACY_NATURAL = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped");
    static final List<String> LEGACY_PAINTED = List.of("green", "yellow", "red", "blue");
    /** The fifteen 0.4.0 door and window finishes (alias ids only). */
    static final List<String> LEGACY_FINISHES =
            Stream.concat(LEGACY_NATURAL.stream(), LEGACY_PAINTED.stream()).toList();
    static final List<String> LEGACY_MASONRY = List.of("brick", "stone_brick", "deepslate_brick", "mud_brick");
    /** The nineteen 0.4.0 trim materials (alias ids only). */
    static final List<String> LEGACY_TRIM_MATERIALS =
            Stream.concat(LEGACY_FINISHES.stream(), LEGACY_MASONRY.stream()).toList();
    private static final Set<String> TRANSFORMS = Set.of("gui", "fixed", "ground", "thirdperson_righthand",
            "firstperson_righthand");

    private ModelExpectations() {
    }

    /** The 15 alias door ids, e.g. {@code oak_round_door}. */
    static List<String> aliasDoors() {
        return LEGACY_FINISHES.stream().map(f -> f + "_round_door").toList();
    }

    /** The 15 alias window ids. */
    static List<String> aliasWindows() {
        return LEGACY_FINISHES.stream().map(f -> f + "_round_window").toList();
    }

    /** The 19 alias trim ids. */
    static List<String> aliasTrims() {
        return LEGACY_TRIM_MATERIALS.stream().map(m -> m + "_round_trim").toList();
    }

    /** All 49 alias block and item ids. */
    static List<String> aliases() {
        return Stream.of(aliasDoors(), aliasWindows(), aliasTrims()).flatMap(List::stream).toList();
    }

    /** Every in-hand/inventory context is set, the icon is lit from the front and centred in the slot. */
    static void assertDisplay(Resolved item, String what, double[] centre) {
        assertEquals("front", item.guiLight(), what + ": gui_light");
        for (String context : TRANSFORMS) {
            assertTrue(item.display().has(context), what + ": no " + context + " transform");
        }
        JsonObject gui = item.display().getAsJsonObject("gui");
        JsonArray rotation = gui.has("rotation") ? gui.getAsJsonArray("rotation") : array(new double[3]);
        for (JsonElement r : rotation) {
            assertEquals(0, r.getAsDouble(), 1e-9, what + ": the icon should face the viewer");
        }
        double scale = gui.getAsJsonArray("scale").get(0).getAsDouble();
        JsonArray translation = gui.has("translation") ? gui.getAsJsonArray("translation") : array(new double[3]);
        for (int axis = 0; axis < 2; axis++) {
            double placed = translation.get(axis).getAsDouble() + scale * (centre[axis] - 8);
            assertEquals(0, placed, 1e-3, what + ": icon not centred on axis " + axis);
        }
        assertTrue(scale * (centre[0] * 2) <= 16.0001, what + ": icon larger than the slot");
    }
}
