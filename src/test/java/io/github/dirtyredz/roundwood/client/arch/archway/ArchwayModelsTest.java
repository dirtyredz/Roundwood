package io.github.dirtyredz.roundwood.client.arch.archway;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.client.material.PaintStyles;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchwayModelsTest {
    static { MinecraftBootstrap.ensure(); }

    @Test
    void paintUsesTheSameTiledBoardsOnFrontAndInnerFaces() {
        for (DyeColor dye : DyeColor.values()) {
            var texture = PaintStyles.windowPanel(dye);
            assertEquals(new TrimLooks.Look(false, texture, texture),
                    ArchwayModels.look(Appearance.DEFAULT.painted(dye)));
        }
    }
}
