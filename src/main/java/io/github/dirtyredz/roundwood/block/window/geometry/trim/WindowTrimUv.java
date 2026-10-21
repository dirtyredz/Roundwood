package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;

/**
 * Where a point on a trim's front or back face samples whole-face artwork (paint boards, masonry):
 * one image spread once over the face it belongs to, the round window's 16 pixels or the 2 x 2
 * arched window's 32, so brick courses and boards run on across the four pieces. The front reads the
 * image as seen from the front, the back as seen from behind (mirrored), like
 * {@code geometry/AssemblyFaceUv}. Tiled looks (wood) need no remapping: the model's own UVs already
 * repeat per block. Plain Java.
 */
public final class WindowTrimUv {
    private static final double SPRITE = 16;

    private WindowTrimUv() {
    }

    /**
     * @param x, y, z a point on the face, block-local pixels
     * @param front   true for the face looking the pose's front way, false for the back
     * @return {u, v} in sprite units (0..16)
     */
    public static float[] whole(WindowProfile profile, WindowPose pose, double x, double y, double z, boolean front) {
        double[] face = pose.facePoint(x, y, z);
        double size = profile.assemblySize();
        double k = SPRITE / size;
        double u = profile.assemblyU() + face[0];
        double v = profile.assemblyV() + face[1];
        return new float[] {(float) ((front ? u : size - u) * k), (float) ((size - v) * k)};
    }
}
