package io.github.dirtyredz.roundwood.fixture;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Real door hits for GameTests (0.8.0): a ray straight through the assembly at ({@code a}, {@code h})
 * (blocks across from the front-left edge, and up; see {@link DoorFixtures#point}), from in front or
 * from behind, clipped against every part's target shape as {@code player} sees it (so held items
 * matter), nearest hit first, then re-attributed to the part containing it as the client does.
 */
public final class DoorRays {
    private DoorRays() {
    }

    public static BlockHitResult through(GameTestHelper helper, ServerPlayer player, DoorLayout.Assembly door, double a, double h,
                                        boolean fromFront) {
        Vec3 from = DoorFixtures.point(door, a, h, fromFront ? -0.75 : 1.75);
        Vec3 to = DoorFixtures.point(door, a, h, fromFront ? 1.75 : -0.75);
        BlockHitResult best = null;
        for (BlockPos cell : door.positions()) {
            BlockState state = helper.getLevel().getBlockState(cell);
            BlockHitResult hit = state.getShape(helper.getLevel(), cell, CollisionContext.of(player)).clip(from, to, cell);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK
                    && (best == null || hit.getLocation().distanceToSqr(from) < best.getLocation().distanceToSqr(from))) {
                best = hit;
            }
        }
        if (best == null) {
            throw new GameTestAssertException("ray at " + a + "," + h + (fromFront ? " from the front" : " from behind") + " missed the door");
        }
        BlockHitResult found = best;
        return DoorHitResolver.resolve(helper.getLevel(), found)
                .orElseThrow(() -> new GameTestAssertException("hit not in any part: " + found.getLocation()));
    }
}
