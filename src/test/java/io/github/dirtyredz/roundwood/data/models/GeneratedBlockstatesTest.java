package io.github.dirtyredz.roundwood.data.models;

import io.github.dirtyredz.roundwood.data.GeneratedFiles;
import io.github.dirtyredz.roundwood.data.ModelExpectations;

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
 * variant (drawn by block-entity renderers), windows (all ten designs and the aliases) are multipart
 * panes that apply exactly one complete model per state (0.7.0). Independent of the mod's own code.
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

    @Test void modularArchStatesUseEmptyWorldModels() {
        for (String id : GeneratedFiles.ARCH_PIECES) {
            assertSingleVariant(id, "roundwood:block/" + id);
            var model = GeneratedFiles.json(GeneratedFiles.assets("models/block/" + id + ".json"));
            assertFalse(model.has("elements"), "BER must be the only body renderer");
            assertEquals("minecraft:block/oak_planks", model.getAsJsonObject("textures").get("particle").getAsString());
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
     * Round windows (both designs and every alias, which draw the plain round window): every position,
     * axis, connection set and trimmed or face_reversed flag (none of them selects a model: a window grows no
     * connecting glass since 0.13.1), checked through the multipart's own conditions.
     */
    @Test
    void roundWindowBlockstatesCoverEveryPositionAxisAndConnection() {
        Map<String, String> windows = new java.util.LinkedHashMap<>();
        ModelExpectations.aliasWindows().forEach(alias -> windows.put(alias, "round_window"));
        windows.put("round_window", "round_window");
        windows.put("round_window_cross", "round_window");
        assertEquals(17, windows.size());
        for (Map.Entry<String, String> window : windows.entrySet()) {
            String id = window.getKey();
            JsonObject state = json(assets("blockstates/" + id + ".json"));
            assertTrue(state.has("multipart") && !state.has("variants"), id);
            JsonArray parts = state.getAsJsonArray("multipart");
            assertFalse(state.toString().contains("waterlogged"), id + ": no waterlogged conditions");
            for (String position : List.of("center", "north", "east", "south", "west", "up", "down")) {
                for (String axis : List.of("x", "z")) {
                    for (String flags : FLAG_STATES) {
                        for (int mask = 0; mask < 16; mask++) {
                            Map<String, String> values = connections(mask);
                            values.put("axis", axis);
                            values.put("position", position);
                            values.putAll(flagValues(flags));
                            assertEquals(expectedRound(window.getValue(), position, axis, mask),
                                    applied(parts, values), id + " " + values);
                        }
                    }
                }
            }
        }
    }

    /** The eight oriented pieces: every orientation, depth, connection set and trimmed or face_reversed flag (neither selects a model) draws exactly the expected model. */
    @Test
    void orientedBlockstatesCoverEveryOrientationDepthAndConnection() {
        for (String piece : List.of("round_window_arch_left", "round_window_arch_right", "round_window_bottom_left",
                "round_window_bottom_right", "round_window_arch_left_cross", "round_window_arch_right_cross",
                "round_window_bottom_left_cross", "round_window_bottom_right_cross")) {
            JsonObject state = json(assets("blockstates/" + piece + ".json"));
            assertTrue(state.has("multipart") && !state.has("variants"), piece);
            assertFalse(state.toString().contains("waterlogged"), piece + ": no waterlogged conditions");
            JsonArray parts = state.getAsJsonArray("multipart");
            for (String orientation : ORIENTATIONS) {
                for (String depth : List.of("center", "negative", "positive")) {
                    for (String flags : FLAG_STATES) {
                        for (int mask = 0; mask < 16; mask++) {
                            Map<String, String> values = connections(mask);
                            values.put("orientation", orientation);
                            values.put("depth", depth);
                            values.putAll(flagValues(flags));
                            assertEquals(expectedArch(piece.replace("_cross", ""), orientation, depth, mask),
                                    applied(parts, values), piece + " " + values);
                        }
                    }
                }
            }
        }
    }

    private static final List<String> SIDES = List.of("north", "east", "south", "west");
    private static final List<String> ORIENTATIONS = List.of("north", "east", "south", "west", "up_north", "up_east",
            "up_south", "up_west", "down_north", "down_east", "down_south", "down_west");

    /** What a round window state should draw, written independently of the generator. */
    private static Set<String> expectedRound(String design, String position, String axis, int mask) {
        Set<String> out = new TreeSet<>();
        if (position.equals("center")) {
            int turns = axis.equals("x") ? 0 : 1;
            out.add("roundwood:block/" + design + "@" + 90 * turns);
            return out;
        }
        if (position.equals("up") || position.equals("down")) {
            posed(out, design + (position.equals("up") ? "_down_ceiling" : "_up_floor"), 0);
            return out;
        }
        // Flush on a side: the north-flush pose turned so its flush side lands on that side.
        posed(out, design + "_back", SIDES.indexOf(position));
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

    /** Every pose draws its one model, whatever is connected (no glass bridges, 0.6.1 and 0.13.1). */
    private static void posed(Set<String> out, String stem, int turns) {
        out.add("roundwood:block/" + stem + "@" + 90 * turns);
    }

    /** Every trimmed / face_reversed combination, as "trimmed,face_reversed". */
    static final List<String> FLAG_STATES = List.of("false,false", "true,false", "false,true", "true,true");

    /** The trimmed and face_reversed property values of one {@link #FLAG_STATES} entry. */
    static Map<String, String> flagValues(String flags) {
        String[] split = flags.split(",");
        return Map.of("trimmed", split[0], "face_reversed", split[1]);
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

    private static boolean matches(JsonObject when, Map<String, String> state) {
        for (Map.Entry<String, JsonElement> condition : when.entrySet()) {
            if (!condition.getValue().getAsString().equals(state.get(condition.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
