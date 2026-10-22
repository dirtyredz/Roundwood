package io.github.dirtyredz.roundwood.data;

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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 32 Roundwood recipes (round door, arched door (0.11.0), 26 windows, round trim, arch sill, archway and
 * keyhole archway (0.12.0)) and their advancements,
 * written out independently of the mod: exact patterns, ingredients, results and groups, plus the
 * guarantee that no two recipes can fill the same crafting grid, mirrored or shifted.
 */
class GeneratedRecipesTest {
    private static final Path DATA = Path.of("src/generated/resources/data/roundwood");
    private static final String PLANKS = "{\"tag\":\"minecraft:planks\"}";
    private static final String PANE = "{\"item\":\"minecraft:glass_pane\"}";
    private static final String STICK = "{\"item\":\"minecraft:stick\"}";
    private static final String NUGGET = "{\"item\":\"minecraft:gold_nugget\"}";

    private static JsonObject json(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("cannot read " + path, e);
        }
    }

    private static Path recipe(String name) {
        return DATA.resolve("recipes/" + name + ".json");
    }

    private static Set<String> recipeNames() throws IOException {
        Set<String> out = new TreeSet<>();
        try (Stream<Path> files = Files.list(DATA.resolve("recipes"))) {
            files.forEach(f -> out.add(f.getFileName().toString().replace(".json", "")));
        }
        return out;
    }

    @Test
    void thereAreExactlyTheThirtyTwoRecipes() throws IOException {
        assertEquals(new TreeSet<>(List.of("round_door", "arched_door", "round_window", "round_window_arch_left",
                "round_window_arch_right", "round_window_bottom_left", "round_window_bottom_right",
                "round_window_cross", "round_window_arch_left_cross", "round_window_arch_right_cross",
                "round_window_bottom_left_cross", "round_window_bottom_right_cross", "round_trim",
                "round_window_arch_bottom_left", "round_window_arch_bottom_right", "round_window_top_left",
                "round_window_top_right", "round_window_edge_top", "round_window_edge_left",
                "round_window_edge_bottom", "round_window_edge_right", "round_window_arch_bottom_left_cross",
                "round_window_arch_bottom_right_cross", "round_window_top_left_cross",
                "round_window_top_right_cross", "round_window_edge_top_cross", "round_window_edge_left_cross",
                "round_window_edge_bottom_cross", "round_window_edge_right_cross", "arch_sill_trim", "archway", "keyhole_archway")),
                recipeNames());
        assertEquals(32, recipeNames().size());
    }

    @Test
    void theThirtyTwoRecipesAreExactlyAsDesigned() {
        assertRecipe("round_door", "roundwood:round_door", List.of("PPP", "PNP", "PPP"), Map.of("P", PLANKS, "N", NUGGET));
        assertRecipe("arched_door", "roundwood:arched_door", List.of(" P ", "PNP", "PPP"), Map.of("P", PLANKS, "N", NUGGET));
        assertRecipe("round_window", "roundwood:round_window", List.of(" P ", "PGP", " P "), Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_arch_left", "roundwood:round_window_arch", List.of("PP", "PG", "P "),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_arch_right", "roundwood:round_window_arch", List.of("PPP", " G ", " P "),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_bottom_left", "roundwood:round_window_bottom", List.of("P ", "PG", "PP"),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_bottom_right", "roundwood:round_window_bottom", List.of(" P ", " G ", "PPP"),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_cross", "roundwood:round_window_cross", List.of("SPS", "PGP", "SPS"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_arch_left_cross", "roundwood:round_window_arch_cross", List.of("PPS", "PGS", "P  "),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_arch_right_cross", "roundwood:round_window_arch_cross", List.of("PPP", "SGS", " P "),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_bottom_left_cross", "roundwood:round_window_bottom_cross", List.of("P  ", "PGS", "PPS"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_bottom_right_cross", "roundwood:round_window_bottom_cross", List.of(" P ", "SGS", "PPP"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_trim", "roundwood:round_trim", List.of("P P", "   ", "P P"), Map.of("P", PLANKS));
        assertRecipe("round_window_arch_bottom_left", "roundwood:round_window_arch_bottom", List.of("PP", "PG", "PP"),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_arch_bottom_right", "roundwood:round_window_arch_bottom", List.of("PPP", " G ", "PPP"),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_top_left", "roundwood:round_window_top", List.of("PPP", "PG ", "P  "),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_top_right", "roundwood:round_window_top", List.of("PPP", " G ", "  P"),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_edge_top", "roundwood:round_window_edge", List.of("PPP", " G "),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_edge_left", "roundwood:round_window_edge", List.of("P ", "PG", "P "),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_edge_bottom", "roundwood:round_window_edge", List.of(" G ", "PPP"),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_edge_right", "roundwood:round_window_edge", List.of("GP", " P", " P"),
                Map.of("P", PLANKS, "G", PANE));
        assertRecipe("round_window_arch_bottom_left_cross", "roundwood:round_window_arch_bottom_cross", List.of("PP ", "PGS", "PPS"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_arch_bottom_right_cross", "roundwood:round_window_arch_bottom_cross", List.of("PPP", "SGS", "PPP"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_top_left_cross", "roundwood:round_window_top_cross", List.of("PPP", "PGS", "PS "),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_top_right_cross", "roundwood:round_window_top_cross", List.of("PPP", "SGS", "  P"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_edge_top_cross", "roundwood:round_window_edge_cross", List.of("PPP", "SGS"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_edge_left_cross", "roundwood:round_window_edge_cross", List.of("PS", "PG", "PS"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_edge_bottom_cross", "roundwood:round_window_edge_cross", List.of("SGS", "PPP"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("round_window_edge_right_cross", "roundwood:round_window_edge_cross", List.of("GP", "SP", "SP"),
                Map.of("P", PLANKS, "G", PANE, "S", STICK));
        assertRecipe("arch_sill_trim", "roundwood:arch_sill_trim", List.of("PPP", "S S"),
                Map.of("P", PLANKS, "S", STICK));
        assertRecipe("archway", "roundwood:archway", List.of("PPP", "P P", "P P"), Map.of("P", PLANKS));
        assertRecipe("keyhole_archway", "roundwood:archway", List.of("SPS", "P P", "P P"),
                Map.of("P", PLANKS, "S", STICK));
    }

    /**
     * No two Roundwood recipes can fill the same crafting grid: every placement of each pattern, and
     * of its mirror image (shaped recipes match mirrored), is compared on a 3 x 3 grid. Sticks and
     * nuggets are distinct ingredients, so a cross window never collides with a plain one.
     */
    @Test
    void noTwoRecipesMatchTheSameGridEvenMirrored() throws IOException {
        Map<String, Set<String>> grids = new TreeMap<>();
        for (String name : recipeNames()) {
            JsonObject recipe = json(recipe(name));
            List<String> rows = new ArrayList<>();
            recipe.getAsJsonArray("pattern").forEach(row -> rows.add(row.getAsString()));
            JsonObject key = recipe.getAsJsonObject("key");
            Set<String> placements = new HashSet<>();
            for (List<String> pattern : List.of(rows, mirror(rows))) {
                placements.addAll(placements(pattern, key));
            }
            grids.put(name, placements);
        }
        assertEquals(32, grids.size());
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

    @Test
    void recipeAdvancementsUnlockTheirRecipeInTheRightCategory() throws IOException {
        Set<String> seen = new TreeSet<>();
        for (String category : List.of("redstone", "decorations")) {
            Path dir = DATA.resolve("advancements/recipes/" + category);
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.toList()) {
                    String name = file.getFileName().toString().replace(".json", "");
                    assertTrue(seen.add(name), name + " appears in two categories");
                    assertEquals(category.equals("redstone"), name.equals("round_door") || name.equals("arched_door"), name + " in " + category);
                    JsonObject advancement = json(file);
                    assertEquals("roundwood:" + name, advancement.getAsJsonObject("rewards").getAsJsonArray("recipes")
                            .get(0).getAsString(), name);
                    assertTrue(advancement.toString().contains("\"recipe\":\"roundwood:" + name + "\""), name);
                }
            }
        }
        assertEquals(recipeNames(), seen);
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

    private static void assertRecipe(String name, String group, List<String> pattern, Map<String, String> ingredients) {
        JsonObject recipe = json(recipe(name));
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), name);
        assertEquals(group, recipe.get("group").getAsString(), name + ": group");
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
}
