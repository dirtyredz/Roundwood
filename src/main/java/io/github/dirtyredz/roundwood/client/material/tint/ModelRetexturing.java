package io.github.dirtyredz.roundwood.client.material.tint;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Immutable transformations of baked quads: another sprite (UVs remapped from the source sprite's
 * rectangle to the target's, so tiled and whole-assembly mappings both survive), a tint index, and
 * quarter turns about the block's vertical axis (positions, normals, face and vertex winding, as a
 * blockstate {@code y} rotation bakes them), and UVs computed from positions. Every result is a new quad over a copied vertex array:
 * shared donor and template quads are never mutated. The array arithmetic is plain and unit-tested.
 */
public final class ModelRetexturing {
    private static final int STRIDE = QuadVertices.STRIDE;
    private static final int UV = 4;

    private ModelRetexturing() {
    }

    /** {@code quad} drawn with {@code target} and {@code tintIndex} ({@code -1}: untinted). */
    public static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite target, int tintIndex) {
        TextureAtlasSprite source = quad.getSprite();
        int[] vertices = QuadVertices.remapUv(quad.getVertices(),
                new float[] {source.getU0(), source.getV0(), source.getU1(), source.getV1()},
                new float[] {target.getU0(), target.getV0(), target.getU1(), target.getV1()});
        return new BakedQuad(vertices, tintIndex, quad.getDirection(), target, quad.isShade(), quad.hasAmbientOcclusion());
    }

    /** Where a corner samples its sprite (0..16 sprite units) from its block-local position (0..1 blocks). */
    @FunctionalInterface
    public interface PositionUv {
        float[] at(float x, float y, float z);
    }

    /**
     * {@code quad} drawn with {@code target}, untinted, every corner's UV computed from its position by
     * {@code uv} (artwork spread over a whole assembly rather than remapped from the source tile).
     */
    public static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite target, PositionUv uv) {
        int[] vertices = quad.getVertices().clone();
        for (int i = 0; i + STRIDE <= vertices.length; i += STRIDE) {
            float[] st = uv.at(Float.intBitsToFloat(vertices[i]), Float.intBitsToFloat(vertices[i + 1]),
                    Float.intBitsToFloat(vertices[i + 2]));
            vertices[i + UV] = Float.floatToRawIntBits(target.getU(st[0] / 16F)); // 1.21 sprite coordinates are 0..1
            vertices[i + UV + 1] = Float.floatToRawIntBits(target.getV(st[1] / 16F));
        }
        return new BakedQuad(vertices, -1, quad.getDirection(), target, quad.isShade(), quad.hasAmbientOcclusion());
    }

    /** {@code quad} turned clockwise (seen from above) by {@code turns} quarter turns about the block centre. */
    public static BakedQuad rotateY(BakedQuad quad, int turns) {
        Direction direction = VoxelShapeFactory.turn(quad.getDirection(), turns);
        int[] vertices = QuadVertices.rewind(QuadVertices.rotateVerticesY(quad.getVertices(), turns), direction);
        return new BakedQuad(vertices, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade(),
                quad.hasAmbientOcclusion());
    }

    /**
     * {@code source} with every quad passed through {@code quads}, culled lists moved to the side
     * {@code sides} names; built once (state-independent sources only), transforms, overrides and
     * render types delegated.
     */
    public static BakedModel map(BakedModel source, UnaryOperator<BakedQuad> quads, UnaryOperator<Direction> sides) {
        RandomSource random = RandomSource.create(42L);
        Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            culled.put(side, new ArrayList<>());
        }
        for (Direction side : Direction.values()) {
            for (BakedQuad quad : source.getQuads(null, side, random, ModelData.EMPTY, null)) {
                culled.get(sides.apply(side)).add(quads.apply(quad));
            }
        }
        List<BakedQuad> unculled = source.getQuads(null, null, random, ModelData.EMPTY, null).stream().map(quads).toList();
        Map<Direction, List<BakedQuad>> frozen = new EnumMap<>(Direction.class);
        culled.forEach((side, list) -> frozen.put(side, List.copyOf(list)));
        return new Mapped(source, unculled, frozen);
    }

    /** A model with precomputed quads in front of its source's transforms and render types. */
    static final class Mapped extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> unculled;
        private final Map<Direction, List<BakedQuad>> culled;

        Mapped(BakedModel source, List<BakedQuad> unculled, Map<Direction, List<BakedQuad>> culled) {
            super(source);
            this.unculled = unculled;
            this.culled = culled;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return side == null ? unculled : culled.get(side);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                        ModelData data, @Nullable RenderType renderType) {
            return getQuads(state, side, random);
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return unculled.isEmpty() ? super.getParticleIcon() : unculled.get(0).getSprite();
        }

        @Override
        public TextureAtlasSprite getParticleIcon(ModelData data) {
            return getParticleIcon();
        }
    }
}
