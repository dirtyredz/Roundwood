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

/** Checked-in resource reader and independent expected identifiers shared by resource guards. */
final class GeneratedFiles {

    static final Path GENERATED = Path.of("src/generated/resources");
    static final Path MAIN = Path.of("src/main/resources");
    /** The 26 windows in creative order: the ten of 0.9.0, then the sixteen of 0.10.0 (plain, then crossed). */
    static final List<String> WINDOWS = List.of("round_window", "round_window_cross", "round_window_arch_left",
            "round_window_arch_left_cross", "round_window_arch_right", "round_window_arch_right_cross",
            "round_window_bottom_left", "round_window_bottom_left_cross", "round_window_bottom_right",
            "round_window_bottom_right_cross",
            "round_window_arch_bottom_left", "round_window_arch_bottom_left_cross",
            "round_window_arch_bottom_right", "round_window_arch_bottom_right_cross",
            "round_window_top_left", "round_window_top_left_cross", "round_window_top_right",
            "round_window_top_right_cross", "round_window_edge_top", "round_window_edge_top_cross",
            "round_window_edge_left", "round_window_edge_left_cross", "round_window_edge_bottom",
            "round_window_edge_bottom_cross", "round_window_edge_right", "round_window_edge_right_cross", "frameless_window");
    /** The two designs that are plain round windows; the other 24 are oriented (arches, corners and edges). */
    static final List<String> ROUND_DESIGNS = List.of("round_window", "round_window_cross");
    /** The two archways (0.12.0): block entities with a front and back trim, like doors and windows. */
    static final List<String> ARCH_PIECES = List.of("arch_top_1x1", "arch_top_1x2", "arch_top_2x3", "keyhole_shoulder", "arch_top_1x1_thin", "arch_top_1x2_thin", "arch_top_2x3_thin", "keyhole_shoulder_thin");
    static final List<String> ARCHWAYS = List.of("archway", "keyhole_archway");
    static final List<String> PLAIN_WINDOWS = WINDOWS.stream().filter(id -> !id.endsWith("_cross")).toList();
    static final List<String> RETIRED = WINDOWS.stream().filter(id -> id.endsWith("_cross")).toList();
    static final List<String> CANONICAL = Stream.of(List.of("round_door", "arched_door", "double_arched_door", "tall_double_arched_door"), PLAIN_WINDOWS, ARCH_PIECES, List.of("round_trim"))
            .flatMap(List::stream).toList();
    /** window_trim is an item only. */
    static final String WINDOW_TRIM = "window_trim";
    /** arch_sill_trim is an item only (0.10.0). */
    static final String ARCH_SILL_TRIM = "arch_sill_trim";
    /** The four upper-arch windows whose loot also drops the arch sill per face. */
    static final List<String> UPPER_ARCHES = List.of("round_window_arch_left", "round_window_arch_left_cross",
            "round_window_arch_right", "round_window_arch_right_cross");
    static final List<String> ALIASES = ModelExpectations.aliases();
    static final List<String> BLOCKS = Stream.of(CANONICAL, ALIASES, RETIRED, ARCHWAYS).flatMap(List::stream).toList();
    static final List<String> PARTS = ModelExpectations.PARTS;
    static final Set<String> ALLOWED_NAMESPACES = Set.of("minecraft", "roundwood", "forge");
    static final Pattern NAMESPACED = Pattern.compile("\"#?([a-z0-9_.-]+):[a-z0-9_./-]+\"");

    static JsonObject json(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("cannot read " + path, e);
        }
    }

    static Path assets(String rel) {
        return GENERATED.resolve("assets/roundwood/" + rel);
    }

    static Path data(String rel) {
        return GENERATED.resolve("data/roundwood/" + rel);
    }

    /** The canonical block an id belongs to: itself, or the family of an alias. */
    static String family(String id) {
        if (id.equals("arched_door") || (id.equals("double_arched_door") || id.equals("tall_double_arched_door"))) {
            return id;
        }
        if (id.endsWith("_round_door") || id.equals("round_door")) {
            return "round_door";
        }
        if (WINDOWS.contains(id) || ARCHWAYS.contains(id) || ARCH_PIECES.contains(id)) {
            return id;
        }
        return id.endsWith("_round_window") ? "round_window" : "round_trim";
    }

    /** "dark_oak_round_door" -> "Dark Oak Round Door", written independently of the mod. */
    static String englishName(String id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            out.append(out.length() == 0 ? "" : " ").append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }

    static Set<String> windowBlockModels() {
        Set<String> out = new TreeSet<>();
        for (String design : PLAIN_WINDOWS) {
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
            out.addAll(stems);
        }
        return out;
    }

    static Set<String> names(Path dir) throws IOException {
        Set<String> out = new TreeSet<>();
        for (Path file : allJson(dir)) {
            out.add(file.getFileName().toString().replace(".json", ""));
        }
        return out;
    }

    static String ownItemPool(String item) {
        return "{\"bonus_rolls\":0.0,\"conditions\":[{\"condition\":\"minecraft:survives_explosion\"}],"
                + "\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"" + item + "\"}],\"rolls\":1.0}";
    }

    static String trimFacePool(String face, String item, String source) {
        return "{\"bonus_rolls\":0.0,\"conditions\":[{\"condition\":\"roundwood:trim_face\",\"face\":\"" + face + "\"},"
                + "{\"condition\":\"minecraft:survives_explosion\"}],"
                + "\"entries\":[{\"type\":\"minecraft:item\",\"functions\":[{\"function\":\"minecraft:copy_nbt\","
                + "\"ops\":[{\"op\":\"replace\",\"source\":\"" + source + ".RoundwoodAppearance\","
                + "\"target\":\"RoundwoodAppearance\"}],\"source\":\"block_entity\"}],"
                + "\"name\":\"" + item + "\"}],\"rolls\":1.0}";
    }

    static String beamPool() {
        return sillFacePool("front", "WindowCrossBeams.Front").replace("arch_sill_trim", "cross_beams").replace("\"sill\"", "\"cross_beams\"");
    }

    static String sillFacePool(String face, String source) {
        return "{\"bonus_rolls\":0.0,\"conditions\":[{\"condition\":\"roundwood:trim_face\",\"face\":\"" + face + "\","
                + "\"slot\":\"sill\"},{\"condition\":\"minecraft:survives_explosion\"}],"
                + "\"entries\":[{\"type\":\"minecraft:item\",\"functions\":[{\"function\":\"minecraft:copy_nbt\","
                + "\"ops\":[{\"op\":\"replace\",\"source\":\"" + source + ".RoundwoodAppearance\","
                + "\"target\":\"RoundwoodAppearance\"}],\"source\":\"block_entity\"}],"
                + "\"name\":\"roundwood:arch_sill_trim\"}],\"rolls\":1.0}";
    }

    static Set<String> tagValues(Path file) {
        Set<String> out = new TreeSet<>();
        json(file).getAsJsonArray("values").forEach(v -> out.add(v.getAsString()));
        return out;
    }

    static Set<Path> allJson(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return new TreeSet<>(walk.filter(p -> p.toString().endsWith(".json")).toList());
        }
    }

    static List<String> modelsIn(JsonObject blockstate) {
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

    static void assertModelExists(String model, Path from) {
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

    static void assertTextureExists(String texture, Path from) {
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
