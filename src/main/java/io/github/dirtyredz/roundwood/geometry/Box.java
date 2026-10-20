package io.github.dirtyredz.roundwood.geometry;

import java.util.Optional;

/**
 * An immutable axis-aligned cuboid in model pixels (one block = 16). Shared by models and shapes,
 * so what is drawn and what collides are the same description.
 */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public static final double BLOCK = 16.0;

    public Box {
        if (!(minX < maxX && minY < maxY && minZ < maxZ)) {
            throw new IllegalArgumentException("Empty or inverted box: " + minX + "," + minY + "," + minZ
                    + " -> " + maxX + "," + maxY + "," + maxZ);
        }
    }

    public Box translate(double dx, double dy, double dz) {
        return new Box(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    /** The part of this box inside {@code bounds}, or empty when they only touch or do not meet. */
    public Optional<Box> intersect(Box bounds) {
        double x1 = Math.max(minX, bounds.minX);
        double y1 = Math.max(minY, bounds.minY);
        double z1 = Math.max(minZ, bounds.minZ);
        double x2 = Math.min(maxX, bounds.maxX);
        double y2 = Math.min(maxY, bounds.maxY);
        double z2 = Math.min(maxZ, bounds.maxZ);
        if (x1 < x2 && y1 < y2 && z1 < z2) {
            return Optional.of(new Box(x1, y1, z1, x2, y2, z2));
        }
        return Optional.empty();
    }

    /**
     * Rotates clockwise (seen from above) around the vertical axis through the block centre, in
     * quarter turns. Matches a blockstate {@code "y": 90 * turns} model rotation.
     */
    public Box rotateY(int quarterTurns) {
        Box box = this;
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
            // (x, z) -> (16 - z, x): south edge goes west, north edge goes east.
            box = new Box(BLOCK - box.maxZ, box.minY, box.minX, BLOCK - box.minZ, box.maxY, box.maxX);
        }
        return box;
    }

    public boolean within(Box bounds) {
        return minX >= bounds.minX && minY >= bounds.minY && minZ >= bounds.minZ
                && maxX <= bounds.maxX && maxY <= bounds.maxY && maxZ <= bounds.maxZ;
    }

    public double volume() {
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }

    /** The unit cell {@code [0,16]^3}. */
    public static Box cell() {
        return new Box(0, 0, 0, BLOCK, BLOCK, BLOCK);
    }
}
