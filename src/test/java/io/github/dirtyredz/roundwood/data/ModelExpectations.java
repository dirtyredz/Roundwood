package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
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
    static final List<String> NATURAL = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped");
    static final List<String> PAINTED = List.of("green", "yellow", "red", "blue");
    static final List<String> FINISHES = Stream.concat(NATURAL.stream(), PAINTED.stream()).toList();
    static final List<String> MASONRY = List.of("brick", "stone_brick", "deepslate_brick", "mud_brick");
    static final List<String> TRIMS = Stream.concat(FINISHES.stream(), MASONRY.stream()).toList();
    private static final Map<String, String> STRIPPED = Map.of("bamboo", "stripped_bamboo_block",
            "crimson", "stripped_crimson_stem", "warped", "stripped_warped_stem");
    private static final Set<String> TRANSFORMS = Set.of("gui", "fixed", "ground", "thirdperson_righthand",
            "firstperson_righthand");

    private ModelExpectations() {
    }

    /** The window's wooden face: the finish's planks, or Roundwood's painted panel. */
    static String windowWood(String finish) {
        return PAINTED.contains(finish) ? "roundwood:block/panel_" + finish : "minecraft:block/" + finish + "_planks";
    }

    /** The door leaf's own vertical-board panel (32 x 32, one image across the whole door). */
    static String doorPanel(String finish) {
        return "roundwood:block/door_panel_" + finish;
    }

    /** The natural wood's stripped log (used by the natural wooden frames). */
    static String stripped(String finish) {
        return "minecraft:block/" + STRIPPED.getOrDefault(finish, "stripped_" + finish + "_log");
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
