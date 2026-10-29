package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test helper: reads the checked-in generated models and resolves them the way the game does
 * (parent chain, texture variables, display, gui light). Independent of the mod's own code.
 */
public final class ResolvedModels {
    public static final Path MODELS = Path.of("src/generated/resources/assets/roundwood/models");
    public static final Path TEXTURES = Path.of("src/main/resources/assets/roundwood/textures/block");

    private ResolvedModels() {
    }

    /** A model after inheritance: the nearest elements, merged textures and display, gui light. */
    public record Resolved(JsonArray elements, Map<String, String> textures, JsonObject display, String guiLight) {
        public String texture(String ref) {
            String value = ref;
            for (int i = 0; i < 10 && value != null && value.startsWith("#"); i++) {
                value = textures.get(value.substring(1));
            }
            return value;
        }
    }

    public static JsonObject read(String id) {
        String[] parts = id.split(":", 2);
        assertEquals("roundwood", parts[0], "unexpected namespace in " + id);
        Path file = MODELS.resolve(parts[1] + ".json");
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("cannot read " + file, e);
        }
    }

    public static Resolved resolve(String id) {
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

    /** The render type a model or its nearest ancestor declares, or null. */
    public static String renderType(String id) {
        String next = id;
        while (next != null) {
            JsonObject model = read(next);
            if (model.has("render_type")) {
                return model.get("render_type").getAsString();
            }
            next = model.has("parent") ? model.get("parent").getAsString() : null;
        }
        return null;
    }

    public static double[] vec(JsonObject element, String key) {
        JsonArray a = element.getAsJsonArray(key);
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    public static double[] doubles(JsonArray array) {
        double[] out = new double[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsDouble();
        }
        return out;
    }

    public static JsonArray array(double[] values) {
        JsonArray out = new JsonArray();
        for (double value : values) {
            out.add(value);
        }
        return out;
    }

    public static List<String> allModelIds() throws IOException {
        try (Stream<Path> walk = Files.walk(MODELS)) {
            return walk.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> "roundwood:" + MODELS.relativize(p).toString().replace('\\', '/').replace(".json", ""))
                    .sorted().toList();
        }
    }

    /** Vanilla's default UVs for an element face, from tile-local coordinates. */
    public static double[] defaultUv(String face, double[] f, double[] t) {
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

    /** The face's UVs as the game reads them: explicit, or vanilla's default from absolute coordinates. */
    public static double[] uv(JsonObject element, String face) {
        JsonObject f = element.getAsJsonObject("faces").getAsJsonObject(face);
        return f.has("uv") ? doubles(f.getAsJsonArray("uv")) : defaultUv(face, vec(element, "from"), vec(element, "to"));
    }

    public static boolean inDisc(int size, int diameter, int u, int v) {
        double du = u + 0.5 - size / 2.0;
        double dv = v + 0.5 - size / 2.0;
        return du * du + dv * dv <= diameter * diameter / 4.0;
    }

    /**
     * Pixel (u, v) of the x-y plane is covered by an element whose {@code side} face resolves to
     * {@code texture} (a resolved texture id, or a raw {@code #variable}).
     */
    public static boolean covered(Resolved model, int u, int v, String side, String texture) {
        for (JsonElement e : model.elements()) {
            JsonObject element = e.getAsJsonObject();
            double[] from = vec(element, "from");
            double[] to = vec(element, "to");
            if (from[0] <= u + 0.5 && u + 0.5 <= to[0] && from[1] <= v + 0.5 && v + 0.5 <= to[1]) {
                JsonObject face = element.getAsJsonObject("faces").getAsJsonObject(side);
                if (face == null) {
                    continue;
                }
                String ref = face.get("texture").getAsString();
                if (texture.equals(ref) || texture.equals(model.texture(ref))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Front ({@code south}) coverage, the face a north-authored model shows the viewer. */
    public static boolean frontCovered(Resolved model, int u, int v, String texture) {
        return covered(model, u, v, "south", texture);
    }

    /**
     * A half-pixel sample point just outside {@code face} of the box {@code from..to} that lies
     * strictly inside one of {@code boxes} ({from, to} pairs), or null when there is none.
     */
    public static double[] insideAnother(String face, double[] from, double[] to, List<double[][]> boxes) {
        int n = switch (face) {
            case "west", "east" -> 0;
            case "down", "up" -> 1;
            default -> 2;
        };
        boolean positive = face.equals("east") || face.equals("up") || face.equals("south");
        double plane = (positive ? to[n] : from[n]) + (positive ? 1e-3 : -1e-3);
        int a = (n + 1) % 3;
        int b = (n + 2) % 3;
        for (double x = from[a] + 0.25; x < to[a]; x += 0.5) {
            for (double y = from[b] + 0.25; y < to[b]; y += 0.5) {
                double[] p = new double[3];
                p[n] = plane;
                p[a] = x;
                p[b] = y;
                for (double[][] box : boxes) {
                    if (box[0][0] < p[0] && p[0] < box[1][0] && box[0][1] < p[1] && p[1] < box[1][1]
                            && box[0][2] < p[2] && p[2] < box[1][2]) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    public static List<JsonObject> elements(Resolved model) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : model.elements()) {
            out.add(e.getAsJsonObject());
        }
        return out;
    }
}
