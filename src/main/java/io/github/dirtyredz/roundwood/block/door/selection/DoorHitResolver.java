package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Every part's outline covers the whole door, so a ray can be credited to the first part it passes
 * through while the point it hit lies in another part (looking through an empty corner), or outside
 * the cells altogether (the open leaf's projection). In-cell hits belong to their containing part;
 * projected leaf hits belong to the nearest loaded part of the same assembly. The actual point is
 * never moved: region classification and the highlight must agree. Projected use needs the dedicated
 * server path because vanilla's packet limits the point to one block from the owning cell's centre.
 */
public final class DoorHitResolver {
    /** How far a hit point is moved into the surface it hit before classifying it. */
    private static final double INWARD = 1e-4;

    private DoorHitResolver() {
    }

    /**
     * The hit as it should be acted on: unchanged when it is not on a round door or already in the
     * right cell, re-attributed to a containing/nearest part, or empty for an unowned point.
     */
    public static Optional<BlockHitResult> resolve(Level level, BlockHitResult hit) {
        BlockPos hitPos = hit.getBlockPos();
        BlockState state = level.getBlockState(hitPos);
        if (!(state.getBlock() instanceof AssemblyDoorBlock door)) {
            return Optional.of(hit);
        }
        DoorLayout.Assembly assembly = DoorLayout.of(hitPos, state);
        return resolve(assembly, state.getValue(DoorBlock.OPEN), hit, cell -> {
            if (!level.isLoaded(cell)) {
                return false;
            }
            BlockState owner = level.getBlockState(cell);
            return owner.is(door) && DoorLayout.of(cell, owner).equals(assembly);
        });
    }

    /** Geometry and ownership policy, independent of a live level. */
    static Optional<BlockHitResult> resolve(DoorLayout.Assembly assembly, boolean open, BlockHitResult hit,
                                             Predicate<BlockPos> ownsLoadedCell) {
        BlockPos hitPos = hit.getBlockPos();
        BlockPos cell = containingCell(hit);
        if (cell.equals(hitPos)) {
            return Optional.of(hit);
        }
        if (ownsLoadedCell.test(cell)) {
            return Optional.of(hit.withPosition(cell));
        }
        if (open && onLeaf(assembly, hit)) {
            return assembly.positions().stream().filter(ownsLoadedCell)
                    .min(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(hit.getLocation())))
                    .map(hit::withPosition);
        }
        return Optional.empty();
    }

    /** Only a genuine resting open leaf may claim space outside its occupied cells. */
    private static boolean onLeaf(DoorLayout.Assembly assembly, BlockHitResult hit) {
        Vec3 point = assembly.toAuthored(insidePoint(hit));
        double epsilon = 1e-6; // quarter-turn floating point noise, in pixels
        return DoorGeometry.placedEnvelope(assembly.design(), assembly.hinge(), true).stream().anyMatch(box ->
                point.x >= box.minX() - epsilon && point.x <= box.maxX() + epsilon
                        && point.y >= box.minY() - epsilon && point.y <= box.maxY() + epsilon
                        && point.z >= box.minZ() - epsilon && point.z <= box.maxZ() + epsilon);
    }

    /** A resolved leaf hit outside the assembly cells, requiring the projected-use transport. */
    public static boolean projected(Level level, BlockHitResult hit) {
        BlockState state = level.getBlockState(hit.getBlockPos());
        return state.getBlock() instanceof AssemblyDoorBlock && state.getValue(DoorBlock.OPEN)
                && !DoorLayout.of(hit.getBlockPos(), state).positions().contains(containingCell(hit));
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
