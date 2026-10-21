package io.github.dirtyredz.roundwood.geometry;

import net.minecraft.core.Direction;

/**
 * Vanilla's default face UVs ({@code BlockElement.uvsByFace}) for a box inside one 0..16 tile: the
 * texture region a full block would show at that spot, so pieces tile like real blocks. Shared by
 * the model generator and the client's camouflage models.
 */
public final class FaceUv {
    private static final double TILE = 16;

    private FaceUv() {
    }

    public static float[] of(Box b, Direction face) {
        double[] uv = switch (face) {
            case DOWN -> new double[] {b.minX(), TILE - b.maxZ(), b.maxX(), TILE - b.minZ()};
            case UP -> new double[] {b.minX(), b.minZ(), b.maxX(), b.maxZ()};
            case NORTH -> new double[] {TILE - b.maxX(), TILE - b.maxY(), TILE - b.minX(), TILE - b.minY()};
            case SOUTH -> new double[] {b.minX(), TILE - b.maxY(), b.maxX(), TILE - b.minY()};
            case WEST -> new double[] {b.minZ(), TILE - b.maxY(), b.maxZ(), TILE - b.minY()};
            case EAST -> new double[] {TILE - b.maxZ(), TILE - b.maxY(), TILE - b.minZ(), TILE - b.minY()};
        };
        return new float[] {(float) uv[0], (float) uv[1], (float) uv[2], (float) uv[3]};
    }
}
