package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The checked-in blockstates of the canonical and alias blocks: doors and trims are one empty
 * variant (drawn by block-entity renderers), windows are the multipart pane. Independent of the
 * mod's own code.
 */
class GeneratedBlockstatesTest {
    private static final Path STATES = Path.of("src/generated/resources/assets/roundwood/blockstates");

    private static JsonObject json(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("cannot read " + path, e);
        }
    }

    private static Path assets(String rel) {
        return STATES.resolve(rel.replace("blockstates/", ""));
    }

    /** Doors and trims: one unconditional variant; the look is drawn from the block entity. */
    @Test
    void doorAndTrimStatesUseTheSingleEmptyWorldModel() {
        List<String> doors = new ArrayList<>(ModelExpectations.aliasDoors());
        doors.add("round_door");
        List<String> trims = new ArrayList<>(ModelExpectations.aliasTrims());
        trims.add("round_trim");
        for (String id : doors) {
            assertSingleVariant(id, "roundwood:block/round_door");
        }
        for (String id : trims) {
            assertSingleVariant(id, "roundwood:block/round_trim");
        }
    }

    private static void assertSingleVariant(String id, String model) {
        JsonObject state = json(assets("blockstates/" + id + ".json"));
        assertFalse(state.has("multipart"), id);
        JsonObject variants = state.getAsJsonObject("variants");
        // One unconditional variant covers every facing, half, hinge, column, open and powered value
        // (and properties other mods inject); the leaf or ring is drawn by the block-entity renderer.
        assertEquals(Set.of(""), variants.keySet(), id);
        assertEquals(model, variants.getAsJsonObject("").get("model").getAsString(), id);
    }

    @Test
    void windowBlockstatesAreMultipartPanesForCanonicalAndEveryAlias() {
        List<String> windows = new ArrayList<>(ModelExpectations.aliasWindows());
        windows.add("round_window");
        assertEquals(16, windows.size());
        // Parts: two cores (one per axis) plus four arms, each tied to the side across its plane.
        List<String[]> arms = List.of(new String[] {"x", "north", "0"}, new String[] {"x", "south", "180"},
                new String[] {"z", "east", "90"}, new String[] {"z", "west", "270"});
        for (String id : windows) {
            JsonObject state = json(assets("blockstates/" + id + ".json"));
            assertTrue(state.has("multipart") && !state.has("variants"), id);
            JsonArray parts = state.getAsJsonArray("multipart");
            assertEquals(6, parts.size(), id);
            assertFalse(state.toString().contains("waterlogged"), id + ": no waterlogged conditions");
            String core = "roundwood:block/round_window";
            String arm = "roundwood:block/round_window_arm";
            assertPart(parts.get(0).getAsJsonObject(), core, 0, Map.of("axis", "x"), id);
            assertPart(parts.get(1).getAsJsonObject(), core, 90, Map.of("axis", "z"), id);
            for (int i = 0; i < arms.size(); i++) {
                String[] a = arms.get(i);
                assertPart(parts.get(2 + i).getAsJsonObject(), arm, Integer.parseInt(a[2]),
                        Map.of("axis", a[0], a[1], "true"), id);
            }
            assertCoverage(parts, core, id);
        }
    }

    /** Every axis and connection combination matches exactly one core and the across-plane arms. */
    private static void assertCoverage(JsonArray parts, String core, String id) {
        for (String axis : List.of("x", "z")) {
            for (int mask = 0; mask < 16; mask++) {
                Map<String, String> values = Map.of("axis", axis, "north", String.valueOf((mask & 1) != 0),
                        "east", String.valueOf((mask & 2) != 0), "south", String.valueOf((mask & 4) != 0),
                        "west", String.valueOf((mask & 8) != 0));
                Set<String> matchedArms = new TreeSet<>();
                int cores = 0;
                for (JsonElement p : parts) {
                    JsonObject part = p.getAsJsonObject();
                    if (!matches(part.getAsJsonObject("when"), values)) {
                        continue;
                    }
                    if (part.getAsJsonObject("apply").get("model").getAsString().equals(core)) {
                        cores++;
                    } else {
                        part.getAsJsonObject("when").keySet().stream().filter(k -> !k.equals("axis")).forEach(matchedArms::add);
                    }
                }
                Set<String> expected = new TreeSet<>();
                for (String side : axis.equals("x") ? List.of("north", "south") : List.of("east", "west")) {
                    if (values.get(side).equals("true")) {
                        expected.add(side);
                    }
                }
                assertEquals(1, cores, id + " axis " + axis + " mask " + mask + ": core parts");
                assertEquals(expected, matchedArms, id + " axis " + axis + " mask " + mask + ": arms");
            }
        }
    }

    private static void assertPart(JsonObject part, String model, int y, Map<String, String> when, String id) {
        JsonObject apply = part.getAsJsonObject("apply");
        assertEquals(model, apply.get("model").getAsString(), id);
        assertEquals(y, apply.has("y") ? apply.get("y").getAsInt() : 0, id + " " + model + " " + when);
        JsonObject condition = part.getAsJsonObject("when");
        assertEquals(when.keySet(), condition.keySet(), id + " " + model);
        when.forEach((k, v) -> assertEquals(v, condition.get(k).getAsString(), id + " when " + k));
    }

    private static boolean matches(JsonObject when, Map<String, String> state) {
        for (Map.Entry<String, JsonElement> condition : when.entrySet()) {
            if (!condition.getValue().getAsString().equals(state.get(condition.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
