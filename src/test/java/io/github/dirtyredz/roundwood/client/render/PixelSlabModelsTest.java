package io.github.dirtyredz.roundwood.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimGeometry;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class PixelSlabModelsTest {
    static { MinecraftBootstrap.ensure(); }
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("test", "planks");
    private static final PixelSlabModels.Extent EXTENT = new PixelSlabModels.Extent(32, 32, 32);

    /** Real sprite coordinates in a nonzero atlas region; no GL context or client is needed. */
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents contents) { super(TEXTURE, contents, 256, 256, 32, 64); }
    }

    @Test
    void everyArchwayQuadHasVanillaOrientationAndOneUvUnitPerPixel() {
        try (var contents = new SpriteContents(TEXTURE, new FrameSize(16, 16),
                new NativeImage(16, 16, false), AnimationMetadataSection.EMPTY)) {
            var sprite = new Sprite(contents);
            var seen = EnumSet.noneOf(Direction.class);
            for (ArchwayDesign design : ArchwayDesign.values()) {
                check(design::inFrame, ArchwayDesign.BACK, ArchwayDesign.FRONT, sprite, seen);
                for (AttachmentFace face : AttachmentFace.values()) {
                    double[] depth = ArchwayTrimGeometry.depth(face);
                    check((u, v) -> ArchwayTrimGeometry.inCasing(design, u, v), depth[0], depth[1], sprite, seen);
                }
            }
            assertEquals(EnumSet.allOf(Direction.class), seen);
        }
    }

    private static void check(PixelCircle.PixelTest mask, double back, double front, Sprite sprite,
                              EnumSet<Direction> seen) {
        var look = new TrimLooks.Look(false, TEXTURE, TEXTURE);
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                for (int turns = 0; turns < 4; turns++) {
                    for (boolean item : new boolean[] {false, true}) {
                        if (item && turns != 0) continue;
                        var quads = PixelSlabModels.quads(mask, column, row, back, front, turns, item, look,
                                EXTENT, ignored -> sprite);
                        assertFalse(quads.isEmpty());
                        for (BakedQuad quad : quads) {
                            seen.add(quad.getDirection());
                            checkQuad(quad, sprite);
                        }
                    }
                }
            }
        }
    }

    private static void checkQuad(BakedQuad quad, Sprite sprite) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        double[][] projected = new double[4][];
        double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int vertex = 0; vertex < 4; vertex++) {
            int i = vertex * stride;
            double x = Float.intBitsToFloat(vertices[i]) * 16;
            double y = Float.intBitsToFloat(vertices[i + 1]) * 16;
            double z = Float.intBitsToFloat(vertices[i + 2]) * 16;
            // Independent vanilla projection, including signs: vertical V always decreases with Y.
            projected[vertex] = switch (quad.getDirection()) {
                case DOWN -> new double[] {x, 16 - z};
                case UP -> new double[] {x, z};
                case NORTH -> new double[] {16 - x, 16 - y};
                case SOUTH -> new double[] {x, 16 - y};
                case WEST -> new double[] {z, 16 - y};
                case EAST -> new double[] {16 - z, 16 - y};
            };
            for (int axis = 0; axis < 2; axis++) {
                min[axis] = Math.min(min[axis], projected[vertex][axis]);
                max[axis] = Math.max(max[axis], projected[vertex][axis]);
            }
        }
        for (int axis = 0; axis < 2; axis++) {
            assertTrue(max[axis] > min[axis], "nonzero geometric span");
            double offset = Math.floor(min[axis] / 16) * 16;
            double centre = (min[axis] + max[axis]) / 2 - offset;
            double uvMin = Double.POSITIVE_INFINITY, uvMax = Double.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                double atlasUv = Float.intBitsToFloat(vertices[vertex * stride + 4 + axis]);
                double uv = axis == 0 ? sprite.getUOffset((float) atlasUv) : sprite.getVOffset((float) atlasUv);
                // FaceBakery shrinks toward the centre, then fillVertex mixes 0.999 of this corner
                // with 0.001 of the opposite corner. Undo both vanilla anti-bleed insets only.
                double unshrunk = centre + (uv - centre) / ((1 - sprite.uvShrinkRatio()) * 0.998);
                assertEquals(projected[vertex][axis] - offset, unshrunk, 0.0001,
                        quad.getDirection() + " vertex " + vertex + " UV axis " + axis);
                assertTrue(uv >= 0 && uv <= 16, "UV stays inside its sprite");
                uvMin = Math.min(uvMin, unshrunk);
                uvMax = Math.max(uvMax, unshrunk);
            }
            assertEquals(max[axis] - min[axis], uvMax - uvMin, 0.0001, "UV span equals geometric extent");
        }
    }
}
