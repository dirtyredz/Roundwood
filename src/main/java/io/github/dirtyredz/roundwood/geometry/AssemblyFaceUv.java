package io.github.dirtyredz.roundwood.geometry;

import net.minecraft.core.Direction;

/**
 * Whole-assembly face UVs: one image spread once across an assembly's front and back faces (the
 * 32 x 32 pixel door leaf or frame ring), instead of repeating per 16-pixel tile. A box face at
 * assembly position (x, y) samples the image at that same spot, so features such as full-height
 * boards or radial brick joints run unbroken across element and cell boundaries.
 *
 * <p>Front ({@code SOUTH}, the side a north-authored model shows the placing player) reads the image
 * as seen from the front; back ({@code NORTH}) as seen from behind, like vanilla's default UVs.
 * Other faces have no assembly mapping (null): they keep the ordinary tiled mapping.
 *
 * <p>Rectangular assemblies (0.11.0, the 16 x 32 arched door) keep the image's scale: an image
 * covering {@code image} x {@code image} pixels is sampled one texel per model pixel as on the
 * square door, the assembly using the image's top-left {@code width} x {@code height} (the left half
 * of a 32-pixel board panel), and its back mirrored within its own width. No square art is stretched.
 */
public final class AssemblyFaceUv {
    private static final double SPRITE = 16;

    private AssemblyFaceUv() {
    }

    /**
     * @param box  the face's box in assembly coordinates (x across, y up, 0..{@code size})
     * @param size the assembly's width and height in pixels, which the whole image covers
     * @return {u1, v1, u2, v2} in sprite units (0..16), or null for a face that is not front or back
     */
    public static float[] of(Box box, Direction face, double size) {
        return of(box, face, size, size, size);
    }

    /**
     * A rectangular assembly's face.
     *
     * @param width  the assembly's width in pixels
     * @param height the assembly's height in pixels
     * @param image  how many pixels the image spans (on both axes; at least the width and height)
     * @return {u1, v1, u2, v2} in sprite units (0..16), or null for a face that is not front or back
     */
    public static float[] of(Box box, Direction face, double width, double height, double image) {
        if (face != Direction.SOUTH && face != Direction.NORTH) {
            return null;
        }
        boolean front = face == Direction.SOUTH;
        double[] a = point(box.minX(), box.maxY(), front, width, height, image);
        double[] b = point(box.maxX(), box.minY(), front, width, height, image);
        return new float[] {(float) Math.min(a[0], b[0]), (float) a[1], (float) Math.max(a[0], b[0]), (float) b[1]};
    }

    /**
     * Where the assembly point ({@code x}, {@code y}) samples the image, {u, v} in sprite units, on the
     * front or (mirrored within {@code width}) the back.
     */
    public static double[] point(double x, double y, boolean front, double width, double height, double image) {
        double k = SPRITE / image;
        return new double[] {(front ? x : width - x) * k, (height - y) * k};
    }
}
