package io.github.dirtyredz.roundwood.data.textures;

import io.github.dirtyredz.roundwood.data.GeneratedFiles;
import io.github.dirtyredz.roundwood.data.ResolvedModels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedArchwayTexturesTest {
    @Test
    void oakArchwaysUsePlanksOnEveryFaceAndParticle() {
        for (String design : GeneratedFiles.ARCHWAYS) {
            var item = ResolvedModels.resolve("roundwood:item/" + design);
            for (String texture : new String[] {"#face", "#edge", "#particle"}) {
                assertEquals("minecraft:block/oak_planks", item.texture(texture), design + " " + texture);
            }
            assertEquals("minecraft:block/oak_planks",
                    ResolvedModels.resolve("roundwood:block/" + design).texture("#particle"));
        }
    }
}
