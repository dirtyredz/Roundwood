package io.github.dirtyredz.roundwood.client.material;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared trim look resolver (0.9.0) as the Universal Trim icon uses it: every masonry gets the
 * square frame's own face (an existing generated texture) whole-mapped with the granular edge, every
 * paint the painted boards, the vanilla woods their stripped log tiled, and an unsupported material
 * oak. (Unfamiliar woods resolve through the client's sprite atlas and are an owner check.)
 */
class TrimLooksTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    private static final Path TEXTURES = Path.of("src/main/resources/assets/roundwood/textures");

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    @Test
    void uniformLookTilesOneTextureOnFacesAndEdges() {
        var texture = vanilla("block/oak_planks");
        var look = TrimLooks.Look.tiled(texture);
        assertFalse(look.whole());
        assertEquals(texture, look.face());
        assertEquals(texture, look.edge());
    }

    @Test
    void everyMasonryShowsTheSquareFrameFaceWhole() {
        for (MasonryMaterials.Material material : MasonryMaterials.ALL) {
            TrimLooks.Look look = TrimLooks.of(Appearance.masonry(material.block()), MasonryStyles::squareFace);
            assertTrue(look.whole(), material.artwork());
            assertEquals("roundwood:block/universal_trim_" + material.artwork(), look.face().toString());
            assertEquals(MasonryStyles.edge(material), look.edge());
            assertTrue(Files.isRegularFile(TEXTURES.resolve(look.face().getPath() + ".png")), look.face() + " is not generated");
            assertTrue(Files.isRegularFile(TEXTURES.resolve(look.edge().getPath() + ".png")), look.edge() + " is not generated");
        }
    }

    @Test
    void paintWoodAndUnsupportedMaterials() {
        for (DyeColor dye : DyeColor.values()) {
            TrimLooks.Look look = TrimLooks.of(Appearance.DEFAULT.painted(dye), MasonryStyles::squareFace);
            assertEquals(new TrimLooks.Look(true, PaintStyles.doorPanel(dye), PaintStyles.doorPanel(dye)), look, dye.getName());
        }
        assertEquals(TrimLooks.OAK, TrimLooks.of(Appearance.DEFAULT, MasonryStyles::squareFace));
        TrimLooks.Look spruce = TrimLooks.of(Appearance.wood(vanilla("spruce_planks")), MasonryStyles::squareFace);
        assertFalse(spruce.whole());
        assertEquals(vanilla("block/stripped_spruce_log"), spruce.face());
        assertEquals(TrimLooks.OAK, TrimLooks.of(Appearance.masonry(vanilla("stone")), MasonryStyles::squareFace),
                "an unsupported masonry shows oak");
    }

    /** The same resolver serves the window trims and door rings; only the masonry face differs. */
    @Test
    void hostsDifferOnlyInTheMasonryFace() {
        MasonryMaterials.Material brick = MasonryMaterials.ALL.get(0);
        Appearance bricks = Appearance.masonry(brick.block());
        assertEquals(MasonryStyles.doorFace(brick), TrimLooks.of(bricks, MasonryStyles::doorFace).face());
        assertEquals(TrimLooks.of(bricks, MasonryStyles::doorFace).edge(), TrimLooks.of(bricks, MasonryStyles::squareFace).edge());
        Appearance red = Appearance.DEFAULT.painted(DyeColor.RED);
        assertEquals(TrimLooks.of(red, MasonryStyles::doorFace), TrimLooks.of(red, MasonryStyles::squareFace));
    }
}
