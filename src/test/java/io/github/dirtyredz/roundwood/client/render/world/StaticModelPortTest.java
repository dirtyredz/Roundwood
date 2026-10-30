package io.github.dirtyredz.roundwood.client.render.world;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/** The family-neutral static-render seams: explicit block bindings and the shared cell-layer order. */
class StaticModelPortTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }

    @Test void registeredBlocksResolveTheirExplicitSourcesAndUnboundBlocksHaveNone() {
        StaticModelSource source = (state, snapshot) -> List.of();
        // Registries are frozen under FML's test launcher, so two registered blocks stand in for host families.
        Block bound = net.minecraft.world.level.block.Blocks.STONE;
        Block other = net.minecraft.world.level.block.Blocks.DIRT;
        var sources = StaticModelSources.builder().bind(bound, source).build();
        assertSame(source, sources.find(bound).orElseThrow());
        assertTrue(sources.find(other).isEmpty());
        assertThrows(IllegalStateException.class, () -> StaticModelSources.builder().bind(bound, source).bind(bound, source));
        assertFalse(source.culledBySide(), "sources draw unculled by default");
    }

    @Test void cellLayersPutTheBodyFirstThenCasingsInFaceOrder() {
        var body = "body";
        var front = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));
        var back = Appearance.DEFAULT;
        var baked = new ArrayList<AttachmentFace>();
        var layers = CellLayers.of(body, new FaceTrims(Optional.of(front), Optional.of(back)), (face, look) -> {
            assertEquals(face == AttachmentFace.FRONT ? front : back, look);
            baked.add(face);
            return "casing:" + face;
        });
        assertEquals(List.of(AttachmentFace.FRONT, AttachmentFace.BACK), baked);
        assertEquals(List.of("body", "casing:FRONT", "casing:BACK"), layers);
        var backOnly = new FaceTrims(Optional.empty(), Optional.of(back));
        assertEquals(1, CellLayers.casings(backOnly, (face, look) -> face).size());
        assertEquals(List.of(body), CellLayers.<String>of(body, FaceTrims.NONE, (face, look) -> fail("no trims, no casings")));
    }
}
