package io.github.dirtyredz.roundwood.client.material;

import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
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
    /** Ints per vertex in the block format: position (3), colour, uv (2), light, normal. */
    public static final int STRIDE = 8;
    private static final int UV = 4;
    private static final int NORMAL = 7;

    private ModelRetexturing() {
    }

    /** {@code quad} drawn with {@code target} and {@code tintIndex} ({@code -1}: untinted). */
    public static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite target, int tintIndex) {
        TextureAtlasSprite source = quad.getSprite();
        int[] vertices = remapUv(quad.getVertices(),
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
            vertices[i + UV] = Float.floatToRawIntBits(target.getU(st[0]));
            vertices[i + UV + 1] = Float.floatToRawIntBits(target.getV(st[1]));
        }
        return new BakedQuad(vertices, -1, quad.getDirection(), target, quad.isShade(), quad.hasAmbientOcclusion());
    }

    /** {@code quad} turned clockwise (seen from above) by {@code turns} quarter turns about the block centre. */
    public static BakedQuad rotateY(BakedQuad quad, int turns) {
        Direction direction = turn(quad.getDirection(), turns);
        int[] vertices = rewind(rotateVerticesY(quad.getVertices(), turns), direction);
        return new BakedQuad(vertices, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade(),
                quad.hasAmbientOcclusion());
    }

    /**
     * A copy of {@code vertices} with every UV moved from the {@code from} rectangle {u0, v0, u1, v1}
     * to the same relative place in {@code to}.
     */
    public static int[] remapUv(int[] vertices, float[] from, float[] to) {
        int[] out = vertices.clone();
        for (int i = 0; i + STRIDE <= out.length; i += STRIDE) {
            float u = Float.intBitsToFloat(out[i + UV]);
            float v = Float.intBitsToFloat(out[i + UV + 1]);
            out[i + UV] = Float.floatToRawIntBits(to[0] + (u - from[0]) / (from[2] - from[0]) * (to[2] - to[0]));
            out[i + UV + 1] = Float.floatToRawIntBits(to[1] + (v - from[1]) / (from[3] - from[1]) * (to[3] - to[1]));
        }
        return out;
    }

    /** A copy of {@code vertices} turned about (0.5, *, 0.5): per turn {@code (x, z) -> (1 - z, x)}, normals alike. */
    public static int[] rotateVerticesY(int[] vertices, int turns) {
        int[] out = vertices.clone();
        int n = Math.floorMod(turns, 4);
        for (int i = 0; i + STRIDE <= out.length; i += STRIDE) {
            float x = Float.intBitsToFloat(out[i]);
            float z = Float.intBitsToFloat(out[i + 2]);
            int normal = out[i + NORMAL];
            int nx = (byte) normal;
            int nz = (byte) (normal >> 16);
            for (int t = 0; t < n; t++) {
                float turnedX = 1 - z;
                z = x;
                x = turnedX;
                int turnedNx = -nz;
                nz = nx;
                nx = turnedNx;
            }
            out[i] = Float.floatToRawIntBits(x);
            out[i + 2] = Float.floatToRawIntBits(z);
            out[i + NORMAL] = (normal & 0xFF00FF00) | (nx & 0xFF) | ((nz & 0xFF) << 16);
        }
        return out;
    }

    /** {@code side} turned like {@link #rotateVerticesY} (vertical sides stay). */
    public static Direction turn(Direction side, int turns) {
        Direction out = side;
        if (out.getAxis().isHorizontal()) {
            for (int i = 0; i < Math.floorMod(turns, 4); i++) {
                out = out.getClockWise();
            }
        }
        return out;
    }

    /**
     * Reorders a quad's corners into the order vanilla bakes for a face looking {@code direction}
     * (what {@code FaceBakery} does after a rotation), carrying each corner's UV with it, so smooth
     * lighting reads every corner where it expects it.
     */
    public static int[] rewind(int[] vertices, Direction direction) {
        int[] copy = vertices.clone();
        int[] out = vertices.clone();
        float[] bounds = new float[Direction.values().length];
        bounds[FaceInfo.Constants.MIN_X] = Float.MAX_VALUE;
        bounds[FaceInfo.Constants.MIN_Y] = Float.MAX_VALUE;
        bounds[FaceInfo.Constants.MIN_Z] = Float.MAX_VALUE;
        bounds[FaceInfo.Constants.MAX_X] = -Float.MAX_VALUE;
        bounds[FaceInfo.Constants.MAX_Y] = -Float.MAX_VALUE;
        bounds[FaceInfo.Constants.MAX_Z] = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float x = Float.intBitsToFloat(copy[i * STRIDE]);
            float y = Float.intBitsToFloat(copy[i * STRIDE + 1]);
            float z = Float.intBitsToFloat(copy[i * STRIDE + 2]);
            bounds[FaceInfo.Constants.MIN_X] = Math.min(bounds[FaceInfo.Constants.MIN_X], x);
            bounds[FaceInfo.Constants.MIN_Y] = Math.min(bounds[FaceInfo.Constants.MIN_Y], y);
            bounds[FaceInfo.Constants.MIN_Z] = Math.min(bounds[FaceInfo.Constants.MIN_Z], z);
            bounds[FaceInfo.Constants.MAX_X] = Math.max(bounds[FaceInfo.Constants.MAX_X], x);
            bounds[FaceInfo.Constants.MAX_Y] = Math.max(bounds[FaceInfo.Constants.MAX_Y], y);
            bounds[FaceInfo.Constants.MAX_Z] = Math.max(bounds[FaceInfo.Constants.MAX_Z], z);
        }
        FaceInfo face = FaceInfo.fromFacing(direction);
        for (int i = 0; i < 4; i++) {
            FaceInfo.VertexInfo corner = face.getVertexInfo(i);
            float x = bounds[corner.xFace];
            float y = bounds[corner.yFace];
            float z = bounds[corner.zFace];
            out[i * STRIDE] = Float.floatToRawIntBits(x);
            out[i * STRIDE + 1] = Float.floatToRawIntBits(y);
            out[i * STRIDE + 2] = Float.floatToRawIntBits(z);
            for (int k = 0; k < 4; k++) {
                if (Mth.equal(x, Float.intBitsToFloat(copy[k * STRIDE]))
                        && Mth.equal(y, Float.intBitsToFloat(copy[k * STRIDE + 1]))
                        && Mth.equal(z, Float.intBitsToFloat(copy[k * STRIDE + 2]))) {
                    out[i * STRIDE + UV] = copy[k * STRIDE + UV];
                    out[i * STRIDE + UV + 1] = copy[k * STRIDE + UV + 1];
                }
            }
        }
        return out;
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
