package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.client.material.tint.TintBindings;
import io.github.dirtyredz.roundwood.client.render.world.StaticQuadTransform;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.dirtyredz.roundwood.block.door.assembly.*;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageCoverage;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.client.material.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaticCamouflageGeometryTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("test", "tinted_cube");
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents contents) { super(TEXTURE, contents, 256, 256, 32, 64); }
    }
    @Test void tintedCornerQuadsPreserveGeometryFlagsAndOriginalDonorIndices() {
        try (var contents = new SpriteContents(TEXTURE, new FrameSize(16,16), new NativeImage(16,16,false), net.minecraft.server.packs.resources.ResourceMetadata.EMPTY)) {
            var sprite = new Sprite(contents);
            var bindings = new TintBindings<String>();
            int tinted = 0, untinted = 0;
            for (var facing : Direction.Plane.HORIZONTAL) for (var depth : DoorDepth.values())
                for (boolean fitted : new boolean[]{false, true}) for (var part : DoorLayout.Part.of(DoorDesign.ROUND)) {
                    var original = CamouflageModels.bake(DoorDesign.ROUND, facing, part, CamouflageCoverage.of(DoorDesign.ROUND, fitted),
                            side -> new DonorSprites.FaceMaterial(sprite, side == Direction.UP ? 3 : side == Direction.NORTH ? -1 : 7));
                    double x = -facing.getStepX()*depth.offset()/16, z = -facing.getStepZ()*depth.offset()/16;
                    for (var quad : original) {
                        int tint = bindings.bind("synthetic_cube", quad.getTintIndex());
                        var chunk = StaticQuadTransform.translate(quad, x, 0, z, tint);
                        if (quad.isTinted()) {
                            tinted++;
                            assertEquals(new TintBindings.Binding<>("synthetic_cube", quad.getTintIndex()), bindings.get(chunk.getTintIndex()));
                        } else { untinted++; assertEquals(-1, chunk.getTintIndex()); }
                        assertSame(sprite, chunk.getSprite()); assertEquals(quad.getDirection(), chunk.getDirection());
                        assertEquals(quad.isShade(), chunk.isShade()); assertEquals(quad.hasAmbientOcclusion(), chunk.hasAmbientOcclusion());
                        for (int vertex = 0; vertex < 32; vertex += 8) {
                            assertEquals((float)(Float.intBitsToFloat(quad.getVertices()[vertex])+x), Float.intBitsToFloat(chunk.getVertices()[vertex]));
                            assertEquals(quad.getVertices()[vertex+1], chunk.getVertices()[vertex+1]);
                            assertEquals((float)(Float.intBitsToFloat(quad.getVertices()[vertex+2])+z), Float.intBitsToFloat(chunk.getVertices()[vertex+2]));
                            for (int attribute = 3; attribute < 8; attribute++) assertEquals(quad.getVertices()[vertex+attribute], chunk.getVertices()[vertex+attribute]);
                        }
                    }
                }
            assertTrue(tinted > 0); assertTrue(untinted > 0);
        }
    }
}
