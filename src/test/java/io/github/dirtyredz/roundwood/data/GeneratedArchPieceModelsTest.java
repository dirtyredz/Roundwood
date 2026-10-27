package io.github.dirtyredz.roundwood.data;

import org.junit.jupiter.api.Test;
import java.util.List;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.*;
import static org.junit.jupiter.api.Assertions.*;

/** Independent authored mask and display bounds, read from the generated oak models. */
class GeneratedArchPieceModelsTest {
    @Test void oakIconsCoverExactlyTheirMasksInsideLegalBounds() {
        int[] widths = {16,32,48,16}, heights = {16,16,32,16}, radii = {8,16,24,0}, springs = {8,0,8,0};
        for (int product = 0; product < GeneratedFiles.ARCH_PIECES.size(); product++) {
            String id = GeneratedFiles.ARCH_PIECES.get(product);
            var model = resolve("roundwood:item/" + id);
            int width = widths[product % 4], height = heights[product % 4], radius = radii[product % 4], spring = springs[product % 4], offset = width == 48 ? -16 : 0;
            int[][] front = new int[width][height], back = new int[width][height];
            assertEquals("minecraft:block/oak_planks", model.texture("#donor"));
            for (var element : elements(model)) {
                var from = vec(element, "from"); var to = vec(element, "to");
                for (int axis = 0; axis < 3; axis++) assertTrue(from[axis] >= -16 && to[axis] <= 32);
                assertEquals(product < 4 ? 0 : 6.5, from[2]); assertEquals(product < 4 ? 16 : 9.5, to[2]);
                var faces = element.getAsJsonObject("faces");
                assertEquals(1, faces.size(), "only visible surfaces are emitted");
                for (int x = (int) from[0] - offset; x < to[0] - offset; x++)
                    for (int y = (int) from[1]; y < to[1]; y++) {
                        if (faces.has("south")) front[x][y]++;
                        if (faces.has("north")) back[x][y]++;
                    }
            }
            int[] shoulderWidths = {4,4,4,4,4,4,4,4,4,3,3,2,2,1,1,0};
            for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) {
                double dx = x + .5 - width / 2.0, dy = y + .5 - spring;
                boolean solid = radius == 0 ? x < shoulderWidths[y] : y >= spring && dx * dx + dy * dy > radius * radius;
                assertEquals(solid ? 1 : 0, front[x][y], id + " front " + x + "," + y);
                assertEquals(solid ? 1 : 0, back[x][y], id + " back " + x + "," + y);
            }
            assertEquals("front", model.guiLight());
            for (String context : List.of("fixed", "ground", "thirdperson_righthand", "firstperson_righthand"))
                assertTrue(model.display().has(context), context);
            // Forge omits identity transforms; vanilla supplies unit scale and zero translation.
            var gui = model.display().has("gui") ? model.display().getAsJsonObject("gui") : new com.google.gson.JsonObject();
            double scale = gui.has("scale") ? gui.getAsJsonArray("scale").get(0).getAsDouble() : 1;
            double[] translation = gui.has("translation") ? vec(gui, "translation") : new double[3];
            assertTrue(Math.max(width,height) * scale <= 16.0001, "icon fits the slot");
            assertEquals(0, translation[0] + scale * (width / 2.0 + offset - 8), .001);
            assertEquals(0, translation[1] + scale * (height / 2.0 - 8), .001);
        }
    }
}
