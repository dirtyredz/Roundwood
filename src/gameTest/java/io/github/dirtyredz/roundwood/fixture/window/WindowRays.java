package io.github.dirtyredz.roundwood.fixture.window;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Real window hits for GameTests (0.8.0): a ray along the pane's normal through face pixel (u, v),
 * from outside the cell on one side, clipped against the window's own target shape as the player
 * sees it, exactly as a client's crosshair ray would land. No synthetic hit points.
 */
public final class WindowRays {
    private static final double PIXELS = 16;

    private WindowRays() {
    }

    /**
     * The hit of a ray through face point (u, v) (pixels as seen from the front) of the window at the
     * relative {@code pos}, travelling from beyond its {@code side} (a world direction along the
     * normal) towards the other side. Fails the test if the ray misses the window.
     */
    public static BlockHitResult face(GameTestHelper helper, ServerPlayer player, BlockPos pos, double u, double v, Direction side) {
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        if (!(state.getBlock() instanceof WindowPaneBlock block)) {
            throw new GameTestAssertException("no window at " + abs + ": " + state);
        }
        WindowPose pose = block.pose(state);
        if (side.getAxis() != pose.normal()) {
            throw new GameTestAssertException(side + " is not along the normal of " + pose);
        }
        boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        Vec3 from = world(abs, pose, u, v, positive ? 24 : -8);
        Vec3 to = world(abs, pose, u, v, positive ? -8 : 24);
        BlockHitResult hit = state.getShape(helper.getLevel(), abs, CollisionContext.of(player)).clip(from, to, abs);
        if (hit == null) {
            throw new GameTestAssertException("ray through " + u + "," + v + " from " + side + " missed the window at " + abs);
        }
        return hit;
    }

    /** The world point of face pixel (u, v) at depth {@code n} along the normal's positive axis. */
    private static Vec3 world(BlockPos abs, WindowPose pose, double u, double v, double n) {
        double[] p = new double[3];
        place(p, pose.right(), u);
        place(p, pose.top(), v);
        p[pose.normal().ordinal()] = n;
        return new Vec3(abs.getX() + p[0] / PIXELS, abs.getY() + p[1] / PIXELS, abs.getZ() + p[2] / PIXELS);
    }

    private static void place(double[] p, Direction along, double value) {
        int i = along.getAxis().ordinal();
        p[i] = along.getAxisDirection() == Direction.AxisDirection.POSITIVE ? value : PIXELS - value;
    }
}
