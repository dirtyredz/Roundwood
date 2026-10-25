package io.github.dirtyredz.roundwood.client.archway;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.client.material.PaintStyles;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchwayModelsTest {
    static { MinecraftBootstrap.ensure(); }

    @Test
    void allWoodsAndMasonryKeepTheResolvedDonorSpriteOnEveryFace() {
        for (String wood : new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "bamboo", "crimson", "warped"}) {
            checkDonor(Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", wood + "_planks")));
        }
        checkDonor(Appearance.wood(ResourceLocation.fromNamespaceAndPath("modded", "custom_planks")));
        for (var material : MasonryMaterials.ALL) checkDonor(Appearance.masonry(material.block()));
    }

    private static void checkDonor(Appearance appearance) {
        // A resource pack may use a different namespace/path than the block id.
        var sprite = ResourceLocation.fromNamespaceAndPath("pack", "block/" + appearance.material().getPath());
        assertEquals(new TrimLooks.Look(false, sprite, sprite), ArchwayModels.look(appearance, requested -> {
            assertEquals(appearance, requested);
            return sprite;
        }));
    }

    @Test
    void paintUsesTheSameTiledBoardsOnFrontAndInnerFaces() {
        for (DyeColor dye : DyeColor.values()) {
            var texture = PaintStyles.windowPanel(dye);
            assertEquals(new TrimLooks.Look(false, texture, texture),
                    ArchwayModels.look(Appearance.DEFAULT.painted(dye)));
        }
    }
}
