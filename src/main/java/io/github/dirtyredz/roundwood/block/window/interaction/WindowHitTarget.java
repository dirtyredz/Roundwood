package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowFaceIdentity;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Where a click on a window landed (0.8.0): which face slot it addresses and which region of that
 * face it is ({@link WindowHitRegions}). Converts a Minecraft hit into the pane's own coordinates;
 * routing ({@link WindowInteraction}), the trim item and the sneak-use bridge all use this one result.
 *
 * <p>The side is the hit's position along the normal against the slab's middle; the hit point is
 * first nudged into the surface it hit. An exact tie (a thin edge of the slab) goes to the viewer's
 * side, and to FRONT when there is no viewer. A point outside the slab is a trim band or an arch sill
 * (when it lies in one installed on that side), or nothing.
 */
public record WindowHitTarget(AttachmentFace face, Direction side, WindowHitRegions.Region region) {
    private static final double NUDGE = 1e-4;
    private static final double TIE = 1e-6;
    private static final double PIXELS = 16;

    /**
     * @param hit       the hit point, world coordinates
     * @param hitFace   the side of the shape the ray hit
     * @param viewer    the clicking player's eye, or null
     * @param trims     the window's installed perimeter trims
     * @param sills     the window's installed arch sills (0.10.0)
     */
    public static Optional<WindowHitTarget> of(BlockState state, BlockPos pos, Vec3 hit, Direction hitFace,
                                               @Nullable Vec3 viewer, FaceTrims trims, FaceTrims sills) {
        return of(state, pos, hit, hitFace, viewer, trims, sills,
                state.getBlock() instanceof WindowPaneBlock block ? block.design() : null);
    }

    public static Optional<WindowHitTarget> of(BlockState state, BlockPos pos, Vec3 hit, Direction hitFace,
            @Nullable Vec3 viewer, FaceTrims trims, FaceTrims sills, WindowDesign design) {
        if (!(state.getBlock() instanceof WindowPaneBlock block)) {
            return Optional.empty();
        }
        WindowPose pose = block.pose(state);
        boolean reversed = WindowPaneBlock.faceReversed(state);
        Vec3 inside = hit.subtract(Vec3.atLowerCornerOf(hitFace.getNormal()).scale(NUDGE));
        double[] p = {
                (inside.x - pos.getX()) * PIXELS, (inside.y - pos.getY()) * PIXELS, (inside.z - pos.getZ()) * PIXELS};
        int n = pose.normal().ordinal();
        double along = p[n];
        double slabMin = pose.depth().slabMin();
        double slabMax = pose.depth().slabMax();
        double middle = (slabMin + slabMax) / 2;
        Direction positive = Direction.fromAxisAndDirection(pose.normal(), Direction.AxisDirection.POSITIVE);
        Direction side;
        if (along > middle + TIE) {
            side = positive;
        } else if (along < middle - TIE) {
            side = positive.getOpposite();
        } else if (viewer != null) {
            double eye = (n == 0 ? viewer.x - pos.getX() : n == 1 ? viewer.y - pos.getY() : viewer.z - pos.getZ()) * PIXELS;
            side = eye >= middle ? positive : positive.getOpposite();
        } else {
            side = WindowFaceIdentity.side(pose, reversed, AttachmentFace.FRONT);
        }
        AttachmentFace face = WindowFaceIdentity.face(pose, reversed, side);
        double[] uv = pose.facePoint(p[0], p[1], p[2]);
        boolean trimmed = trims.has(face);
        boolean silled = sills.has(face);
        if (along < slabMin || along > slabMax) {
            double[] band = WindowTrimGeometry.depth(pose, side);
            if ((trimmed || silled) && along >= band[0] && along <= band[1]) {
                WindowHitRegions.Region region = WindowHitRegions.classify(design, uv[0], uv[1], trimmed, silled);
                if (region.slot() != null) {
                    return Optional.of(new WindowHitTarget(face, side, region));
                }
            }
            return Optional.of(new WindowHitTarget(face, side, WindowHitRegions.Region.NONE));
        }
        return Optional.of(new WindowHitTarget(face, side, WindowHitRegions.classify(design, uv[0], uv[1], trimmed, silled)));
    }
}
