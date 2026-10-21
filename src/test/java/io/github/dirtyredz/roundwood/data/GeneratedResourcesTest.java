package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates the checked-in generated tree without starting Minecraft. The expected names are listed
 * here independently of the mod's catalogue, so a catalogue mistake shows up as a diff.
 */
class GeneratedResourcesTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path MAIN = Path.of("src/main/resources");
    private static final List<String> FINISHES = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped", "green", "yellow", "red", "blue");
    private static final List<String> PAINTED = List.of("green", "yellow", "red", "blue");
    private static final List<String> MASONRY = List.of("brick", "stone_brick", "deepslate_brick", "mud_brick");
    private static final List<String> TRIM_MATERIALS = Stream.concat(FINISHES.stream(), MASONRY.stream()).toList();
    private static final List<String> PARTS = List.of("lower_left", "upper_left", "lower_right", "upper_right");
    private static final int TOTAL = 49;
    private static final Set<String> ALLOWED_NAMESPACES = Set.of("minecraft", "roundwood", "forge");
    private static final Pattern NAMESPACED = Pattern.compile("\"#?([a-z0-9_.-]+):[a-z0-9_./-]+\"");

    private static final List<String> BLOCKS = new ArrayList<>();

    @BeforeAll
    static void names() {
        for (String finish : FINISHES) {
            BLOCKS.add(finish + "_round_door");
        }
        for (String finish : FINISHES) {
            BLOCKS.add(finish + "_round_window");
        }
        for (String material : TRIM_MATERIALS) {
            BLOCKS.add(material + "_round_trim");
        }
    }

    private static JsonObject json(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("cannot read " + path, e);
        }
    }

    private static Path assets(String rel) {
        return GENERATED.resolve("assets/roundwood/" + rel);
    }

    private static Path data(String rel) {
        return GENERATED.resolve("data/roundwood/" + rel);
    }

    @Test
    void everyBlockHasStateItemModelLootRecipeAndName() {
        assertEquals(TOTAL, BLOCKS.size());
        assertEquals(TOTAL, new HashSet<>(BLOCKS).size(), "identifiers must be unique");
        JsonObject lang = json(assets("lang/en_us.json"));
        for (String block : BLOCKS) {
            assertTrue(Files.isRegularFile(assets("blockstates/" + block + ".json")), "blockstate " + block);
            assertTrue(Files.isRegularFile(assets("models/item/" + block + ".json")), "item model " + block);
            assertTrue(Files.isRegularFile(data("loot_tables/blocks/" + block + ".json")), "loot " + block);
            String recipe = PAINTED.stream().anyMatch(p -> block.startsWith(p + "_round_")) ? block + "_from_painting" : block;
            assertTrue(Files.isRegularFile(data("recipes/" + recipe + ".json")), "recipe " + recipe);
            assertTrue(lang.has("block.roundwood." + block), "name " + block);
        }
        assertTrue(lang.has("itemGroup.roundwood"));
    }

    @Test
    void generatedModelSetIsExactlyTheExpectedManifest() throws IOException {
        Set<String> block = new TreeSet<>(List.of("template_round_door_leaf_left", "template_round_door_leaf_right",
                "template_round_window", "round_window_arm"));
        Set<String> item = new TreeSet<>(List.of("template_round_door", "template_round_window",
                "template_round_trim", "template_round_trim_whole"));
        for (String finish : FINISHES) {
            block.addAll(List.of(finish + "_round_door", finish + "_round_door_leaf_left", finish + "_round_door_leaf_right",
                    finish + "_round_window"));
            item.addAll(List.of(finish + "_round_door", finish + "_round_window"));
        }
        for (String part : PARTS) {
            block.add("template_round_trim_" + part);
            block.add("template_round_trim_whole_" + part);
        }
        for (String material : TRIM_MATERIALS) {
            PARTS.forEach(part -> block.add(material + "_round_trim_" + part));
            item.add(material + "_round_trim");
        }
        // Exact sets: the 0.1.0 quadrant, open-quadrant and side-fill models must be gone; the 0.3.0 pane arm is present.
        assertEquals(block, names(assets("models/block")));
        assertEquals(item, names(assets("models/item")));
        assertEquals(TOTAL, allJson(assets("blockstates")).size());
        assertEquals(TOTAL, allJson(data("recipes")).size());
        assertEquals(TOTAL, allJson(data("loot_tables/blocks")).size());
        assertEquals(TOTAL, allJson(data("advancements/recipes")).size());
    }

    private static Set<String> names(Path dir) throws IOException {
        Set<String> out = new TreeSet<>();
        for (Path file : allJson(dir)) {
            out.add(file.getFileName().toString().replace(".json", ""));
        }
        return out;
    }

    @Test
    void everyDoorStateUsesTheEmptyWorldModel() {
        for (String finish : FINISHES) {
            JsonObject variants = json(assets("blockstates/" + finish + "_round_door.json")).getAsJsonObject("variants");
            // One unconditional variant covers every facing, half, hinge, column, open and powered value
            // (and properties other mods inject); the leaf itself is drawn by the block-entity renderer.
            assertEquals(Set.of(""), variants.keySet(), finish);
            assertEquals("roundwood:block/" + finish + "_round_door",
                    variants.getAsJsonObject("").get("model").getAsString(), finish);
        }
    }

    @Test
    void windowBlockstatesAreMultipartPanes() {
        // Parts: two cores (one per axis) plus four arms, each tied to the side across its plane.
        List<String[]> arms = List.of(new String[] {"x", "north", "0"}, new String[] {"x", "south", "180"},
                new String[] {"z", "east", "90"}, new String[] {"z", "west", "270"});
        for (String finish : FINISHES) {
            JsonObject state = json(assets("blockstates/" + finish + "_round_window.json"));
            assertTrue(state.has("multipart") && !state.has("variants"), finish);
            JsonArray parts = state.getAsJsonArray("multipart");
            assertEquals(6, parts.size(), finish);
            assertTrue(!state.toString().contains("waterlogged"), finish + ": no waterlogged conditions");
            String core = "roundwood:block/" + finish + "_round_window";
            String arm = "roundwood:block/round_window_arm";
            assertPart(parts.get(0).getAsJsonObject(), core, 0, Map.of("axis", "x"), finish);
            assertPart(parts.get(1).getAsJsonObject(), core, 90, Map.of("axis", "z"), finish);
            for (int i = 0; i < arms.size(); i++) {
                String[] a = arms.get(i);
                assertPart(parts.get(2 + i).getAsJsonObject(), arm, Integer.parseInt(a[2]),
                        Map.of("axis", a[0], a[1], "true"), finish);
            }
            // Coverage: every axis and connection combination matches one core and the across-plane arms.
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
                            JsonObject when = part.getAsJsonObject("when");
                            when.keySet().stream().filter(k -> !k.equals("axis")).forEach(matchedArms::add);
                        }
                    }
                    Set<String> expected = new TreeSet<>();
                    for (String side : axis.equals("x") ? List.of("north", "south") : List.of("east", "west")) {
                        if (values.get(side).equals("true")) {
                            expected.add(side);
                        }
                    }
                    assertEquals(1, cores, finish + " axis " + axis + " mask " + mask + ": core parts");
                    assertEquals(expected, matchedArms, finish + " axis " + axis + " mask " + mask + ": arms");
                }
            }
        }
    }

    private static void assertPart(JsonObject part, String model, int y, Map<String, String> when, String finish) {
        JsonObject apply = part.getAsJsonObject("apply");
        assertEquals(model, apply.get("model").getAsString(), finish);
        assertEquals(y, apply.has("y") ? apply.get("y").getAsInt() : 0, finish + " " + model + " " + when);
        JsonObject condition = part.getAsJsonObject("when");
        assertEquals(when.keySet(), condition.keySet(), finish + " " + model);
        when.forEach((k, v) -> assertEquals(v, condition.get(k).getAsString(), finish + " when " + k));
    }

    private static boolean matches(JsonObject when, Map<String, String> state) {
        for (Map.Entry<String, JsonElement> condition : when.entrySet()) {
            if (!condition.getValue().getAsString().equals(state.get(condition.getKey()))) {
                return false;
            }
        }
        return true;
    }

    @Test
    void everyModelAndTextureReferenceResolves() throws IOException {
        Set<Path> files = allJson(GENERATED.resolve("assets/roundwood"));
        for (Path file : files) {
            JsonObject obj = json(file);
            if (file.toString().replace('\\', '/').contains("/blockstates/")) {
                for (String model : modelsIn(obj)) {
                    assertModelExists(model, file);
                }
            } else if (file.toString().replace('\\', '/').contains("/models/")) {
                if (obj.has("parent")) {
                    assertModelExists(obj.get("parent").getAsString(), file);
                }
                if (obj.has("textures")) {
                    for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("textures").entrySet()) {
                        String texture = e.getValue().getAsString();
                        if (!texture.startsWith("#")) {
                            assertTextureExists(texture, file);
                        }
                    }
                }
            }
        }
    }

    @Test
    void noOptionalModReferences() throws IOException {
        try (Stream<Path> walk = Files.walk(GENERATED)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".json") && !p.toString().contains(".cache")).toList()) {
                Matcher m = NAMESPACED.matcher(Files.readString(file));
                while (m.find()) {
                    assertTrue(ALLOWED_NAMESPACES.contains(m.group(1)), file + " references " + m.group());
                }
            }
        }
    }

    @Test
    void doorTagsAndMineableTags() {
        Path tags = GENERATED.resolve("data/minecraft/tags");
        for (String tag : List.of("blocks/doors", "blocks/wooden_doors", "items/doors", "items/wooden_doors")) {
            String values = json(tags.resolve(tag + ".json")).getAsJsonArray("values").toString();
            for (String finish : FINISHES) {
                assertTrue(values.contains("roundwood:" + finish + "_round_door"), tag + " " + finish);
            }
            assertTrue(!values.contains("_round_window"), tag + " must not list windows");
            assertTrue(!values.contains("_round_trim"), tag + " must not list frames");
        }
        Set<String> axe = tagValues(tags.resolve("blocks/mineable/axe.json"));
        Set<String> pickaxe = tagValues(tags.resolve("blocks/mineable/pickaxe.json"));
        for (String block : BLOCKS) {
            boolean masonry = MASONRY.stream().anyMatch(m -> block.equals(m + "_round_trim"));
            assertEquals(!masonry, axe.contains("roundwood:" + block), "axe " + block);
            assertEquals(masonry, pickaxe.contains("roundwood:" + block), "pickaxe " + block);
        }
    }

    @Test
    void familyTagsListEachFamilyExactly() {
        Path tags = GENERATED.resolve("data/roundwood/tags");
        for (String kind : List.of("blocks", "items")) {
            Set<String> expected = new TreeSet<>();
            TRIM_MATERIALS.forEach(m -> expected.add("roundwood:" + m + "_round_trim"));
            assertEquals(expected, tagValues(tags.resolve(kind + "/round_trims.json")), kind + " round_trims");
            assertTrue(tagValues(tags.resolve(kind + "/round_doors.json")).stream().allMatch(v -> v.endsWith("_round_door")));
            assertTrue(tagValues(tags.resolve(kind + "/round_windows.json")).stream().allMatch(v -> v.endsWith("_round_window")));
        }
    }

    /** Frames: four planks or bricks in the grid corners; painted ones are dyed, never from themselves. */
    @Test
    void frameRecipes() {
        for (String material : TRIM_MATERIALS) {
            String result = "roundwood:" + material + "_round_trim";
            if (PAINTED.contains(material)) {
                JsonObject recipe = json(data("recipes/" + material + "_round_trim_from_painting.json"));
                assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
                String ingredients = recipe.getAsJsonArray("ingredients").toString();
                assertTrue(ingredients.contains("roundwood:oak_round_trim"), material + ": not dyed from oak");
                assertTrue(ingredients.contains("minecraft:" + material + "_dye"), material + ": dye");
                assertTrue(!ingredients.contains(result), material + ": a frame dyed from itself");
                continue;
            }
            JsonObject recipe = json(data("recipes/" + material + "_round_trim.json"));
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            assertEquals("[\"M M\",\"   \",\"M M\"]", recipe.getAsJsonArray("pattern").toString(), material);
            String key = recipe.getAsJsonObject("key").getAsJsonObject("M").get("item").getAsString();
            String expected = MASONRY.contains(material) ? "minecraft:" + material + "s" : "minecraft:" + material + "_planks";
            assertEquals(expected, key, material + ": corner material");
            assertEquals(result, recipe.getAsJsonObject("result").get("item").getAsString());
            assertTrue(!recipe.getAsJsonObject("result").has("count"), material + ": one frame per craft");
        }
    }

    private static Set<String> tagValues(Path file) {
        Set<String> out = new TreeSet<>();
        json(file).getAsJsonArray("values").forEach(v -> out.add(v.getAsString()));
        return out;
    }

    private static Set<Path> allJson(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return new TreeSet<>(walk.filter(p -> p.toString().endsWith(".json")).toList());
        }
    }

    private static List<String> modelsIn(JsonObject blockstate) {
        List<String> models = new ArrayList<>();
        if (blockstate.has("variants")) {
            blockstate.getAsJsonObject("variants").entrySet()
                    .forEach(e -> models.add(e.getValue().getAsJsonObject().get("model").getAsString()));
        }
        if (blockstate.has("multipart")) {
            blockstate.getAsJsonArray("multipart")
                    .forEach(p -> models.add(p.getAsJsonObject().getAsJsonObject("apply").get("model").getAsString()));
        }
        return models;
    }

    private static void assertModelExists(String model, Path from) {
        String[] parts = model.split(":", 2);
        if (parts.length != 2) {
            fail(from + ": unqualified model " + model);
        }
        if (parts[0].equals("roundwood")) {
            assertTrue(Files.isRegularFile(GENERATED.resolve("assets/roundwood/models/" + parts[1] + ".json")),
                    from + ": missing model " + model);
        } else {
            assertEquals("minecraft", parts[0], from + ": foreign model " + model);
        }
    }

    private static void assertTextureExists(String texture, Path from) {
        String[] parts = texture.split(":", 2);
        if (parts.length != 2) {
            fail(from + ": unqualified texture " + texture);
        }
        if (parts[0].equals("roundwood")) {
            assertTrue(Files.isRegularFile(MAIN.resolve("assets/roundwood/textures/" + parts[1] + ".png")),
                    from + ": missing texture " + texture);
        } else {
            // Vanilla textures were resolved by datagen's ExistingFileHelper; check the shape only.
            assertEquals("minecraft", parts[0], from + ": foreign texture " + texture);
            assertTrue(parts[1].startsWith("block/"), from + ": " + texture);
        }
    }
}
