package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated window models: square wooden face, visible round glazing, glass arms. */
class GeneratedWindowModelsTest {
    private static final String GLASS = "roundwood:block/window_glass";

    @Test
    void windowItemsAreSquareWoodWithRoundGlass() {
        for (String finish : ModelExpectations.FINISHES) {
            Resolved item = resolve("roundwood:item/" + finish + "_round_window");
            String wood = ModelExpectations.windowWood(finish);
            assertEquals(wood, item.texture("#frame"), finish);
            assertEquals(GLASS, item.texture("#glass"), finish);
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    boolean opening = inDisc(16, 12, u, v);
                    assertEquals(!opening, covered(item, u, v, "south", wood), finish + " wood at " + u + "," + v);
                    assertEquals(opening, covered(item, u, v, "south", GLASS), finish + " glass at " + u + "," + v);
                }
            }
            ModelExpectations.assertDisplay(item, finish + " window", new double[] {8, 8, 8});
            for (JsonObject element : elements(item)) {
                double[] from = vec(element, "from");
                double[] to = vec(element, "to");
                assertTrue(from[2] >= 7 && to[2] <= 9, finish + ": window element outside the pane depth z 7..9");
                JsonObject south = element.getAsJsonObject("faces").getAsJsonObject("south");
                if (south == null) {
                    continue;
                }
                String texture = item.texture(south.get("texture").getAsString());
                if (wood.equals(texture)) {
                    assertEquals(9, to[2], 1e-9, finish + ": wood front face is not at z 9");
                } else if (GLASS.equals(texture)) {
                    assertEquals(7.5, from[2], 1e-9, finish + ": glass back");
                    assertEquals(8.5, to[2], 1e-9, finish + ": glass front");
                }
            }
        }
    }

    /**
     * Owner report (0.3.1): windows showed no glass. Vanilla {@code glass.png} is transparent inside
     * its one-pixel border and the glazing sampled only that interior. Every glazing face must use
     * Roundwood's own glass, whose sampled texels are all visible, in a translucent model, and the
     * glazing must fill the opening on both faces.
     */
    @Test
    void windowGlazingIsVisibleTranslucentGlass() throws IOException {
        BufferedImage glass = ImageIO.read(TEXTURES.resolve("window_glass.png").toFile());
        assertNotNull(glass, GLASS + " is missing");
        int scale = glass.getWidth() / 16;
        for (String id : List.of("roundwood:block/template_round_window", "roundwood:block/round_window_arm")) {
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
                            assertTrue(alpha >= 40, id + ": invisible glass texel " + u + "," + v + " on " + face.getKey());
                        }
                    }
                    glassFaces++;
                }
            }
            assertTrue(glassFaces > 0, id + ": no glazing faces at all");
        }
        for (String finish : ModelExpectations.FINISHES) {
            Resolved core = resolve("roundwood:block/" + finish + "_round_window");
            assertEquals(GLASS, core.texture("#glass"), finish + ": glazing texture");
            assertEquals("minecraft:translucent", renderType("roundwood:block/" + finish + "_round_window"), finish);
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    if (inDisc(16, 12, u, v)) {
                        assertTrue(covered(core, u, v, "south", GLASS), finish + ": no front glass at " + u + "," + v);
                        assertTrue(covered(core, u, v, "north", GLASS), finish + ": no back glass at " + u + "," + v);
                    }
                }
            }
        }
    }

    @Test
    void windowArmModelIsGlassFromTheEdgeToThePanel() {
        Resolved arm = resolve("roundwood:block/round_window_arm");
        assertFalse(arm.elements().isEmpty());
        boolean reachesEdge = false;
        for (JsonObject element : elements(arm)) {
            double[] from = vec(element, "from");
            double[] to = vec(element, "to");
            assertTrue(from[0] >= 7 && to[0] <= 9, "arm outside x 7..9");
            assertTrue(from[2] >= 0 && to[2] <= 7.5, "arm outside z 0..7.5");
            reachesEdge |= from[2] == 0;
            JsonObject faces = element.getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                assertEquals(GLASS, arm.texture(face.getValue().getAsJsonObject().get("texture").getAsString()), face.getKey());
            }
            if (to[2] == 7 || to[2] == 7.5) {
                assertFalse(faces.has("south"), "buried south face on the element ending at z " + to[2]);
            }
        }
        assertTrue(reachesEdge, "no arm element starts at z 0");
    }
}
