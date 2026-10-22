package io.github.dirtyredz.roundwood.block.archway.trim;

import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What a click on an archway addresses (0.12.0), from the hit in the archway's authored frame. Every
 * archway cell's shape is frame only, so a hit is always on the frame:
 * <ul>
 *     <li>on its front or back face (the plane a casing covers): that face's casing, installed or not
 *     ({@link Region#FACE}): shears remove it, planks, masonry or dye restyle an installed one;</li>
 *     <li>on any other surface (the inside of the passage, the crown's outer curve, the jambs' outer
 *     sides): the archway itself ({@link Region#BODY}), restyled by planks, masonry or dye.</li>
 * </ul>
 * A held Universal Trim fits the face nearer the hit ({@link ArchwayTrimGeometry#face}) wherever it
 * lands.
 */
public final class ArchwayTrimTarget {
    /** How far a hit point is nudged into the surface it hit. */
    private static final double INWARD = 1.0E-4;

    /** The part of the archway a hit lands on. */
    public enum Region {
        FACE, BODY
    }

    /** The decided target: the region and the face nearer the hit. */
    public record Hit(Region region, AttachmentFace face) {
    }

    private ArchwayTrimTarget() {
    }

    public static Hit of(BlockState state, BlockPos pos, BlockHitResult hit) {
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(pos, state);
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Vec3 local = assembly.toAuthored(hit.getLocation().subtract(normal.scale(INWARD)));
        AttachmentFace face = AttachmentFace.at(local.z, ArchwayTrimGeometry.MIDDLE);
        Direction side = hit.getDirection();
        boolean planar = side == assembly.facing() || side == assembly.facing().getOpposite();
        return new Hit(planar ? Region.FACE : Region.BODY, face);
    }
}
