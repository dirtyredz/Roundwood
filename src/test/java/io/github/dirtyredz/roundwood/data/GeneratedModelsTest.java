package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.dirtyredz.roundwood.data.ResolvedModels.Resolved;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.allModelIds;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.array;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.defaultUv;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.elements;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.frontCovered;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.inDisc;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.read;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.resolve;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.vec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolves the checked-in models the way the game does and checks what the owner sees: bounded
 * sprite UVs (tiled per block, or one image across a whole door or frame where intended), whole
 * closed doors with full-height board panels and prominent hardware, and deliberate item transforms.
 * Independent of the mod's own code. Windows: {@link GeneratedWindowModelsTest}; frames:
 * {@link GeneratedTrimModelsTest}.
 */
class GeneratedModelsTest {
    private static final String BRASS = ModelExpectations.BRASS;
    private static final String PANEL_OAK = ModelExpectations.DOOR_PANEL_OAK;

    /** Assembly-mapped UVs: one image across a 32-pixel face, front (south) and back (north). */
    static double[] assemblyUv(String face, double[] from, double[] to, double offsetX, double offsetY) {
        return assemblyUv(face, from, to, offsetX, offsetY, 32);
    }

    /**
     * As above for an assembly {@code width} pixels wide (0.11.0: the 16-wide arched door), still one
     * texel per pixel of a 32-pixel image, its back mirrored within its own width.
     */
    static double[] assemblyUv(String face, double[] from, double[] to, double offsetX, double offsetY, double width) {
        return assemblyUv(face, from, to, offsetX, offsetY, width, width == 48 ? 48 : 32, 1);
    }

    static double[] assemblyUv(String face, double[] from, double[] to, double offsetX, double offsetY, double width, double height, double yScale) {
        double x1 = from[0] + offsetX;
        double x2 = to[0] + offsetX;
        double scale = height / 16;
        double v1 = (height - ((to[1] + offsetY) / yScale)) / scale;
        double v2 = (height - ((from[1] + offsetY) / yScale)) / scale;
        return switch (face) {
            case "south" -> new double[] {x1 / scale, v1, x2 / scale, v2};
            case "north" -> new double[] {(width - x2) / scale, v1, (width - x1) / scale, v2};
            default -> null;
        };
    }

    /**
     * Where a model spreads one image across the assembly (door panels, painted and brick frames):
     * the texture variable that does it and the model's offset in the assembly, or null when every
     * face is tiled.
     */
    private static Object[] assemblyMapping(String id) {
        if (id.startsWith("roundwood:block/template_round_door_leaf_")) {
            return new Object[] {"#panel", 0.0, 0.0, 32.0};
        }
        if (id.startsWith("roundwood:block/template_arched_door_leaf_")) {
            return new Object[] {"#panel", 0.0, 0.0, 16.0};
        }
        if (id.startsWith("roundwood:block/template_double_arched_door_leaf_") || id.equals("roundwood:item/template_double_arched_door")) {
            return new Object[] {"#panel", 16.0, 16.0, 48.0};
        }
        if (id.contains("template_tall_double_arched_door")) {
            return new Object[] {"#panel", 16.0, 16.0, 48.0};
        }
        String quarter = "roundwood:block/template_round_trim_whole_";
        if (id.startsWith(quarter)) {
            String part = id.substring(quarter.length());
            return new Object[] {"#face", part.endsWith("right") ? 16.0 : 0.0, part.startsWith("upper") ? 16.0 : 0.0, 32.0};
        }
        return null;
    }

