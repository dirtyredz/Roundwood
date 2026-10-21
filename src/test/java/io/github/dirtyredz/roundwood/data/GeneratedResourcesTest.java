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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates the checked-in generated tree without starting Minecraft. Since 0.7.0 there are twelve
 * canonical blocks (round_door, ten windows, round_trim) whose look lives on block entities, the
 * window_trim item (an item only: no blockstate, no loot table), and the 49 ids of 0.4.0 kept as
 * hidden aliases. The expected names are listed here independently of the mod's catalogue, so a
 * catalogue mistake shows up as a diff. Recipes are covered by {@link GeneratedRecipesTest}.
 */
class GeneratedResourcesTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path MAIN = Path.of("src/main/resources");
    /** The ten windows in creative order. */
    private static final List<String> WINDOWS = List.of("round_window", "round_window_cross", "round_window_arch_left",
            "round_window_arch_left_cross", "round_window_arch_right", "round_window_arch_right_cross",
            "round_window_bottom_left", "round_window_bottom_left_cross", "round_window_bottom_right",
            "round_window_bottom_right_cross");
    /** The two designs that are plain round windows; the other eight are oriented (arches and corners). */
    private static final List<String> ROUND_DESIGNS = List.of("round_window", "round_window_cross");
    private static final List<String> CANONICAL = Stream.of(List.of("round_door"), WINDOWS, List.of("round_trim"))
            .flatMap(List::stream).toList();
    /** window_trim is an item only. */
    private static final String WINDOW_TRIM = "window_trim";
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
        if (WINDOWS.contains(id)) {
            return id;
        }
        return id.endsWith("_round_window") ? "round_window" : "round_trim";
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
        assertEquals(12, CANONICAL.size());
        assertEquals(10, WINDOWS.size());
        assertEquals(61, BLOCKS.size());
        assertEquals(61, new HashSet<>(BLOCKS).size(), "identifiers must be unique");
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
    void windowTrimIsAnItemWithoutABlock() {
        assertTrue(Files.isRegularFile(assets("models/item/" + WINDOW_TRIM + ".json")), "item model");
        assertTrue(json(assets("lang/en_us.json")).has("item.roundwood." + WINDOW_TRIM), "item name");
        assertFalse(Files.exists(assets("blockstates/" + WINDOW_TRIM + ".json")), "no blockstate");
        assertFalse(Files.exists(data("loot_tables/blocks/" + WINDOW_TRIM + ".json")), "no loot table");
        assertFalse(Files.exists(assets("models/block/" + WINDOW_TRIM + ".json")), "no block model");
    }

    @Test
    void generatedSetsAreExactlyTheExpectedManifests() throws IOException {
        Set<String> block = new TreeSet<>(List.of("template_round_door_leaf_left", "template_round_door_leaf_right",
                "round_door_leaf_left", "round_door_leaf_right", "round_door", "round_trim"));
        Set<String> item = new TreeSet<>(List.of("template_round_door", "round_door", "template_round_trim",
                "template_round_trim_whole", "round_trim", "round_trim_whole", WINDOW_TRIM));
        block.addAll(windowBlockModels());
        item.addAll(WINDOWS);
        for (String part : PARTS) {
            block.add("template_round_trim_" + part);
            block.add("template_round_trim_whole_" + part);
            block.add("round_trim_" + part);
            block.add("round_trim_whole_" + part);
        }
        item.addAll(ALIASES);
        assertEquals(220, windowBlockModels().size());
        assertEquals(22 + 220, block.size());
        assertEquals(66, item.size());
        // Exact sets: no per-finish models remain; the 49 old ids are only item-model aliases.
        assertEquals(block, names(assets("models/block")));
        assertEquals(item, names(assets("models/item")));
        assertEquals(new TreeSet<>(BLOCKS), names(assets("blockstates")));
        assertEquals(new TreeSet<>(BLOCKS), names(data("loot_tables/blocks")));
        Set<String> crafted = new TreeSet<>(CANONICAL);
        crafted.add(WINDOW_TRIM);
        assertEquals(crafted, names(data("recipes")));
        assertEquals(crafted, names(data("advancements/recipes")));
        assertEquals(61, allJson(assets("blockstates")).size());
        assertEquals(242, allJson(assets("models/block")).size());
        assertEquals(66, allJson(assets("models/item")).size());
        assertEquals(13, allJson(data("recipes")).size());
        assertEquals(61, allJson(data("loot_tables/blocks")).size());
        assertEquals(13, allJson(data("advancements/recipes")).size());
    }

    /**
     * The window block models, named independently. Per design the stems are: round designs
     * ({@code round_window}, {@code round_window_cross}) the centred stem, {@code _back},
     * {@code _down_ceiling} and {@code _up_floor}; oriented designs the centred stem, {@code _back},
     * {@code _front} and front up/down at floor, level and ceiling. Every stem exists untrimmed and
     * with {@code _trimmed}; the centred stem additionally has {@code _arms_north|south|both} in both
     * forms. That is 14 models per round design and 24 per oriented design.
     */
    static Set<String> windowBlockModels() {
        Set<String> out = new TreeSet<>();
        for (String design : WINDOWS) {
            List<String> stems = new ArrayList<>(List.of(design, design + "_back"));
            if (ROUND_DESIGNS.contains(design)) {
                stems.add(design + "_down_ceiling");
                stems.add(design + "_up_floor");
            } else {
                stems.add(design + "_front");
                for (String front : List.of("up", "down")) {
                    for (String level : List.of("floor", "level", "ceiling")) {
                        stems.add(design + "_" + front + "_" + level);
                    }
                }
            }
            for (String stem : stems) {
                out.add(stem);
                out.add(stem + "_trimmed");
            }
            for (String arms : List.of("north", "south", "both")) {
                out.add(design + "_arms_" + arms);
                out.add(design + "_trimmed_arms_" + arms);
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
            Set<String> windows = new TreeSet<>();
            WINDOWS.forEach(w -> windows.add("roundwood:" + w));
            assertEquals(10, windows.size());
            assertEquals(windows, tagValues(tags.resolve(kind + "/round_windows.json")), kind);
        }
        assertEquals(Set.of("roundwood:round_trim"), tagValues(tags.resolve("blocks/round_trims.json")), "block trims");
        assertEquals(Set.of("roundwood:round_trim", "roundwood:window_trim"),
                tagValues(tags.resolve("items/round_trims.json")), "item trims");
    }

    @Test
    void lootDropsTheCanonicalItemOfTheBlocksFamily() {
        for (String block : BLOCKS) {
            JsonObject loot = json(data("loot_tables/blocks/" + block + ".json"));
            assertEquals("minecraft:block", loot.get("type").getAsString(), block);
            JsonArray pools = loot.getAsJsonArray("pools");
            boolean window = WINDOWS.contains(block) || block.endsWith("_round_window");
            assertEquals(window ? 2 : 1, pools.size(), block);
            assertEquals(JsonParser.parseString(ownItemPool("roundwood:" + family(block))), pools.get(0), block);
            if (window) {
                assertEquals(JsonParser.parseString(trimPool("roundwood:" + block)), pools.get(1), block + ": trim pool");
            }
        }
        for (String canonical : CANONICAL) { // canonical blocks drop themselves
            assertEquals("roundwood:" + canonical, json(data("loot_tables/blocks/" + canonical + ".json"))
                    .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                    .getAsJsonObject().get("name").getAsString());
        }
        assertEquals("roundwood:round_window", json(data("loot_tables/blocks/oak_round_window.json"))
                .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject().get("name").getAsString());
    }

    /** Pool 1: the block's own item (an alias drops its family's canonical item), once, unless blown up. */
    private static String ownItemPool(String item) {
        return "{\"bonus_rolls\":0.0,\"conditions\":[{\"condition\":\"minecraft:survives_explosion\"}],"
                + "\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"" + item + "\"}],\"rolls\":1.0}";
    }

    /** Pool 2: a trimmed window also drops a window_trim carrying the trim's look. */
    private static String trimPool(String block) {
        return "{\"bonus_rolls\":0.0,\"conditions\":[{\"block\":\"" + block + "\","
                + "\"condition\":\"minecraft:block_state_property\",\"properties\":{\"trimmed\":\"true\"}},"
                + "{\"condition\":\"minecraft:survives_explosion\"}],"
                + "\"entries\":[{\"type\":\"minecraft:item\",\"functions\":[{\"function\":\"minecraft:copy_nbt\","
                + "\"ops\":[{\"op\":\"replace\",\"source\":\"WindowTrim.RoundwoodAppearance\","
                + "\"target\":\"RoundwoodAppearance\"}],\"source\":\"block_entity\"}],"
                + "\"name\":\"roundwood:window_trim\"}],\"rolls\":1.0}";
    }

    @Test
    void langNamesCanonicalAndAliasBlocksAndKeepsTheTooltips() {
        JsonObject lang = json(assets("lang/en_us.json"));
        assertEquals("Round Door", lang.get("block.roundwood.round_door").getAsString());
        List<String> windowNames = List.of("Round Window", "Round Window with Cross Beams", "Round Window Arch, Left",
                "Round Window Arch with Cross Beams, Left", "Round Window Arch, Right",
                "Round Window Arch with Cross Beams, Right", "Round Window Corner, Bottom Left",
                "Round Window Corner with Cross Beams, Bottom Left", "Round Window Corner, Bottom Right",
                "Round Window Corner with Cross Beams, Bottom Right");
        for (int i = 0; i < WINDOWS.size(); i++) {
            assertEquals(windowNames.get(i), lang.get("block.roundwood." + WINDOWS.get(i)).getAsString(), WINDOWS.get(i));
        }
        assertEquals("Window Trim", lang.get("item.roundwood.window_trim").getAsString());
        assertTrue(lang.has("tooltip.roundwood.window_trim"));
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
