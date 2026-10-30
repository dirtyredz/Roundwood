package io.github.dirtyredz.roundwood.block.door.interaction;

import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative use of a leaf outside its occupied cells, preserving the real hit point. */
public final class ProjectedDoorUse {
    private ProjectedDoorUse() {
    }

    /** Re-raycast before dispatching through the ordinary NeoForge/vanilla interaction router. */
    public static InteractionResult use(ServerPlayer player, BlockHitResult requested) {
        var level = player.serverLevel();
        var pos = requested.getBlockPos();
        if (!player.isAlive() || player.isSpectator() || player.isUsingItem()
                || !level.isLoaded(pos) || !Reach.within(player, requested.getLocation())) {
            return InteractionResult.FAIL;
        }
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AssemblyDoorBlock door)) {
            return InteractionResult.FAIL;
        }
        var assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, door)
                || !assembly.mayEditAll(level, player) || !level.getWorldBorder().isWithinBounds(pos)
                || !DoorHitResolver.projected(level, requested)) {
            return InteractionResult.FAIL;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = requested.getLocation().add(requested.getLocation().subtract(eye).normalize().scale(0.01));
        // Whole-door outlines can return a point beyond the cell currently traversed by Level.clip.
        // A second ray against clipped collision catches a solid neighbour before that projection.
        BlockHitResult obstruction = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (obstruction.getType() == HitResult.Type.BLOCK
                && obstruction.getLocation().distanceToSqr(eye) + 1e-6 < requested.getLocation().distanceToSqr(eye)) {
            return InteractionResult.FAIL;
        }
        BlockHitResult ray = io.github.dirtyredz.roundwood.block.door.selection.DoorRaycast.clip(level, player, eye, end);
        // The face of a projected point is an artefact of which part box the ray crossed (it differs
        // between the client's full-reach ray and this short one), so only the point is compared and
        // the server's own face is used for dispatch.
        var resolved = DoorHitResolver.resolve(level, ray);
        if (ray.getType() != HitResult.Type.BLOCK || resolved.isEmpty()
                || !resolved.get().getBlockPos().equals(pos)
                || resolved.get().getLocation().distanceToSqr(requested.getLocation()) > 1e-6) {
            return InteractionResult.FAIL;
        }
        player.resetLastActionTime();
        // Canceling the client input ends its hand loop; preserve main-hand/offhand precedence here.
        for (InteractionHand hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (!stack.isItemEnabled(level.enabledFeatures())) {
                return InteractionResult.FAIL;
            }
            InteractionResult result = player.gameMode.useItemOn(player, level, stack, hand, resolved.get());
            if (result.consumesAction()) {
                player.swing(hand, true);
                return result;
            }
            if (result == InteractionResult.FAIL) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }
}
