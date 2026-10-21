package io.github.dirtyredz.roundwood.showcase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The showcase's coordinate frame: {@code r} runs to the builder's right, {@code f} straight ahead,
 * {@code y} up from the player's feet. Content lives in {@code [0,WIDTH) x [0,HEIGHT) x [0,DEPTH)};
 * the bounds add one block of margin on every side (and the floor below), so nothing a section
 * places ever touches, or sends an update across, the edge of the area that gets rewritten.
 */
public final class ShowcaseFrame {
    public static final int WIDTH = 60;
    public static final int DEPTH = 70;
    public static final int HEIGHT = 6;

    private final BlockPos origin;
    private final Direction forward;
    private final Direction right;

    private ShowcaseFrame(BlockPos origin, Direction forward) {
        this.origin = origin;
        this.forward = forward;
        this.right = forward.getClockWise();
    }

    /** The frame for a builder standing at {@code feet} looking along {@code facing}: starts 3 ahead, centred on them. */
    public static ShowcaseFrame at(BlockPos feet, Direction facing) {
        BlockPos origin = feet.relative(facing, 3).relative(facing.getCounterClockWise(), WIDTH / 2);
        return new ShowcaseFrame(origin.immutable(), facing);
    }

    public Direction forward() {
        return forward;
    }

    public Direction right() {
        return right;
    }

    public BlockPos pos(int r, int y, int f) {
        return origin.relative(right, r).relative(forward, f).above(y);
    }

    /** Everything the builder may rewrite: content, a one-block margin, and the floor layer. */
    public BoundingBox bounds() {
        return BoundingBox.fromCorners(pos(-1, -1, -1), pos(WIDTH, HEIGHT, DEPTH));
    }

    public int floorY() {
        return origin.getY() - 1;
    }
}
