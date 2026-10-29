package io.github.dirtyredz.roundwood.data.models;

import io.github.dirtyredz.roundwood.data.ModelExpectations;
import io.github.dirtyredz.roundwood.data.ResolvedModels;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final List<String> PARTS = ModelExpectations.PARTS;

    private static boolean ring(int u, int v) {
        return inDisc(32, 32, u, v) && !inDisc(32, 28, u, v);
    }

    private static int column(String part) {
        return part.endsWith("right") ? 1 : 0;
    }

    private static int row(String part) {
        return part.startsWith("upper") ? 1 : 0;
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

    /** The oak models are what the client retextures: tiled stripped oak log, or the oak door panel as one image. */
    @Test
    void canonicalQuarterModelsBindTheOakPlaceholdersToTheRightTemplate() {
        for (String part : PARTS) {
            assertQuarter("roundwood:block/round_trim_" + part, "roundwood:block/template_round_trim_" + part,
                    ModelExpectations.STRIPPED_OAK_LOG);
            assertQuarter("roundwood:block/round_trim_whole_" + part, "roundwood:block/template_round_trim_whole_" + part,
                    ModelExpectations.DOOR_PANEL_OAK);
        }
    }

    private static void assertQuarter(String id, String parent, String oak) {
        JsonObject raw = ResolvedModels.read(id);
        assertEquals(parent, raw.get("parent").getAsString(), id);
        Resolved model = resolve(id);
        assertEquals(oak, model.texture("#face"), id);
        assertEquals(oak, model.texture("#edge"), id);
        assertEquals(oak, model.texture("#particle"), id);
    }

    @Test
    void worldModelIsEmptyWithOnlyAParticleTexture() {
        Resolved world = resolve("roundwood:block/round_trim");
        assertEquals(0, world.elements().size(), "the ring is drawn by the block-entity renderer");
        assertEquals(ModelExpectations.STRIPPED_OAK_LOG, world.texture("#particle"));
        assertEquals(Set.of("particle"), world.textures().keySet());
    }

    @Test
    void canonicalAndEveryAliasBlockstateIsTheSingleEmptyVariant() throws Exception {
        List<String> ids = new ArrayList<>(ModelExpectations.aliasTrims());
        ids.add("round_trim");
        assertEquals(20, ids.size());
        for (String id : ids) {
            Path file = Path.of("src/generated/resources/assets/roundwood/blockstates/" + id + ".json");
            JsonObject state = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            assertFalse(state.has("multipart"), id);
            JsonObject variants = state.getAsJsonObject("variants");
            assertEquals(Set.of(""), variants.keySet(), id);
            assertEquals("roundwood:block/round_trim", variants.getAsJsonObject("").get("model").getAsString(), id);
        }
    }

    @Test
    void aliasItemModelsParentTheCanonicalTrimItem() {
        for (String alias : ModelExpectations.aliasTrims()) {
            JsonObject model = ResolvedModels.read("roundwood:item/" + alias);
            assertEquals(Set.of("parent"), model.keySet(), alias);
            assertEquals("roundwood:item/round_trim", model.get("parent").getAsString(), alias);
        }
    }

    /** Since 0.9.0 the item is the Universal Trim: a flat, face-on square frame with its own elements. */
    @Test
    void theItemIconIsAFlatSquareFrame() {
        String id = "roundwood:item/round_trim";
        assertFalse(ResolvedModels.read(id).has("parent"), id + ": no template parent");
        Resolved item = resolve(id);
        String oak = ModelExpectations.STRIPPED_OAK_LOG;
        assertEquals(oak, item.texture("#face"), id);
        assertEquals(oak, item.texture("#edge"), id);
        assertEquals(oak, item.texture("#particle"), id);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean band = u < 2 || u >= 14 || v < 2 || v >= 14;
                assertEquals(band, ResolvedModels.frontCovered(item, u, v, oak), id + " icon at " + u + "," + v);
            }
        }
        ModelExpectations.assertDisplay(item, id, new double[] {8, 8, 8});
    }

    @Test
    void theWindowTrimItemModelIsTheUniversalTrim() {
        JsonObject model = ResolvedModels.read("roundwood:item/window_trim");
        assertEquals(Set.of("parent"), model.keySet());
        assertEquals("roundwood:item/round_trim", model.get("parent").getAsString());
    }
}
