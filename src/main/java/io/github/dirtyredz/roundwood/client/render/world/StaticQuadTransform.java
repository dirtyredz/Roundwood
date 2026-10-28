package io.github.dirtyredz.roundwood.client.render.world;

import net.minecraft.client.renderer.block.model.BakedQuad;

/** Converts the BER pose translation to final owning-cell vertices, preserving all other attributes. */
public final class StaticQuadTransform {
    private StaticQuadTransform() {}
    public static BakedQuad translate(BakedQuad quad, double x, double y, double z, int tint) {
        int[] vertices = quad.getVertices().clone();
        for (int i = 0; i < vertices.length; i += 8) {
            vertices[i] = Float.floatToRawIntBits((float) (Float.intBitsToFloat(vertices[i]) + x));
            vertices[i + 1] = Float.floatToRawIntBits((float) (Float.intBitsToFloat(vertices[i + 1]) + y));
            vertices[i + 2] = Float.floatToRawIntBits((float) (Float.intBitsToFloat(vertices[i + 2]) + z));
        }
        return new BakedQuad(vertices, tint, quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }
}
