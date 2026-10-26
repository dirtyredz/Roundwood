package io.github.dirtyredz.roundwood.block.arch.assembly;

import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import net.minecraft.core.Direction;

/** Pure hit-to-side authority. Coordinates are block-local; overlay and placement share MIDPOINT. */
public final class ArchPieceTargeting {
    public static final double MIDPOINT = 0.5;
    private ArchPieceTargeting() {}

    /** Project a block-local hit onto the authored right axis, in blocks (0..1 on its face). */
    public static double across(Direction facing, double localX, double localZ) {
        if (!facing.getAxis().isHorizontal() || !Double.isFinite(localX) || !Double.isFinite(localZ))
            throw new IllegalArgumentException("A finite hit and horizontal facing are required");
        Direction right = facing.getClockWise();
        return MIDPOINT + (localX - MIDPOINT) * right.getStepX() + (localZ - MIDPOINT) * right.getStepZ();
    }

    /** Side walls choose the touching edge; all other faces split across width, ties LEFT. */
    public static Side side(Direction facing, Direction clickedFace, double localX, double localZ) {
        double across = across(facing, localX, localZ);
        if (clickedFace == facing.getClockWise()) return Side.LEFT;
        if (clickedFace == facing.getCounterClockWise()) return Side.RIGHT;
        return across <= MIDPOINT ? Side.LEFT : Side.RIGHT;
    }
}
