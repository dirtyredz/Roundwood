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
        double k = SPRITE / size;
        double v1 = (size - box.maxY()) * k;
        double v2 = (size - box.minY()) * k;
        return switch (face) {
            case SOUTH -> new float[] {(float) (box.minX() * k), (float) v1, (float) (box.maxX() * k), (float) v2};
            case NORTH -> new float[] {(float) ((size - box.maxX()) * k), (float) v1, (float) ((size - box.minX()) * k), (float) v2};
            default -> null;
        };
    }
}