    @Test
    void everyFaceHasExplicitUvsInsideOneSpriteInTheIntendedMapping() throws IOException {
        int tiled = 0;
        int assembly = 0;
        for (String id : allModelIds()) {
            JsonObject model = read(id);
            if (!model.has("elements")) {
                continue;
            }
            Object[] mapping = assemblyMapping(id);
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
                    JsonArray uv = f.has("uv") ? f.getAsJsonArray("uv") : array(defaultUv(face.getKey(), from, to));
                    double[] expected = defaultUv(face.getKey(), localFrom, localTo);
                    double[] whole = mapping != null && mapping[0].equals(f.get("texture").getAsString())
                            ? (id.contains("tall_double_arched_door")
                                ? assemblyUv(face.getKey(), from, to, 16, 16, 48, 64, .75)
                                : assemblyUv(face.getKey(), from, to, (Double) mapping[1], (Double) mapping[2], (Double) mapping[3])) : null;
                    if (whole != null) {
                        expected = whole;
                        assembly++;
                    } else {
                        tiled++;
                    }
                    for (int i = 0; i < 4; i++) {
                        double value = uv.get(i).getAsDouble();
                        assertTrue(Double.isFinite(value) && value >= 0 && value <= 16, id + ": uv out of the sprite " + uv);
                        assertEquals(expected[i], value, 1e-4, id + ": uv " + face.getKey() + " " + uv);
                    }
                }
            }
        }
        assertTrue(tiled > 1000, "suspiciously few tiled faces: " + tiled);
        assertTrue(assembly > 200, "suspiciously few whole-image faces: " + assembly);
    }

    /** The oak item model is the placeholder the client retextures: oak boards, brass hardware, whole closed leaf. */
    @Test
    void doorItemIsTheWholeClosedLeafWithOakPlaceholders() {
        Resolved item = resolve("roundwood:item/round_door");
        assertEquals(PANEL_OAK, item.texture("#panel"));
        assertNull(item.texture("#trim"), "the door has no built-in trim since 0.4.0");
        assertEquals(BRASS, item.texture("#brass"));
        assertEquals(PANEL_OAK, item.texture("#particle"));
        for (int u = 0; u < 32; u++) {
            for (int v = 0; v < 32; v++) {
                boolean disc = inDisc(32, 32, u, v);
                boolean covered = frontCovered(item, u, v, PANEL_OAK) || frontCovered(item, u, v, BRASS);
                assertEquals(disc, covered, "door face at " + u + "," + v);
            }
        }
        ModelExpectations.assertDisplay(item, "door", new double[] {16, 16, 14.5});
    }

    /**
     * 0.11.0: the arched door's item is its whole closed leaf: 16 x 32, straight sides to y 24 then a
     * radius-eight crown about (8, 24), written out here independently; oak placeholders as the round door's.
     */
    @Test
    void archedDoorItemIsTheWholeClosedArchedLeaf() {
        Resolved item = resolve("roundwood:item/arched_door");
        assertEquals(PANEL_OAK, item.texture("#panel"));
        assertEquals(BRASS, item.texture("#brass"));
        for (int u = 0; u < 20; u++) {
            for (int v = 0; v < 32; v++) {
                double dx = u + 0.5 - 8;
                double dy = v + 0.5 - 24;
                boolean leaf = u < 16 && (v < 24 || dx * dx + dy * dy <= 64);
                boolean covered = frontCovered(item, u, v, PANEL_OAK) || frontCovered(item, u, v, BRASS);
                assertEquals(leaf, covered, "arched door face at " + u + "," + v);
            }
        }
        ModelExpectations.assertDisplay(item, "door", new double[] {8, 16, 14.5});
    }

    @Test
    void aliasDoorItemsJustParentTheCanonicalDoorItem() {
        for (String alias : ModelExpectations.aliasDoors()) {
            JsonObject model = read("roundwood:item/" + alias);
            assertEquals(Set.of("parent"), model.keySet(), alias);
            assertEquals("roundwood:item/round_door", model.get("parent").getAsString(), alias);
        }
    }

    /** The panel fills the whole disc at z 13..16 with no ring: every non-brass element is panel. */
    @Test
    void doorLeavesAreOnePanelOverTheWholeDisc() {
        for (String hinge : List.of("left", "right")) {
            Resolved leaf = resolve("roundwood:block/round_door_leaf_" + hinge);
            for (JsonObject element : elements(leaf)) {
                boolean brass = usesBrass(leaf, element);
                for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
                    String texture = face.getValue().getAsJsonObject().get("texture").getAsString();
                    assertTrue(brass ? texture.equals("#brass") : texture.equals("#panel"), hinge + ": unexpected " + texture);
                }
                if (!brass) {
                    assertEquals(13, vec(element, "from")[2], 1e-9, hinge + ": panel back");
                    assertEquals(16, vec(element, "to")[2], 1e-9, hinge + ": panel front");
                }
            }
            for (int u = 0; u < 32; u++) {
                for (int v = 0; v < 32; v++) {
                    assertEquals(inDisc(32, 32, u, v), frontCovered(leaf, u, v, "#panel"), hinge + " panel at " + u + "," + v);
                }
            }
        }
    }

    @Test
    void prominentCentredKnobsAndProudStrapsOnBothFacesForBothHinges() {
        for (String hinge : List.of("left", "right")) {
            Resolved leaf = resolve("roundwood:block/round_door_leaf_" + hinge);
            boolean[][] front = new boolean[2][2];
            boolean[][] back = new boolean[2][2];
            int straps = 0;
            for (JsonObject element : elements(leaf)) {
                if (!usesBrass(leaf, element)) {
                    continue;
                }
                double[] from = vec(element, "from");
                double[] to = vec(element, "to");
                if (to[2] - from[2] == .5) {
                    double centre = (from[0] + to[0]) / 2;
                    assertTrue(hinge.equals("left") ? centre < 16 : centre > 16, hinge + ": strap centre " + centre);
                    assertTrue((from[2] == 16 && to[2] == 16.5) || (from[2] == 12.5 && to[2] == 13),
                            hinge + ": strap not half a pixel proud: z " + from[2] + ".." + to[2]);
                    straps++;
                    continue;
                }
                // A knob piece: one quarter of the 2x2 knob at x 15..17, y 15..17, two pixels proud.
                assertTrue(from[0] >= 15 && to[0] <= 17 && from[1] >= 15 && to[1] <= 17, hinge + ": knob piece off centre");
                boolean isFront = from[2] >= 16 && to[2] == 18;
                boolean isBack = from[2] == 11 && to[2] <= 13;
                assertTrue(isFront || isBack, hinge + ": knob piece not 2 pixels proud: z " + from[2] + ".." + to[2]);
                for (int x = (int) Math.round(from[0]); x < Math.round(to[0]); x++) {
                    for (int y = (int) Math.round(from[1]); y < Math.round(to[1]); y++) {
                        (isFront ? front : back)[x - 15][y - 15] = true;
                    }
                }
            }
            assertTrue(straps >= 4, hinge + ": expected two straps on each face, got " + straps + " pieces");
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    assertTrue(front[x][y], hinge + ": front knob pixel " + (15 + x) + "," + (15 + y));
                    assertTrue(back[x][y], hinge + ": back knob pixel " + (15 + x) + "," + (15 + y));
                }
            }
        }
    }

    /** The door panel's front reads the image once, left to right and top to bottom, across the cells. */
    @Test
    void doorPanelImageRunsUnbrokenAcrossTheMidlines() {
        Resolved leaf = resolve("roundwood:block/round_door_leaf_left");
        int checked = 0;
        for (JsonObject element : elements(leaf)) {
            JsonObject south = element.getAsJsonObject("faces").getAsJsonObject("south");
            if (south == null || !"#panel".equals(south.get("texture").getAsString())) {
                continue;
            }
            double[] from = vec(element, "from");
            double[] to = vec(element, "to");
            double[] uv = ResolvedModels.uv(element, "south");
            // One texel per pixel of a 32 x 32 image: u = x / 2, v = (32 - y) / 2 in sprite units.
            assertEquals(from[0] / 2, uv[0], 1e-6, "panel u at x " + from[0]);
            assertEquals(to[0] / 2, uv[2], 1e-6, "panel u at x " + to[0]);
            assertEquals((32 - to[1]) / 2, uv[1], 1e-6, "panel v at y " + to[1]);
            checked++;
        }
        assertTrue(checked > 10, "too few panel faces: " + checked);
    }

    private static boolean usesBrass(Resolved model, JsonObject element) {
        for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
            // Templates leave the variable unbound; finish models bind it to roundwood:block/brass.
            String texture = face.getValue().getAsJsonObject().get("texture").getAsString();
            if ("#brass".equals(texture) || BRASS.equals(model.texture(texture))) {
                return true;
            }
        }
        return false;
    }

    @Test
    void doorWorldModelIsEmptyAndLeafModelsExistForBothHinges() {
        Resolved world = resolve("roundwood:block/round_door");
        assertEquals(0, world.elements().size(), "the world model must not draw the leaf twice");
        assertEquals(PANEL_OAK, world.texture("#particle"), "break particle");
        for (String hinge : List.of("left", "right")) {
            JsonObject raw = read("roundwood:block/round_door_leaf_" + hinge);
            assertEquals("roundwood:block/template_round_door_leaf_" + hinge, raw.get("parent").getAsString(), hinge);
            Resolved leaf = resolve("roundwood:block/round_door_leaf_" + hinge);
            assertFalse(leaf.elements().isEmpty(), hinge + " leaf has no geometry");
            assertEquals(PANEL_OAK, leaf.texture("#panel"), hinge);
            assertEquals(BRASS, leaf.texture("#brass"), hinge);
            assertNull(leaf.texture("#trim"), hinge + ": no trim binding since 0.4.0");
        }
    }

    /** The templates leave the variables open; only the oak models bind them, so retexturing has one source. */
    @Test
    void templatesBindNothingAndOnlyBrassIsFixed() {
        for (String hinge : List.of("left", "right")) {
            Resolved template = resolve("roundwood:block/template_round_door_leaf_" + hinge);
            assertNull(template.texture("#panel"), hinge + ": template must not pick a panel");
            assertNull(template.texture("#brass"), hinge + ": brass is bound by the leaf model");
        }
    }
}
