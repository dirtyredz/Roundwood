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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates the checked-in generated tree without starting Minecraft. Since 0.6.0 there are five
 * canonical blocks (round_door, round_window, the two arch pieces, round_trim) whose look lives on
 * block entities, plus the 49 ids of 0.4.0 kept as hidden aliases. The expected names are listed here independently of
 * the mod's catalogue, so a catalogue mistake shows up as a diff.
 */
class GeneratedResourcesTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path MAIN = Path.of("src/main/resources");
    private static final List<String> CANONICAL = List.of("round_door", "round_window", "round_window_arch_left",
            "round_window_arch_right", "round_trim");
    private static final List<String> ARCHES = List.of("round_window_arch_left", "round_window_arch_right");
    private static final List<String> ALIASES = ModelExpectations.aliases();
    private static final List<String> BLOCKS = Stream.concat(CANONICAL.stream(), ALIASES.stream()).toList();
    private static final List<String> PARTS = ModelExpectations.PARTS;
    private static final Set<String> ALLOWED_NAMESPACES = Set.of("minecraft", "roundwood", "forge");
    private static final Pattern NAMESPACED = Pattern.compile("\"#?([a-z0-9_.-]+):[a-z0-9_./-]+\"");

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

    /** The canonical block an id belongs to: itself, or the family of an alias. */
    private static String family(String id) {
        if (id.endsWith("_round_door") || id.equals("round_door")) {
            return "round_door";
        }
        if (ARCHES.contains(id)) {
            return id;
        }
        return id.endsWith("_round_window") || id.equals("round_window") ? "round_window" : "round_trim";
    }

    /** "dark_oak_round_door" -> "Dark Oak Round Door", written independently of the mod. */
    private static String englishName(String id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            out.append(out.length() == 0 ? "" : " ").append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }

    @Test
    void expectedIdentifierCountsAreRight() {
        assertEquals(15, ModelExpectations.aliasDoors().size());
        assertEquals(15, ModelExpectations.aliasWindows().size());
        assertEquals(19, ModelExpectations.aliasTrims().size());
        assertEquals(49, ALIASES.size());
        assertEquals(54, BLOCKS.size());
        assertEquals(54, new HashSet<>(BLOCKS).size(), "identifiers must be unique");
    }

    @Test
    void everyBlockHasStateItemModelLootAndName() {
        JsonObject lang = json(assets("lang/en_us.json"));
        for (String block : BLOCKS) {
            assertTrue(Files.isRegularFile(assets("blockstates/" + block + ".json")), "blockstate " + block);
            assertTrue(Files.isRegularFile(assets("models/item/" + block + ".json")), "item model " + block);
            assertTrue(Files.isRegularFile(data("loot_tables/blocks/" + block + ".json")), "loot " + block);
            assertTrue(lang.has("block.roundwood." + block), "name " + block);
        }
        assertTrue(lang.has("itemGroup.roundwood"));
    }

    @Test
    void generatedSetsAreExactlyTheExpectedManifests() throws IOException {
        Set<String> block = new TreeSet<>(List.of("template_round_door_leaf_left", "template_round_door_leaf_right",
                "round_door_leaf_left", "round_door_leaf_right", "round_door", "template_round_window", "round_window",
                "round_window_arm", "round_trim"));
        Set<String> item = new TreeSet<>(List.of("template_round_door", "round_door", "template_round_window",
                "round_window", "template_round_trim", "template_round_trim_whole", "round_trim", "round_trim_whole"));
        block.addAll(windowPaneModels());
        item.addAll(ARCHES);
        for (String part : PARTS) {
            block.add("template_round_trim_" + part);
            block.add("template_round_trim_whole_" + part);
            block.add("round_trim_" + part);
            block.add("round_trim_whole_" + part);
        }
        item.addAll(ALIASES);
        assertEquals(25 + 25, block.size());
        assertEquals(59, item.size());
        // Exact sets: no per-finish models remain; the 49 old ids are only item-model aliases.
        assertEquals(block, names(assets("models/block")));
        assertEquals(item, names(assets("models/item")));
        assertEquals(new TreeSet<>(BLOCKS), names(assets("blockstates")));
        assertEquals(new TreeSet<>(BLOCKS), names(data("loot_tables/blocks")));
        assertEquals(new TreeSet<>(CANONICAL), names(data("recipes")));
        assertEquals(new TreeSet<>(CANONICAL), names(data("advancements/recipes")));
        assertEquals(54, allJson(assets("blockstates")).size());
        assertEquals(50, allJson(assets("models/block")).size());
        assertEquals(59, allJson(assets("models/item")).size());
        assertEquals(5, allJson(data("recipes")).size());
        assertEquals(54, allJson(data("loot_tables/blocks")).size());
        assertEquals(5, allJson(data("advancements/recipes")).size());
    }

    /**
     * The posed window models, named independently: per canonical pose a core, plus (centred upright
     * only, since 0.6.1) an arm per side across the plane. Round window: flush against the north side
     * ({@code _back}), on the ceiling ({@code _down_ceiling}) and on the floor ({@code _up_floor}).
     * Each arch piece: upright centred (two across-plane arms), upright behind or in front of centre,
     * and front up or down at each of three heights. No hub, cap or positioned arm model exists.
     */
    static Set<String> windowPaneModels() {
        Set<String> out = new TreeSet<>();
        out.add("round_window_back");
        out.add("round_window_down_ceiling");
        out.add("round_window_up_floor");
        for (String arch : ARCHES) {
            out.add(arch);
            out.add(arch + "_arm_north");
            out.add(arch + "_arm_south");
            out.add(arch + "_back");
            out.add(arch + "_front");
            for (String front : List.of("up", "down")) {
                for (String level : List.of("floor", "level", "ceiling")) {
                    out.add(arch + "_" + front + "_" + level);
                }
            }
        }
        return out;
    }

    private static Set<String> names(Path dir) throws IOException {
        Set<String> out = new TreeSet<>();
        for (Path file : allJson(dir)) {
            out.add(file.getFileName().toString().replace(".json", ""));
        }
        return out;
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
    void doorTagsListTheCanonicalAndAliasDoorsOnly() {
        Path tags = GENERATED.resolve("data/minecraft/tags");
        Set<String> blockDoors = new TreeSet<>();
        blockDoors.add("roundwood:round_door");
        ModelExpectations.aliasDoors().forEach(d -> blockDoors.add("roundwood:" + d));
        assertEquals(16, blockDoors.size());
        for (String tag : List.of("blocks/doors", "blocks/wooden_doors")) {
            assertEquals(blockDoors, tagValues(tags.resolve(tag + ".json")), tag);
        }
        for (String tag : List.of("items/doors", "items/wooden_doors")) {
            assertEquals(Set.of("roundwood:round_door"), tagValues(tags.resolve(tag + ".json")), tag);
        }
    }

    @Test
    void everyBlockIsMineableWithTheAxeAndNothingWithThePickaxe() {
        Path tags = GENERATED.resolve("data/minecraft/tags/blocks/mineable");
        Set<String> axe = new TreeSet<>();
        BLOCKS.forEach(b -> axe.add("roundwood:" + b));
        assertEquals(axe, tagValues(tags.resolve("axe.json")));
        Path pickaxe = tags.resolve("pickaxe.json");
        if (Files.exists(pickaxe)) {
            assertTrue(tagValues(pickaxe).stream().noneMatch(v -> v.startsWith("roundwood:")), "no roundwood block is a pickaxe block");
        }
    }

    @Test
    void familyTagsListOnlyTheCanonicalBlocks() {
        Path tags = GENERATED.resolve("data/roundwood/tags");
        for (String kind : List.of("blocks", "items")) {
            assertEquals(Set.of("roundwood:round_door"), tagValues(tags.resolve(kind + "/round_doors.json")), kind);
            assertEquals(Set.of("roundwood:round_window", "roundwood:round_window_arch_left", "roundwood:round_window_arch_right"),
                    tagValues(tags.resolve(kind + "/round_windows.json")), kind);
            assertEquals(Set.of("roundwood:round_trim"), tagValues(tags.resolve(kind + "/round_trims.json")), kind);
        }
    }

    @Test
    void theFiveRecipesAreExactlyAsDesigned() {
        assertRecipe("round_door", List.of("PPP", "PNP", "PPP"),
                Map.of("P", "{\"tag\":\"minecraft:planks\"}", "N", "{\"item\":\"minecraft:gold_nugget\"}"));
        assertRecipe("round_window", List.of(" P ", "PGP", " P "),
                Map.of("P", "{\"tag\":\"minecraft:planks\"}", "G", "{\"item\":\"minecraft:glass_pane\"}"));
        assertRecipe("round_trim", List.of("P P", "   ", "P P"), Map.of("P", "{\"tag\":\"minecraft:planks\"}"));
        Map<String, String> arch = Map.of("P", "{\"tag\":\"minecraft:planks\"}", "G", "{\"item\":\"minecraft:glass_pane\"}");
        assertRecipe("round_window_arch_left", List.of("PP", "PG", "P "), arch);
        assertRecipe("round_window_arch_right", List.of("PPP", " G ", " P "), arch);
    }

    /**
     * No two Roundwood recipes can fill the same crafting grid: every placement of each pattern, and
     * of its mirror image (shaped recipes match mirrored), is compared on a 3 x 3 grid.
     */
    @Test
    void noTwoRecipesMatchTheSameGridEvenMirrored() {
        Map<String, Set<String>> grids = new TreeMap<>();
        for (String name : CANONICAL) {
            JsonObject recipe = json(data("recipes/" + name + ".json"));
            List<String> rows = new ArrayList<>();
            recipe.getAsJsonArray("pattern").forEach(row -> rows.add(row.getAsString()));
            JsonObject key = recipe.getAsJsonObject("key");
            Set<String> placements = new HashSet<>();
            for (List<String> pattern : List.of(rows, mirror(rows))) {
                placements.addAll(placements(pattern, key));
            }
            grids.put(name, placements);
        }
        for (String a : grids.keySet()) {
            for (String b : grids.keySet()) {
                if (a.compareTo(b) < 0) {
                    Set<String> shared = new HashSet<>(grids.get(a));
                    shared.retainAll(grids.get(b));
                    assertTrue(shared.isEmpty(), a + " and " + b + " both match " + shared);
                }
            }
        }
    }

    private static List<String> mirror(List<String> rows) {
        return rows.stream().map(row -> new StringBuilder(row).reverse().toString()).toList();
    }

    /** Every 3 x 3 grid (nine cells of ingredient JSON, "." when empty) the pattern fills. */
    private static Set<String> placements(List<String> rows, JsonObject key) {
        Set<String> out = new HashSet<>();
        int height = rows.size();
        int width = rows.get(0).length();
        for (int dy = 0; dy + height <= 3; dy++) {
            for (int dx = 0; dx + width <= 3; dx++) {
                String[] cells = new String[9];
                Arrays.fill(cells, ".");
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        char c = rows.get(y).charAt(x);
                        cells[(dy + y) * 3 + dx + x] = c == ' ' ? "." : key.get(String.valueOf(c)).toString();
                    }
                }
                out.add(String.join("|", cells));
            }
        }
        return out;
    }

    private static void assertRecipe(String name, List<String> pattern, Map<String, String> ingredients) {
        JsonObject recipe = json(data("recipes/" + name + ".json"));
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), name);
        List<String> actualPattern = new ArrayList<>();
        recipe.getAsJsonArray("pattern").forEach(row -> actualPattern.add(row.getAsString()));
        assertEquals(pattern, actualPattern, name);
        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals(ingredients.keySet(), key.keySet(), name + ": ingredient symbols");
        ingredients.forEach((symbol, expected) -> assertEquals(expected, key.get(symbol).toString(), name + " " + symbol));
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("roundwood:" + name, result.get("item").getAsString(), name);
        assertFalse(result.has("count") && result.get("count").getAsInt() != 1, name + ": one per craft");
    }

    @Test
    void recipeAdvancementsUnlockTheirRecipe() throws IOException {
        for (Path file : allJson(data("advancements/recipes"))) {
            String name = file.getFileName().toString().replace(".json", "");
            JsonObject advancement = json(file);
            assertEquals("roundwood:" + name, advancement.getAsJsonObject("rewards").getAsJsonArray("recipes")
                    .get(0).getAsString(), name);
            assertTrue(advancement.toString().contains("\"recipe\":\"roundwood:" + name + "\""), name);
        }
    }

    @Test
    void lootDropsTheCanonicalItemOfTheBlocksFamily() {
        for (String block : BLOCKS) {
            JsonObject loot = json(data("loot_tables/blocks/" + block + ".json"));
            JsonArray pools = loot.getAsJsonArray("pools");
            assertEquals(1, pools.size(), block);
            JsonArray entries = pools.get(0).getAsJsonObject().getAsJsonArray("entries");
            assertEquals(1, entries.size(), block);
            assertEquals("roundwood:" + family(block), entries.get(0).getAsJsonObject().get("name").getAsString(), block);
        }
        for (String canonical : CANONICAL) { // canonical blocks drop themselves
            assertEquals("roundwood:" + canonical, json(data("loot_tables/blocks/" + canonical + ".json"))
                    .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                    .getAsJsonObject().get("name").getAsString());
        }
    }

    @Test
    void langNamesCanonicalAndAliasBlocksAndKeepsTheTooltips() {
        JsonObject lang = json(assets("lang/en_us.json"));
        assertEquals("Round Door", lang.get("block.roundwood.round_door").getAsString());
        assertEquals("Round Window", lang.get("block.roundwood.round_window").getAsString());
        assertEquals("Round Window Arch, Left", lang.get("block.roundwood.round_window_arch_left").getAsString());
        assertEquals("Round Window Arch, Right", lang.get("block.roundwood.round_window_arch_right").getAsString());
        assertEquals("Round Trim", lang.get("block.roundwood.round_trim").getAsString());
        for (String alias : ALIASES) {
            assertEquals(englishName(alias), lang.get("block.roundwood." + alias).getAsString(), alias);
        }
        assertEquals("Dark Oak Round Door", lang.get("block.roundwood.dark_oak_round_door").getAsString());
        assertEquals("Stone Brick Round Trim", lang.get("block.roundwood.stone_brick_round_trim").getAsString());
        assertTrue(lang.has("itemGroup.roundwood"));
        assertTrue(lang.keySet().stream().anyMatch(k -> k.startsWith("tooltip.roundwood.")), "tooltip keys");
        assertTrue(lang.keySet().stream().anyMatch(k -> k.startsWith("message.roundwood.")), "message keys");
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
