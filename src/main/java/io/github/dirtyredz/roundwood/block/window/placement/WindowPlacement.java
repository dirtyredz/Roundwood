package io.github.dirtyredz.roundwood.block.window.placement;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Where a window being placed goes and how it sits, from an ordinary placement context: the server
 * decides from the same use context the client previews with, so no zone is ever sent. The
 * destination cell is vanilla's (a replaceable clicked block is replaced); the zone is read on the
 * originally clicked cell's face ({@link WindowTargetZones}). The centre gives the upright pane facing
 * the player; an edge gives a pane flush against that side of the destination cell. Sneaking changes
 * nothing.
 */
public final class WindowPlacement {
    private static final double TIE = 1e-6;

    private WindowPlacement() {
    }

    /**
     * Everything placement needs from a context.
     *
     * @param destination the cell the window goes in
     * @param clicked     the cell whose face was hit (the destination itself when it was replaceable)
     * @param zone        the targeted zone of that face
     * @param facing      the player's horizontal facing (north without a player)
     * @param eye         the player's eye relative to the destination's lower corner, or null
     */
    public record Request(BlockPos destination, BlockPos clicked, Direction face, WindowTargetZones.Zone zone,
                          Direction facing, @Nullable Vec3 eye) {
        /** The targeted edge's world direction, empty for the centre. */
        public Optional<Direction> edge() {
            return WindowTargetZones.edge(face, zone);
        }
    }

    public static Request of(BlockPlaceContext context) {
        BlockPos destination = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos clicked = context.replacingClickedOnBlock() ? destination : destination.relative(face.getOpposite());
        Vec3 hit = context.getClickLocation();
        WindowTargetZones.Zone zone = WindowTargetZones.classify(WindowTargetZones.facePoint(face,
                hit.x - clicked.getX(), hit.y - clicked.getY(), hit.z - clicked.getZ()));
        Player player = context.getPlayer();
        Vec3 eye = player == null ? null : player.getEyePosition().subtract(Vec3.atLowerCornerOf(destination));
        return new Request(destination, clicked, face, zone, context.getHorizontalDirection(), eye);
    }

    /** The round window's position: centred, or flush against the targeted side. */
    public static WindowPosition roundPosition(Request request) {
        return request.edge().map(WindowPosition::flush).orElse(WindowPosition.CENTER);
    }

    /** The centred round window's plane runs across the player's line of sight (the 0.5.0 rule). */
    public static Direction.Axis roundAxis(Request request) {
        return request.facing().getClockWise().getAxis();
    }

    public static WindowPose archPose(Request request) {
        return archPose(request.edge(), request.facing(), request.eye());
    }

    /**
     * An arch's pose. Centre: upright, front towards the player. Edge: flush against that side,
     * front towards the player's eye (on a tie or without a player: towards the player's side if the
     * plane faces them, else into the cell). A floor or ceiling arch takes the top the player sees
     * as up: away from them on a floor, towards them on a ceiling, so a left piece looks left to the
     * player who placed it.
     */
    public static WindowPose archPose(Optional<Direction> edge, Direction facing, @Nullable Vec3 eye) {
        if (edge.isEmpty()) {
            return WindowPose.upright(facing.getOpposite(), WindowDepth.CENTER);
        }
        Direction side = edge.get();
        WindowDepth depth = WindowDepth.flushAgainst(side);
        Direction.Axis axis = side.getAxis();
        double plane = (depth.slabMin() + WindowDepth.SLAB / 2) / WindowPose.SIZE;
        Direction front = towardsEye(axis, plane, eye);
        if (front == null) {
            front = facing.getAxis() == axis ? facing.getOpposite() : side.getOpposite();
        }
        if (axis.isHorizontal()) {
            return WindowPose.upright(front, depth);
        }
        return new WindowPose(front, front == Direction.UP ? facing : facing.getOpposite(), depth);
    }

    @Nullable
    private static Direction towardsEye(Direction.Axis axis, double plane, @Nullable Vec3 eye) {
        if (eye == null) {
            return null;
        }
        double at = eye.get(axis);
        if (Math.abs(at - plane) < TIE) {
            return null;
        }
        return Direction.fromAxisAndDirection(axis, at > plane ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }
}
