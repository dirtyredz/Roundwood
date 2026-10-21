package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static io.github.dirtyredz.roundwood.data.ResolvedModels.Resolved;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.elements;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.inDisc;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.resolve;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.uv;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.vec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated frame models: four cell-local quarters whose union is exactly the ring (outer 32,
 * inner 28, one pixel deep at the back of the cells) with nothing in the aperture, the right face
 * and edge textures per material, one continuous image across the quarters for painted and brick
 * frames, and a centred whole-ring inventory icon. Independent of the mod's own code.
 */
class GeneratedTrimModelsTest {
    private static final List<String> PARTS = List.of("lower_left", "upper_left", "lower_right", "upper_right");

    private static boolean ring(int u, int v) {
        return inDisc(32, 32, u, v) && !inDisc(32, 28, u, v);
    }

    private static int column(String part) {
        return part.endsWith("right") ? 1 : 0;
    }

    private static int row(String part) {
        return part.startsWith("upper") ? 1 : 0;
    }

    private static boolean whole(String material) {
        return !ModelExpectations.NATURAL.contains(material);
    }

    private static String face(String material) {
        if (ModelExpectations.NATURAL.contains(material)) {
            return ModelExpectations.stripped(material);
        }
        return ModelExpectations.PAINTED.contains(material)
                ? ModelExpectations.doorPanel(material) : "roundwood:block/trim_radial_" + material;
    }

    private static String edge(String material) {
        return ModelExpectations.MASONRY.contains(material) ? "roundwood:block/trim_edge_" + material : face(material);
    }

    @Test
    void quartersTogetherAreExactlyTheRingAndTheApertureIsEmpty() {
        for (String template : List.of("template_round_trim_", "template_round_trim_whole_")) {
            boolean[][] front = new boolean[32][32];
            boolean[][] back = new boolean[32][32];
            for (String part : PARTS) {
                Resolved quarter = resolve("roundwood:block/" + template + part);
                assertFalse(quarter.elements().isEmpty(), template + part + ": no geometry");
                for (JsonObject element : elements(quarter)) {
                    double[] from = vec(element, "from");
                    double[] to = vec(element, "to");
                    for (int axis = 0; axis < 2; axis++) {
                        assertTrue(from[axis] >= 0 && to[axis] <= 16, template + part + ": quarter leaves its cell");
                    }
                    assertEquals(0, from[2], 1e-9, template + part + ": ring back");
                    assertEquals(1, to[2], 1e-9, template + part + ": ring front");
                    JsonObject faces = element.getAsJsonObject("faces");
                    for (int x = (int) from[0]; x < to[0]; x++) {
                        for (int y = (int) from[1]; y < to[1]; y++) {
                            int u = x + 16 * column(part);
                            int v = y + 16 * row(part);
                            assertFalse(faces.has("south") && front[u][v], template + ": quarters overlap at " + u + "," + v);
                            front[u][v] |= faces.has("south");
                            back[u][v] |= faces.has("north");
                        }
                    }
                }
            }
            for (int u = 0; u < 32; u++) {
                for (int v = 0; v < 32; v++) {
                    assertEquals(ring(u, v), front[u][v], template + " front at " + u + "," + v);
                    assertEquals(ring(u, v), back[u][v], template + " back at " + u + "," + v);
                }
            }
        }
    }

