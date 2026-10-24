package io.github.dirtyredz.roundwood.client.material;

import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/** Pure block-format vertex transformations; never mutates the source array. */
public final class QuadVertices {
    public static final int STRIDE = 8;
    private static final int UV = 4;
    private static final int NORMAL = 7;

    private QuadVertices() {}

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

}
