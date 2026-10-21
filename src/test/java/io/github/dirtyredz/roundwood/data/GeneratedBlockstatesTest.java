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
 * variant (drawn by block-entity renderers), windows (round and arch) are multipart panes. Independent of the
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


    /**
     * Round windows (canonical and every alias): the 0.5.0 centred parts, now also conditioned on
     * {@code position=center}, then the posed parts of the six flush positions. Every state is
     * checked through the multipart's own conditions.
     */
    @Test
    void roundWindowBlockstatesCoverEveryPositionAxisAndConnection() {
        List<String> windows = new ArrayList<>(ModelExpectations.aliasWindows());
        windows.add("round_window");
        assertEquals(16, windows.size());
        for (String id : windows) {
            JsonObject state = json(assets("blockstates/" + id + ".json"));
            assertTrue(state.has("multipart") && !state.has("variants"), id);
            JsonArray parts = state.getAsJsonArray("multipart");
            assertFalse(state.toString().contains("waterlogged"), id + ": no waterlogged conditions");
            assertPart(parts.get(0).getAsJsonObject(), "roundwood:block/round_window", 0, Map.of("axis", "x", "position", "center"), id);
            assertPart(parts.get(1).getAsJsonObject(), "roundwood:block/round_window", 90, Map.of("axis", "z", "position", "center"), id);
            for (String position : List.of("center", "north", "east", "south", "west", "up", "down")) {
                for (String axis : List.of("x", "z")) {
                    for (int mask = 0; mask < 16; mask++) {
                        Map<String, String> values = connections(mask);
                        values.put("axis", axis);
                        values.put("position", position);
                        assertEquals(expectedRound(position, axis, mask), applied(parts, values), id + " " + values);
                    }
                }
            }
        }
    }

    /** Both arch pieces: every orientation, depth and connection set draws exactly the expected parts. */
    @Test
    void archBlockstatesCoverEveryOrientationDepthAndConnection() {
        for (String piece : List.of("round_window_arch_left", "round_window_arch_right")) {
            JsonObject state = json(assets("blockstates/" + piece + ".json"));
            assertTrue(state.has("multipart") && !state.has("variants"), piece);
            assertFalse(state.toString().contains("waterlogged"), piece + ": no waterlogged conditions");
            JsonArray parts = state.getAsJsonArray("multipart");
            for (String orientation : ORIENTATIONS) {
                for (String depth : List.of("center", "negative", "positive")) {
                    for (int mask = 0; mask < 16; mask++) {
                        Map<String, String> values = connections(mask);
                        values.put("orientation", orientation);
                        values.put("depth", depth);
                        assertEquals(expectedArch(piece, orientation, depth, mask), applied(parts, values), piece + " " + values);
                    }
                }
            }
        }
    }

    private static final List<String> SIDES = List.of("north", "east", "south", "west");
    private static final List<String> ORIENTATIONS = List.of("north", "east", "south", "west", "up_north", "up_east",
            "up_south", "up_west", "down_north", "down_east", "down_south", "down_west");

    /** What a round window state should draw, written independently of the generator. */
    private static Set<String> expectedRound(String position, String axis, int mask) {
        Set<String> out = new TreeSet<>();
        if (position.equals("center")) {
            out.add("roundwood:block/round_window@" + (axis.equals("x") ? 0 : 90));
            Map<String, Integer> armTurns = Map.of("north", 0, "east", 90, "south", 180, "west", 270);
            for (String side : axis.equals("x") ? List.of("north", "south") : List.of("east", "west")) {
                if (connected(mask, side)) {
                    out.add("roundwood:block/round_window_arm@" + armTurns.get(side));
                }
            }
            return out;
        }
        if (position.equals("up") || position.equals("down")) {
            posed(out, position.equals("up") ? "round_window_down_ceiling" : "round_window_up_floor", 0);
            return out;
        }
        // Flush on a side: the north-flush pose turned so its flush side lands on that side.
        posed(out, "round_window_back", SIDES.indexOf(position));
        return out;
    }

    /** What an arch state should draw: its canonical model family (upright facing south or top north) turned. */
    private static Set<String> expectedArch(String piece, String orientation, String depth, int mask) {
        Set<String> out = new TreeSet<>();
        if (!orientation.contains("_")) {
            int turns = List.of("south", "west", "north", "east").indexOf(orientation);
            Map<String, String> flush = Map.of("north", "z", "south", "z", "east", "x", "west", "x");
            if (depth.equals("center")) {
                out.add("roundwood:block/" + piece + "@" + 90 * turns);
                for (String side : SIDES) {
                    // across the plane: the two sides on the normal's axis
                    if (flush.get(side).equals(flush.get(orientation)) && connected(mask, side)) {
                        out.add("roundwood:block/" + piece + "_arm_" + SIDES.get(Math.floorMod(SIDES.indexOf(side) - turns, 4))
                                + "@" + 90 * turns);
                    }
                }
                return out;
            }
            // The slab lies against the negative or positive world side of the normal's axis.
            String side = flush.get(orientation).equals("z") ? (depth.equals("negative") ? "north" : "south")
                    : (depth.equals("negative") ? "west" : "east");
            String canonicalSide = SIDES.get(Math.floorMod(SIDES.indexOf(side) - turns, 4));
            String stem = piece + (canonicalSide.equals("north") ? "_back" : "_front");
            posed(out, stem, turns);
            return out;
        }
        String[] parts = orientation.split("_");
        int turns = SIDES.indexOf(parts[1]);
        String level = switch (depth) {
            case "negative" -> "floor";
            case "center" -> "level";
            default -> "ceiling";
        };
        posed(out, piece + "_" + parts[0] + "_" + level, turns);
        return out;
    }

    /** A positioned pose draws its core only, whatever is connected (0.6.1: no glass bridges). */
    private static void posed(Set<String> out, String stem, int turns) {
        out.add("roundwood:block/" + stem + "@" + 90 * turns);
    }

    private static boolean connected(int mask, String side) {
        return (mask & (1 << SIDES.indexOf(side))) != 0;
    }

    private static Map<String, String> connections(int mask) {
        Map<String, String> values = new java.util.HashMap<>();
        for (String side : SIDES) {
            values.put(side, String.valueOf(connected(mask, side)));
        }
        return values;
    }

    /** The parts a state applies, as "model@y"; a duplicate part (drawn twice) fails. */
    static Set<String> applied(JsonArray parts, Map<String, String> state) {
        Set<String> out = new TreeSet<>();
        for (JsonElement p : parts) {
            JsonObject part = p.getAsJsonObject();
            if (!matches(part.getAsJsonObject("when"), state)) {
                continue;
            }
            JsonObject apply = part.getAsJsonObject("apply");
            String key = apply.get("model").getAsString() + "@" + (apply.has("y") ? apply.get("y").getAsInt() : 0);
            assertTrue(out.add(key), "part applied twice: " + key + " for " + state);
        }
        return out;
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
