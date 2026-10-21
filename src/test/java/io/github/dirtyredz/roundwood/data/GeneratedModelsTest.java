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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolves the checked-in models the way the game does (parent chain, texture variables) and checks
 * what the owner sees: bounded sprite UVs, whole closed doors, square wooden windows with round
 * glass, finish materials and deliberate item transforms. Independent of the mod's own code.
 */
class GeneratedModelsTest {
    private static final Path MODELS = Path.of("src/generated/resources/assets/roundwood/models");
    private static final List<String> NATURAL = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped");
    private static final List<String> PAINTED = List.of("green", "yellow", "red", "blue");
    private static final Map<String, String> TRIMS = Map.of("bamboo", "stripped_bamboo_block",
            "crimson", "stripped_crimson_stem", "warped", "stripped_warped_stem");
    private static final Set<String> TRANSFORMS = Set.of("gui", "fixed", "ground", "thirdperson_righthand",
            "firstperson_righthand");

    /** A model after inheritance: the nearest elements, merged textures and display, gui light. */
    record Resolved(JsonArray elements, Map<String, String> textures, JsonObject display, String guiLight) {
        String texture(String ref) {
            String value = ref;
            for (int i = 0; i < 10 && value != null && value.startsWith("#"); i++) {
                value = textures.get(value.substring(1));
            }
            return value;
        }
    }

