package io.github.dirtyredz.roundwood.block.assembly.placement;

import net.minecraft.core.Direction;

/** Aimed-face thirds shared by saved door and arch poses. Band order is BACK, MIDDLE, FRONT. */
public final class FaceDepthBands {
    private FaceDepthBands() {}
    public record Zone(double minU, double minV, double maxU, double maxV) {}

    public static Zone zone(int band, Direction facing, Direction face) {
        boolean wallFront = face.getAxis() == facing.getAxis();
        boolean splitU = !wallFront && (face.getAxis() != Direction.Axis.Y || facing.getAxis() == Direction.Axis.X);
        int step = facing.getAxis() == Direction.Axis.X ? facing.getStepX() : facing.getStepZ();
        if (!wallFront && step > 0) band = 2 - band;
        double low = band / 3.0, high = (band + 1) / 3.0;
        return splitU ? new Zone(low, 0, high, 1) : new Zone(0, low, 1, high);
    }

    public static int target(Direction facing, Direction face, double x, double y, double z) {
        double depth = face.getAxis() == facing.getAxis() ? y
                : .5 - (x - .5) * facing.getStepX() - (z - .5) * facing.getStepZ();
        if (!Double.isFinite(depth)) throw new IllegalArgumentException("Finite hit required");
        return depth < 1.0 / 3 ? 0 : depth > 2.0 / 3 ? 2 : 1;
    }
}
