package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

import static io.github.dirtyredz.roundwood.data.ResolvedModels.Resolved;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.TEXTURES;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.covered;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.elements;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.inDisc;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.renderType;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.resolve;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.uv;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.vec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated round window models: square wooden face, visible round glazing, glass arms, no seams. */
class GeneratedWindowModelsTest {
    private static final String GLASS = ModelExpectations.GLASS;
    /** Both glazing textures: a check that must also catch a model left on the 0.6 glass. */
    private static final Set<String> GLASS_TEXTURES = Set.of("roundwood:block/window_glass", ModelExpectations.GLASS);

    /** The oak item model is the placeholder: only its oak_planks faces are swapped at runtime, glass never. */
    @Test
    void windowItemIsSquareOakWithRoundGlass() {
        Resolved item = resolve("roundwood:item/round_window");
        String wood = ModelExpectations.OAK_PLANKS;
        assertEquals(wood, item.texture("#frame"));
        assertEquals(GLASS, item.texture("#glass"));
        assertEquals(wood, item.texture("#particle"));
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                boolean opening = inDisc(16, 12, u, v);
                assertEquals(!opening, covered(item, u, v, "south", wood), "wood at " + u + "," + v);
                assertEquals(opening, covered(item, u, v, "south", GLASS), "glass at " + u + "," + v);
            }
        }
        ModelExpectations.assertDisplay(item, "window", new double[] {8, 8, 8});
        for (JsonObject element : elements(item)) {
            double[] from = vec(element, "from");
            double[] to = vec(element, "to");
            assertTrue(from[2] >= 7 && to[2] <= 9, "window element outside the pane depth z 7..9");
            JsonObject south = element.getAsJsonObject("faces").getAsJsonObject("south");
            if (south == null) {
                continue;
            }
            String texture = item.texture(south.get("texture").getAsString());
            if (wood.equals(texture)) {
                assertEquals(9, to[2], 1e-9, "wood front face is not at z 9");
            } else if (GLASS.equals(texture)) {
                assertEquals(8.5, to[2], 1e-9, "glass front");
            }
        }
    }

    @Test
    void aliasWindowItemsJustParentTheCanonicalWindowItem() {
        for (String alias : ModelExpectations.aliasWindows()) {
            JsonObject model = ResolvedModels.read("roundwood:item/" + alias);
            assertEquals(Set.of("parent"), model.keySet(), alias);
            assertEquals("roundwood:item/round_window", model.get("parent").getAsString(), alias);
        }
    }

    /**
     * Owner reports: windows showed no glass (0.3.1, vanilla glass.png is transparent inside its
     * border), then glass too opaque (0.6.1, screenshot 02). Every glazing face uses Roundwood's clear
     * glass, every texel it samples is visible but faint (alpha 24..36, the diagonal accents 176), in
     * a translucent model, and the glazing fills the opening on both faces.
     */
    @Test
    void windowGlazingIsVisibleClearTranslucentGlass() throws IOException {
        BufferedImage glass = ImageIO.read(TEXTURES.resolve("window_glass_clear.png").toFile());
        assertNotNull(glass, GLASS + " is missing");
        int scale = glass.getWidth() / 16;
        for (String id : List.of("roundwood:block/round_window", "roundwood:block/round_window_arms_both",
                "roundwood:block/round_window_arch_left_cross_trimmed_arms_north")) {
            assertEquals("minecraft:translucent", renderType(id), id + ": glazing needs the translucent render type");
            Resolved model = resolve(id);
            int glassFaces = 0;
            for (JsonObject element : elements(model)) {
                for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
                    if (!"#glass".equals(face.getValue().getAsJsonObject().get("texture").getAsString())) {
                        continue;
                    }
                    double[] uv = uv(element, face.getKey());
                    int u0 = (int) Math.floor(Math.min(uv[0], uv[2]) * scale);
                    int u1 = (int) Math.ceil(Math.max(uv[0], uv[2]) * scale);
                    int v0 = (int) Math.floor(Math.min(uv[1], uv[3]) * scale);
                    int v1 = (int) Math.ceil(Math.max(uv[1], uv[3]) * scale);
                    for (int u = u0; u < u1; u++) {
                        for (int v = v0; v < v1; v++) {
                            int alpha = glass.getRGB(u, v) >>> 24;
                            assertTrue(alpha == 176 || (alpha >= 24 && alpha <= 36),
                                    id + ": glass texel " + u + "," + v + " alpha " + alpha + " on " + face.getKey());
                        }
                    }
                    glassFaces++;
                }
            }
            assertTrue(glassFaces > 0, id + ": no glazing faces at all");
        }
        Resolved core = resolve("roundwood:block/round_window");
        assertEquals(GLASS, core.texture("#glass"), "glazing texture");
        assertEquals(ModelExpectations.OAK_PLANKS, core.texture("#frame"), "oak frame placeholder");
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                if (inDisc(16, 12, u, v)) {
                    assertTrue(covered(core, u, v, "south", GLASS), "no front glass at " + u + "," + v);
                    assertTrue(covered(core, u, v, "north", GLASS), "no back glass at " + u + "," + v);
                }
            }
        }
    }

    /**
     * Owner report (0.6.1, screenshots 01-03): horizontal lines across the glazing. The opening was
     * rasterised into stacked glass boxes and every face of every box was drawn, so translucent sheets
     * sat inside the glass where two rows meet (heights 3, 4, 6, 10, 12, 13 in the round window). No
     * glass face of any window block model may lie against other glass of the same model.
     */
    @Test
    void noGlassFaceIsDrawnInsideGlass() throws IOException {
        List<String> failures = new ArrayList<>();
        for (String id : ResolvedModels.allModelIds()) {
            if (!id.startsWith("roundwood:block/") || !id.contains("window")) {
                continue;
            }
            Resolved model = resolve(id);
            List<double[][]> glass = new ArrayList<>();
            for (JsonObject element : elements(model)) {
                for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
                    if (isGlass(model, face.getValue().getAsJsonObject().get("texture").getAsString())) {
                        glass.add(new double[][] {vec(element, "from"), vec(element, "to")});
                        break;
                    }
                }
            }
            for (JsonObject element : elements(model)) {
                double[] from = vec(element, "from");
                double[] to = vec(element, "to");
                for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
                    if (!isGlass(model, face.getValue().getAsJsonObject().get("texture").getAsString())) {
                        continue;
                    }
                    double[] p = ResolvedModels.insideAnother(face.getKey(), from, to, glass);
                    if (p != null) {
                        failures.add(id + " " + face.getKey() + " face of " + Arrays.toString(from) + "->"
                                + Arrays.toString(to) + " lies inside glass at " + Arrays.toString(p));
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.size() + " glass sheets inside glass:\n" + String.join("\n",
                failures.subList(0, Math.min(40, failures.size()))));
    }

    private static boolean isGlass(Resolved model, String ref) {
        return ref.equals("#glass") || GLASS_TEXTURES.contains(String.valueOf(model.texture(ref)));
    }

    /** A centred window connected across its plane reaches the cell edge with vanilla-pane glass (x 7..9). */
    @Test
    void connectedCentredWindowsReachTheCellEdgeWithGlass() {
        Resolved both = resolve("roundwood:block/round_window_arms_both");
        boolean north = false;
        boolean south = false;
        for (JsonObject element : elements(both)) {
            double[] from = vec(element, "from");
            double[] to = vec(element, "to");
            JsonObject faces = element.getAsJsonObject("faces");
            if (faces.has("north") && from[2] == 0) {
                assertEquals("#glass", faces.getAsJsonObject("north").get("texture").getAsString());
                assertEquals("north", faces.getAsJsonObject("north").get("cullface").getAsString());
                assertTrue(from[0] >= 7 && to[0] <= 9, "arm outside x 7..9");
                north = true;
            }
            if (faces.has("south") && to[2] == 16) {
                assertTrue(from[0] >= 7 && to[0] <= 9, "arm outside x 7..9");
                south = true;
            }
        }
        assertTrue(north && south, "the arms do not reach both cell edges");
    }
}
