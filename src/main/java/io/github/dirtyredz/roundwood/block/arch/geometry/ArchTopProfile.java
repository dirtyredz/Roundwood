package io.github.dirtyredz.roundwood.block.arch.geometry;

/** Filled rectangle minus a true upper semicircle, sampled at pixel centres. */
public record ArchTopProfile(int width, int height, int radius) {
    public ArchTopProfile {
        if (width <= 0 || height <= 0 || radius <= 0 || radius * 2 > width || radius > height) {
            throw new IllegalArgumentException("Invalid semicircular top dimensions");
        }
    }

    public boolean contains(int u, int v) {
        if (u < 0 || u >= width || v < 0 || v >= height) return false;
        double x = u + 0.5 - width / 2.0, y = v + 0.5;
        return x * x + y * y > radius * radius;
    }
}
