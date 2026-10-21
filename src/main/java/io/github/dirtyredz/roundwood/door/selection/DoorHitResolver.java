package io.github.dirtyredz.roundwood.door.selection;

import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Every part's outline covers the whole door, so a ray can be credited to the first part it passes
 * through while the point it hit lies in another part (looking through an empty corner), or outside
 * the cells altogether (the open leaf's visual-only projection). The server only accepts a hit within
 * one block of its cell's centre, so a hit is re-attributed to the loaded part of the same assembly
 * that actually contains the hit point. A hit with no such part (the projection, or a point in a
 * neighbouring assembly) is refused: it must not be used, broken or picked.
 */
public final class DoorHitResolver {
    /** How far a hit point is moved into the surface it hit before classifying it. */
    private static final double INWARD = 1e-4;

    private DoorHitResolver() {
    }

    /**
     * The hit as it should be acted on: unchanged when it is not on a round door or already in the
     * right cell, re-attributed to the containing part, or empty when no part contains it.
     */
    public static Optional<BlockHitResult> resolve(Level level, BlockHitResult hit) {
        BlockPos hitPos = hit.getBlockPos();
        BlockState state = level.getBlockState(hitPos);
        if (!(state.getBlock() instanceof RoundDoorBlock door)) {
            return Optional.of(hit);
        }
        BlockPos cell = containingCell(hit);
        if (cell.equals(hitPos)) {
            return Optional.of(hit);
        }
        if (!level.isLoaded(cell)) {
            return Optional.empty();
        }
        DoorLayout.Assembly assembly = DoorLayout.of(hitPos, state);
        BlockState owner = level.getBlockState(cell);
        if (owner.is(door) && DoorLayout.of(cell, owner).equals(assembly)) {
            return Optional.of(hit.withPosition(cell));
        }
        return Optional.empty();
    }

    /** The cell holding the hit point. */
    public static BlockPos containingCell(BlockHitResult hit) {
        return BlockPos.containing(insidePoint(hit));
    }

    /** The hit point nudged into the surface it hit, so a point on a cell or piece boundary is not ambiguous. */
    public static Vec3 insidePoint(BlockHitResult hit) {
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        return hit.getLocation().subtract(normal.scale(INWARD));
    }
}
