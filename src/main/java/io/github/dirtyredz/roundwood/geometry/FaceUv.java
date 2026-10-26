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

    /**
     * {@link #of} for a box that may lie in a neighbouring tile (0.8.0: a trim projecting one pixel
     * past the cell, at {@code -1..0} or {@code 16..17}): the box is first moved by whole tiles on each
     * axis into {@code 0..16}, so the face samples the same spot of the sprite a block there would,
     * never outside the sprite. A box spanning a tile boundary is refused (split it first).
     */
    public static float[] tiled(Box b, Direction face) {
        // Only the two axes in this face affect its UVs. A render-only normal offset may
        // carry the backing through a tile boundary without changing either texture axis.
        double dx = face.getAxis() == Direction.Axis.X ? 0 : shift(b.minX(), b.maxX());
        double dy = face.getAxis() == Direction.Axis.Y ? 0 : shift(b.minY(), b.maxY());
        double dz = face.getAxis() == Direction.Axis.Z ? 0 : shift(b.minZ(), b.maxZ());
        return of(b.translate(-dx, -dy, -dz), face);
    }

    private static double shift(double min, double max) {
        double tile = Math.floor(min / TILE) * TILE;
        if (max - tile > TILE + 1e-9) {
            throw new IllegalArgumentException("box spans a tile boundary: " + min + ".." + max);
        }
        return tile;
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
