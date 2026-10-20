package io.github.dirtyredz.roundwood.data;

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
        assertEquals(30, BLOCKS.size());
        assertEquals(30, new HashSet<>(BLOCKS).size(), "identifiers must be unique");
        JsonObject lang = json(assets("lang/en_us.json"));
        for (String block : BLOCKS) {
            assertTrue(Files.isRegularFile(assets("blockstates/" + block + ".json")), "blockstate " + block);
            assertTrue(Files.isRegularFile(assets("models/item/" + block + ".json")), "item model " + block);
            assertTrue(Files.isRegularFile(data("loot_tables/blocks/" + block + ".json")), "loot " + block);
            String recipe = PAINTED.stream().anyMatch(p -> block.startsWith(p + "_")) ? block + "_from_painting" : block;
            assertTrue(Files.isRegularFile(data("recipes/" + recipe + ".json")), "recipe " + recipe);
            assertTrue(lang.has("block.roundwood." + block), "name " + block);
        }
        assertTrue(lang.has("itemGroup.roundwood"));
    }

    @Test
    void generatedFileSetHasExactlyTheExpectedSize() throws IOException {
        // 8 door templates + 15 x 8 door models + window template, fill, arm + 15 window cores.
        assertEquals(8 + 15 * 8 + 3 + 15, allJson(assets("models/block")).size());
        // 30 block items + the door item template.
        assertEquals(31, allJson(assets("models/item")).size());
        assertEquals(30, allJson(assets("blockstates")).size());
        assertEquals(30, allJson(data("recipes")).size());
        assertEquals(30, allJson(data("loot_tables/blocks")).size());
        assertEquals(30, allJson(data("advancements/recipes")).size());
    }

    @Test
    void doorBlockstatesCoverEveryStateExceptPowered() {
        for (String finish : FINISHES) {
            JsonObject variants = json(assets("blockstates/" + finish + "_round_door.json")).getAsJsonObject("variants");
            // facing 4 x half 2 x hinge 2 x open 2
            assertEquals(32, variants.size(), finish);
            Set<String> keys = new TreeSet<>(variants.keySet());
            for (String key : keys) {
                assertTrue(!key.contains("powered"), "powered must not select a model: " + key);
            }
            assertTrue(keys.contains("facing=north,half=lower,hinge=left,open=false"), finish);
        }
    }

    @Test
    void windowBlockstatesCoverBothAxesAndEveryConnection() {
        for (String finish : FINISHES) {
            String text = json(assets("blockstates/" + finish + "_round_window.json")).toString();
            assertTrue(!text.contains("waterlogged"), "waterlogged must not select a model");
            int parts = json(assets("blockstates/" + finish + "_round_window.json")).getAsJsonArray("multipart").size();
            assertEquals(10, parts, "core + 4 connections per axis");
        }
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
    void doorTagsAndMineableTag() {
        Path tags = GENERATED.resolve("data/minecraft/tags");
        for (String tag : List.of("blocks/doors", "blocks/wooden_doors", "items/doors", "items/wooden_doors")) {
            String values = json(tags.resolve(tag + ".json")).getAsJsonArray("values").toString();
            for (String finish : FINISHES) {
                assertTrue(values.contains("roundwood:" + finish + "_round_door"), tag + " " + finish);
            }
            assertTrue(!values.contains("_round_window"), tag + " must not list windows");
        }
        String axe = json(tags.resolve("blocks/mineable/axe.json")).getAsJsonArray("values").toString();
        BLOCKS.forEach(block -> assertTrue(axe.contains("roundwood:" + block), "axe " + block));
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
