package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;

/** Whole-leaf acquisition, including rays which never traverse an occupied door cell. */
public final class DoorRaycast {
    // The largest leaf is two blocks wide; BACK adds 13/16 to its forward projection.
    private static final int PROJECTION = 3;
    private DoorRaycast() {}

    public static BlockHitResult clip(Level level, Player player, Vec3 eye, Vec3 end) {
        if(io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareDonors.held(player)) {
            var visible=clip(level,player,eye,end,false);
            if(visible.getType()==HitResult.Type.BLOCK) {
                var state=level.getBlockState(visible.getBlockPos());
                if(state.getBlock() instanceof AssemblyDoorBlock && DoorHitResolver.onHardware(DoorLayout.of(visible.getBlockPos(),state),state.getValue(DoorBlock.OPEN),visible))return visible;
            }
        }
        return clip(level,player,eye,end,true);
    }

    private static BlockHitResult clip(Level level,Player player,Vec3 eye,Vec3 end,boolean emptyCorners) {
        // A whole-assembly outline can otherwise end traversal before a nearer neighbour is visited.
        var nearest = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player) {
            @Override public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
                var shape = state.getBlock() instanceof AssemblyDoorBlock
                        ? DoorSelectionShapes.target(state,world,pos,CollisionContext.of(player),emptyCorners) : super.getBlockShape(state, world, pos);
                return state.getBlock() instanceof AssemblyDoorBlock ? DoorTargetShapes.cell(shape) : shape;
            }
        });
        double distance = nearest.getType() == HitResult.Type.MISS ? eye.distanceToSqr(end)
                : eye.distanceToSqr(nearest.getLocation());
        var visited = new HashSet<BlockPos>();
        boolean hardware = io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareDonors.held(player);
        var min = BlockPos.containing(Math.min(eye.x, end.x) - PROJECTION, Math.min(eye.y, end.y), Math.min(eye.z, end.z) - PROJECTION);
        var max = BlockPos.containing(Math.max(eye.x, end.x) + PROJECTION, Math.max(eye.y, end.y), Math.max(eye.z, end.z) + PROJECTION);
        for (var pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) continue;
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof AssemblyDoorBlock block) || !state.getValue(DoorBlock.OPEN) && !hardware) continue;
            var door = DoorLayout.of(pos, state);
            if (!visited.add(door.anchor()) || !door.loaded(level) || !door.complete(level, block)) continue;
            // betweenClosed reuses a mutable cursor; BlockHitResult retains its supplied position.
            var hit = DoorSelectionShapes.target(state, level, pos, CollisionContext.of(player),emptyCorners).clip(eye, end, pos.immutable());
            if (hit == null || eye.distanceToSqr(hit.getLocation()) > distance + 1e-8) continue;
            var resolved = DoorHitResolver.resolve(level, hit);
            if (resolved.isEmpty()) continue;
            nearest = resolved.get();
            distance = eye.distanceToSqr(nearest.getLocation());
        }
        return DoorHitResolver.resolve(level, nearest).orElse(nearest);
    }
}
