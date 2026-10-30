package io.github.dirtyredz.roundwood.client.arch;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchGeometry;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.client.door.arch.DoorArchModels;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/** Captured 0.16.0 showcase poses/material kinds, replayed with current production geometry. */
class FittedCorpusTest {
    static { MinecraftBootstrap.ensure(); }
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("test", "donor");
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents contents) { super(TEXTURE, contents, 256, 256, 32, 64); }
    }
    private record Sample(DoorDesign design, ArchPieceThickness thickness, ArchPieceDepth depth,
                          DoorArchGeometry.Layer layer, ArchPieceCell cell, boolean masonry) {}

    @Test void censusFittedBodiesAndCasingsStayBelowFourteenThousandQuads() throws Exception {
        try (var input = getClass().getResourceAsStream("/fps/fitted-corpus.json")) {
            assertNotNull(input);
            var cases = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Sample[].class);
            assertEquals(135, cases.length);
            try (var contents = new SpriteContents(TEXTURE, new FrameSize(16, 16), new NativeImage(16, 16, false), net.minecraft.server.packs.resources.ResourceMetadata.EMPTY)) {
                var sprite = new Sprite(contents);
                int total = 0, bodies = 0;
                long started = System.nanoTime();
                var chunkModels = new java.util.ArrayList<net.minecraft.client.resources.model.BakedModel>();
                var legacyQuads = new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
                for (var sample : cases) {
                    if (sample.layer == DoorArchGeometry.Layer.BODY) bodies++;
                    var quads = DoorArchModels.quads(sample.design,sample.thickness,sample.depth,sample.layer,sample.cell,Direction.SOUTH,sprite,sample.masonry);
                    total += quads.size();
                    legacyQuads.addAll(quads);
                    chunkModels.add(new io.github.dirtyredz.roundwood.testsupport.QuadModel(quads, sprite));
                }
                long baked = System.nanoTime();
                var chunkQuads = io.github.dirtyredz.roundwood.client.render.world.StaticWorldModel.collect(chunkModels, null,
                        net.minecraft.util.RandomSource.create(0), null);
                assertEquals(legacyQuads, chunkQuads, "Chunk collection preserves vertices, UVs, sprites, normals, tint and shade/AO flags");
                System.out.printf("Fitted synthetic section: %d models, %d quads; cold bake %.3f ms, cached-model collection %.3f ms%n",
                        chunkModels.size(), chunkQuads.size(), (baked-started)/1e6, (System.nanoTime()-baked)/1e6);
                assertEquals(45, bodies);
                assertEquals(13410, total, "Conforming wood and mortar mesh census");
                assertTrue(total <= 14000, "Fitted corpus emitted " + total + " quads; budget is 14000");
                long[] warm = new long[5];
                for (int run = 0; run < warm.length; run++) {
                    long begin = System.nanoTime();
                    int count = 0;
                    for (var sample : cases) count += DoorArchModels.quads(sample.design,sample.thickness,sample.depth,sample.layer,sample.cell,Direction.SOUTH,sprite,sample.masonry).size();
                    warm[run] = System.nanoTime() - begin;
                    assertEquals(13410, count);
                }
                java.util.Arrays.sort(warm);
                System.out.printf("Fitted synthetic section warmed topology-cache bake median (5 runs): %.3f ms; no lighting/upload included%n", warm[2]/1e6);
            }
        }
    }
}