    @Test
    void facesUseTheFaceTextureAndSidesTheEdgeTexture() {
        for (String part : PARTS) {
            Resolved quarter = resolve("roundwood:block/template_round_trim_" + part);
            for (JsonObject element : elements(quarter)) {
                double[] from = vec(element, "from");
                double[] to = vec(element, "to");
                for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
                    String texture = face.getValue().getAsJsonObject().get("texture").getAsString();
                    boolean frontOrBack = face.getKey().equals("north") || face.getKey().equals("south");
                    assertEquals(frontOrBack ? "#face" : "#edge", texture, part + " " + face.getKey());
                    // Faces where two quarters meet are buried and left out.
                    if (face.getKey().equals("east") && column(part) == 0) {
                        assertTrue(to[0] < 16, part + ": buried east face at the column join");
                    }
                    if (face.getKey().equals("up") && row(part) == 0) {
                        assertTrue(to[1] < 16, part + ": buried top face at the row join");
                    }
                    if (face.getKey().equals("west") && column(part) == 1) {
                        assertTrue(from[0] > 0, part + ": buried west face at the column join");
                    }
                    if (face.getKey().equals("down") && row(part) == 1) {
                        assertTrue(from[1] > 0, part + ": buried bottom face at the row join");
                    }
                }
            }
        }
    }

    /** Brick and painted frames read one image across all four quarters, front and back. */
    @Test
    void wholeImageFacesContinueAcrossTheQuarters() {
        for (String part : PARTS) {
            Resolved quarter = resolve("roundwood:block/template_round_trim_whole_" + part);
            for (JsonObject element : elements(quarter)) {
                double[] from = vec(element, "from");
                double[] to = vec(element, "to");
                double[] south = uv(element, "south");
                double x1 = from[0] + 16 * column(part);
                double x2 = to[0] + 16 * column(part);
                double y1 = from[1] + 16 * row(part);
                double y2 = to[1] + 16 * row(part);
                assertEquals(x1 / 2, south[0], 1e-6, part + " front u1");
                assertEquals(x2 / 2, south[2], 1e-6, part + " front u2");
                assertEquals((32 - y2) / 2, south[1], 1e-6, part + " front v1");
                assertEquals((32 - y1) / 2, south[3], 1e-6, part + " front v2");
                double[] north = uv(element, "north");
                assertEquals((32 - x2) / 2, north[0], 1e-6, part + " back u1");
                assertEquals((32 - x1) / 2, north[2], 1e-6, part + " back u2");
            }
        }
    }

    @Test
    void everyMaterialBindsItsTexturesToTheRightTemplate() {
        for (String material : ModelExpectations.TRIMS) {
            String family = whole(material) ? "template_round_trim_whole_" : "template_round_trim_";
            for (String part : PARTS) {
                String id = "roundwood:block/" + material + "_round_trim_" + part;
                JsonObject raw = ResolvedModels.read(id);
                assertEquals("roundwood:block/" + family + part, raw.get("parent").getAsString(), id);
                Resolved model = resolve(id);
                assertEquals(face(material), model.texture("#face"), id);
                assertEquals(edge(material), model.texture("#edge"), id);
            }
        }
    }

    @Test
    void blockstatesCoverEveryFacingAndPartWithTheTurnedQuarter() throws Exception {
        Map<String, Integer> turns = Map.of("north", 0, "east", 90, "south", 180, "west", 270);
        for (String material : ModelExpectations.TRIMS) {
            Path file = Path.of("src/generated/resources/assets/roundwood/blockstates/" + material + "_round_trim.json");
            JsonObject variants = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                    .getAsJsonObject("variants");
            Set<String> expected = new TreeSet<>();
            for (String facing : turns.keySet()) {
                for (String part : PARTS) {
                    String key = "facing=" + facing + ",part=" + part;
                    expected.add(key);
                    JsonObject variant = variants.getAsJsonObject(key);
                    assertEquals("roundwood:block/" + material + "_round_trim_" + part, variant.get("model").getAsString(), key);
                    assertEquals((int) turns.get(facing), variant.has("y") ? variant.get("y").getAsInt() : 0, material + " " + key);
                }
            }
            assertEquals(expected, new TreeSet<>(variants.keySet()), material);
        }
    }

    @Test
    void itemIconsAreTheWholeRingCentred() {
        for (String material : ModelExpectations.TRIMS) {
            Resolved item = resolve("roundwood:item/" + material + "_round_trim");
            assertEquals(face(material), item.texture("#face"), material);
            for (int u = 0; u < 32; u++) {
                for (int v = 0; v < 32; v++) {
                    assertEquals(ring(u, v), ResolvedModels.frontCovered(item, u, v, face(material)),
                            material + " icon at " + u + "," + v);
                }
            }
            ModelExpectations.assertDisplay(item, material + " frame", new double[] {16, 16, 0.5});
        }
    }
}
