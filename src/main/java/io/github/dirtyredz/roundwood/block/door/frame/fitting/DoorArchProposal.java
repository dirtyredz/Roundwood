package io.github.dirtyredz.roundwood.block.door.frame.fitting;

import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchStore;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A complete, read-only fitted-door proposal: the fit, the door block, the arch custody, every
 * door cell's next state and the anchor's full NBT. Both click orders
 * ({@link DoorIntoArchProposal}, {@link ArchOntoDoorProposal}) end in {@link #validated}, which owns
 * the shared world, permission and obstruction checks.
 */
public record DoorArchProposal(DoorArchFitPlan fit, AssemblyDoorBlock block, DoorArchAttachment attachment,
                               Map<BlockPos, BlockState> states, CompoundTag anchorData, boolean intoArch) {

    /** Checks every door cell for {@code context}'s player and returns the proposal, or empty when any is refused. */
    static Optional<DoorArchProposal> validated(UseOnContext context, DoorArchFitPlan fit, AssemblyDoorBlock block,
                                                DoorArchAttachment attachment, CompoundTag data,
                                                boolean open, boolean powered, boolean intoArch) {
        var level = context.getLevel(); var player = context.getPlayer();
        var cells = fit.door().positions();
        for (var pos : cells) if (!AssemblyPlacementPlan.inWorld(level, pos)) return Optional.empty();
        var states = new LinkedHashMap<BlockPos, BlockState>();
        var collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (var part : fit.door().parts()) {
            var pos = fit.door().pos(part); var previous = level.getBlockState(pos);
            if (player != null && (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) return Optional.empty();
            if (intoArch && !fit.arch().positions().contains(pos) && !previous.canBeReplaced()) return Optional.empty();
            var next = fit.door().stateFor(part, block, open, powered);
            if (!level.isUnobstructed(next, pos, collision) || part.row() == 0 && !next.canSurvive(level, pos)) return Optional.empty();
            states.put(pos, next);
        }
        data.put(DoorArchStore.TAG, DoorArchStore.encode(attachment));
        return Optional.of(new DoorArchProposal(fit, block, attachment, Collections.unmodifiableMap(states), data, intoArch));
    }
}