    private static JsonObject read(String id) {
        String[] parts = id.split(":", 2);
        assertEquals("roundwood", parts[0], "unexpected namespace in " + id);
        Path file = MODELS.resolve(parts[1] + ".json");
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("cannot read " + file, e);
        }
    }

    static Resolved resolve(String id) {
        List<JsonObject> chain = new ArrayList<>();
        String next = id;
        while (next != null) {
            JsonObject model = read(next);
            chain.add(model);
            next = model.has("parent") ? model.get("parent").getAsString() : null;
        }
        JsonArray elements = null;
        Map<String, String> textures = new HashMap<>();
        JsonObject display = new JsonObject();
        String guiLight = null;
        for (int i = chain.size() - 1; i >= 0; i--) { // root parent first, child overrides
            JsonObject model = chain.get(i);
            if (model.has("elements")) {
                elements = model.getAsJsonArray("elements");
            }
            if (model.has("textures")) {
                model.getAsJsonObject("textures").entrySet().forEach(e -> textures.put(e.getKey(), e.getValue().getAsString()));
            }
            if (model.has("display")) {
                model.getAsJsonObject("display").entrySet().forEach(e -> display.add(e.getKey(), e.getValue()));
            }
            if (model.has("gui_light")) {
                guiLight = model.get("gui_light").getAsString();
            }
        }
        return new Resolved(elements == null ? new JsonArray() : elements, textures, display, guiLight);
    }

    private static double[] vec(JsonObject element, String key) {
        JsonArray a = element.getAsJsonArray(key);
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    private static List<String> allModelIds() throws IOException {
        try (Stream<Path> walk = Files.walk(MODELS)) {
            return walk.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> "roundwood:" + MODELS.relativize(p).toString().replace('\\', '/').replace(".json", ""))
                    .sorted().toList();
        }
    }

    /** Vanilla's default UVs for an element face, from tile-local coordinates. */
    private static double[] defaultUv(String face, double[] f, double[] t) {
        return switch (face) {
            case "down" -> new double[] {f[0], 16 - t[2], t[0], 16 - f[2]};
            case "up" -> new double[] {f[0], f[2], t[0], t[2]};
            case "north" -> new double[] {16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]};
            case "south" -> new double[] {f[0], 16 - t[1], t[0], 16 - f[1]};
            case "west" -> new double[] {f[2], 16 - t[1], t[2], 16 - f[1]};
            case "east" -> new double[] {16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]};
            default -> throw new AssertionError(face);
        };
    }

    @Test
    void everyFaceHasExplicitUvsInsideOneSpriteMatchingItsTilePosition() throws IOException {
        int faces = 0;
        for (String id : allModelIds()) {
            JsonObject model = read(id);
            if (!model.has("elements")) {
                continue;
            }
            for (JsonElement e : model.getAsJsonArray("elements")) {
                JsonObject element = e.getAsJsonObject();
                double[] from = vec(element, "from");
                double[] to = vec(element, "to");
                double[] origin = new double[3];
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(from[axis] >= -16 && to[axis] <= 32, id + ": element outside the model range");
                    origin[axis] = Math.floor(from[axis] / 16) * 16;
                    assertTrue(to[axis] <= origin[axis] + 16, id + ": element crosses a texture tile on axis " + axis);
                }
                double[] localFrom = {from[0] - origin[0], from[1] - origin[1], from[2] - origin[2]};
                double[] localTo = {to[0] - origin[0], to[1] - origin[1], to[2] - origin[2]};
                for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
                    JsonObject f = face.getValue().getAsJsonObject();
                    // Forge omits a uv equal to the vanilla default (taken from absolute coordinates).
                    double[] absolute = defaultUv(face.getKey(), from, to);
                    JsonArray uv = f.has("uv") ? f.getAsJsonArray("uv") : array(absolute);
                    double[] expected = defaultUv(face.getKey(), localFrom, localTo);
                    for (int i = 0; i < 4; i++) {
                        double value = uv.get(i).getAsDouble();
                        assertTrue(Double.isFinite(value) && value >= 0 && value <= 16, id + ": uv out of the sprite " + uv);
                        // Same orientation as vanilla, continuous across tiles because each tile restarts at 0.
                        assertEquals(expected[i], value, 1e-4, id + ": uv " + face.getKey() + " " + uv);
                    }
                    faces++;
                }
            }
        }
        assertTrue(faces > 1000, "suspiciously few faces: " + faces);
    }

    private static JsonArray array(double[] values) {
        JsonArray out = new JsonArray();
        for (double value : values) {
            out.add(value);
        }
        return out;
    }

    /** Pixel (u, v) of the front plane is covered by an element with the given resolved texture. */
    private static boolean frontCovered(Resolved model, int u, int v, String texture) {
        for (JsonElement e : model.elements()) {
            JsonObject element = e.getAsJsonObject();
            double[] from = vec(element, "from");
            double[] to = vec(element, "to");
            if (from[0] <= u + 0.5 && u + 0.5 <= to[0] && from[1] <= v + 0.5 && v + 0.5 <= to[1]) {
                JsonObject south = element.getAsJsonObject("faces").getAsJsonObject("south");
                if (south != null && texture.equals(model.texture(south.get("texture").getAsString()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean inDisc(int size, int diameter, int u, int v) {
        double du = u + 0.5 - size / 2.0;
        double dv = v + 0.5 - size / 2.0;
        return du * du + dv * dv <= diameter * diameter / 4.0;
    }

    private static String panel(String finish) {
        return PAINTED.contains(finish) ? "roundwood:block/panel_" + finish : "minecraft:block/" + finish + "_planks";
    }

    private static String trim(String finish) {
        if (PAINTED.contains(finish)) {
            return "minecraft:block/stripped_oak_log";
        }
        return "minecraft:block/" + TRIMS.getOrDefault(finish, "stripped_" + finish + "_log");
    }

    @Test
    void doorItemsAreTheWholeClosedLeafInTheirFinish() {
        for (String finish : concat()) {
            Resolved item = resolve("roundwood:item/" + finish + "_round_door");
            assertEquals(panel(finish), item.texture("#panel"), finish);
            assertEquals(trim(finish), item.texture("#trim"), finish);
            assertEquals("roundwood:block/brass", item.texture("#brass"), finish);
            for (int u = 0; u < 32; u++) {
                for (int v = 0; v < 32; v++) {
                    boolean disc = inDisc(32, 32, u, v);
                    boolean covered = frontCovered(item, u, v, panel(finish)) || frontCovered(item, u, v, trim(finish))
                            || frontCovered(item, u, v, "roundwood:block/brass");
                    assertEquals(disc, covered, finish + " door face at " + u + "," + v);
                }
            }
            assertDisplay(item, finish + " door", new double[] {16, 16, 14.5});
        }
    }

    @Test
    void windowItemsAreSquareWoodWithRoundGlass() {
        for (String finish : concat()) {
            Resolved item = resolve("roundwood:item/" + finish + "_round_window");
            assertEquals(panel(finish), item.texture("#frame"), finish);
            assertEquals("minecraft:block/glass", item.texture("#glass"), finish);
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    boolean opening = inDisc(16, 12, u, v);
                    assertEquals(!opening, frontCovered(item, u, v, panel(finish)), finish + " wood at " + u + "," + v);
                    assertEquals(opening, frontCovered(item, u, v, "minecraft:block/glass"), finish + " glass at " + u + "," + v);
                }
            }
            assertDisplay(item, finish + " window", new double[] {8, 8, 8});
        }
    }

    /** Every in-hand/inventory context is set, the icon is lit from the front and centred in the slot. */
    private static void assertDisplay(Resolved item, String what, double[] centre) {
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

    @Test
    void doorWorldModelsAreEmptyAndLeafModelsExistForBothHinges() {
        for (String finish : concat()) {
            Resolved world = resolve("roundwood:block/" + finish + "_round_door");
            assertEquals(0, world.elements().size(), finish + ": the world model must not draw the leaf twice");
            assertEquals(panel(finish), world.texture("#particle"), finish + ": break particle");
            for (String hinge : List.of("left", "right")) {
                Resolved leaf = resolve("roundwood:block/" + finish + "_round_door_leaf_" + hinge);
                assertFalse(leaf.elements().isEmpty(), finish + " " + hinge + " leaf has no geometry");
                assertNotNull(leaf.texture("#panel"));
                assertEquals(trim(finish), leaf.texture("#trim"));
            }
        }
    }

    private static List<String> concat() {
        return Stream.concat(NATURAL.stream(), PAINTED.stream()).toList();
    }
}
