package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Which door trim face a click addresses (0.8.0), from the hit point in the door's authored frame
 * (nudged into the surface hit), against the door design's own casing outline (0.11.0: the round
 * ring or the arched jambs and crown). The casing is static, so the rule ignores the swing animation:
 * <ul>
 *     <li>{@link #ring}: a hit on a ring's footprint, at the depth of that face's ring or of its
 *     in-cell target (closed: anywhere on the leaf's face over the footprint) - restyling, shears;</li>
 *     <li>{@link #fit}: where a Universal Trim attaches. Closed door: any hit inside the ring's
 *     outer edge, the face by depth; open door: only a ring target.</li>
 * </ul>
 */
public final class DoorTrimTarget {
    private DoorTrimTarget() {
    }

    /** The face whose ring (installed or not) the hit is on, if any. */
    public static Optional<AttachmentFace> ring(BlockState state, BlockPos pos, BlockHitResult hit) {
        Vec3 local = authored(state, pos, hit);
        if (!DoorTrimGeometry.onRing(DoorLayout.designOf(state), local.x, local.y)) {
            return Optional.empty();
        }
        AttachmentFace face = AttachmentFace.at(local.z, DoorTrimGeometry.MIDDLE);
        if (!state.getValue(DoorBlock.OPEN) || DoorTrimGeometry.inTargetDepth(face, local.z)) {
            return Optional.of(face);
        }
        return Optional.empty();
    }

    /** The face the door trim item would attach to for this hit, if any. */
    public static Optional<AttachmentFace> fit(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (state.getValue(DoorBlock.OPEN)) {
            return ring(state, pos, hit);
        }
        Vec3 local = authored(state, pos, hit);
        return DoorTrimGeometry.withinRing(DoorLayout.designOf(state), local.x, local.y)
                ? Optional.of(AttachmentFace.at(local.z, DoorTrimGeometry.MIDDLE)) : Optional.empty();
    }

    private static Vec3 authored(BlockState state, BlockPos pos, BlockHitResult hit) {
        return DoorLayout.of(pos, state).toAuthored(DoorHitResolver.insidePoint(hit));
    }
}
