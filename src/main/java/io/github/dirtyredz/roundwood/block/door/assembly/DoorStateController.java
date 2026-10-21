package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Opening and redstone for a whole assembly. Every request is applied to all present parts at once,
 * server-side, with one sound and one game event per real transition. Explicit open/close requests
 * are idempotent; only a player's click computes a toggle. Another mod writing {@code open} straight
 * into one part is followed by the partners, silently (that mod owns its own sound).
 */
public final class DoorStateController {
    /** Clients + immediate re-render, no neighbour or shape cascade: the batch writes every part. */
    private static final int STATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE;

    private DoorStateController() {
    }

    /** A player's click on any part. */
    public static InteractionResult use(RoundDoorBlock block, BlockState state, Level level, BlockPos pos, @Nullable Entity player) {
        if (!block.type().canOpenByHand()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            // The server rewrites all four parts and plays the sound for everyone, this player included.
            return InteractionResult.SUCCESS;
        }
        apply(block, level, pos, state, !state.getValue(DoorBlock.OPEN), null, player, true);
        return InteractionResult.CONSUME;
    }

    /** {@link DoorBlock#setOpen} from villagers, mobs and other mods: open or close, never toggle. */
    public static void setOpen(RoundDoorBlock block, BlockState state, Level level, BlockPos pos, boolean open, @Nullable Entity entity) {
        if (level.isClientSide || !state.is(block)) {
            return;
        }
        apply(block, level, pos, state, open, null, entity, true);
    }

    /** Redstone: the assembly is powered when any of its four cells receives a signal. */
    public static void neighborChanged(RoundDoorBlock block, BlockState state, Level level, BlockPos pos, Block source) {
        if (level.isClientSide || source instanceof RoundDoorBlock || AssemblyMutationBatch.isMutating(level, pos)) {
            return;
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level)) {
            return; // defer: an unloaded partner is not air; the next update after it loads decides
        }
        boolean powered = false;
        for (BlockPos part : assembly.positions()) {
            powered |= level.hasNeighborSignal(part);
        }
        if (powered != state.getValue(DoorBlock.POWERED)) {
            // A power transition opens or closes; between transitions players keep manual control.
            apply(block, level, pos, state, powered, powered, null, true);
        }
    }

    /**
     * Called from {@code onPlace} of a part whose state just changed in place. If something other
     * than Roundwood wrote a new {@code open} value into this one part (Double Doors, a command, a
     * structure tool), the partners follow without a second sound. Our own batches, rollbacks of
     * cancelled placements and changes of membership (facing, hinge, column, half) are ignored.
     */
    public static void reconcileExternalOpen(RoundDoorBlock block, BlockState oldState, BlockState newState, Level level, BlockPos pos) {
        if (level.isClientSide || !oldState.is(block) || !newState.is(block)
                || level.restoringBlockSnapshots || AssemblyMutationBatch.isMutating(level, pos)) {
            return;
        }
        boolean open = newState.getValue(DoorBlock.OPEN);
        if (open == oldState.getValue(DoorBlock.OPEN) || !DoorLayout.partOf(oldState).equals(DoorLayout.partOf(newState))
                || !DoorLayout.of(pos, oldState).equals(DoorLayout.of(pos, newState))) {
            return;
        }
        apply(block, level, pos, newState, open, null, null, false);
    }

    /**
     * Writes OPEN and/or POWERED to every present part. Returns true if anything changed. Does
     * nothing while any cell is unloaded. {@code announce} plays the sound and game event when the
     * open state changes.
     */
    static boolean apply(RoundDoorBlock block, Level level, BlockPos pos, BlockState state,
                         @Nullable Boolean open, @Nullable Boolean powered, @Nullable Entity entity, boolean announce) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level)) {
            return false;
        }
        List<BlockPos> targets = new ArrayList<>(4);
        List<BlockState> updates = new ArrayList<>(4);
        boolean openChanged = false;
        for (DoorLayout.Part part : assembly.presentParts(level, block)) {
            BlockPos partPos = assembly.pos(part);
            BlockState current = level.getBlockState(partPos);
            BlockState next = current;
            if (open != null) {
                next = next.setValue(DoorBlock.OPEN, open);
            }
            if (powered != null) {
                next = next.setValue(DoorBlock.POWERED, powered);
            }
            if (next != current) {
                openChanged |= next.getValue(DoorBlock.OPEN) != current.getValue(DoorBlock.OPEN);
                targets.add(partPos);
                updates.add(next);
            }
        }
        if (targets.isEmpty()) {
            return false;
        }
        AssemblyMutationBatch.run(level, assembly.positions(), () -> {
            for (int i = 0; i < targets.size(); i++) {
                level.setBlock(targets.get(i), updates.get(i), STATE_FLAGS);
            }
        });
        if (openChanged && announce) {
            boolean nowOpen = updates.get(0).getValue(DoorBlock.OPEN);
            BlockPos anchor = assembly.anchor();
            BlockPos centre = assembly.pos(DoorLayout.Part.UPPER_RIGHT);
            level.playSound(null,
                    (anchor.getX() + centre.getX() + 1) / 2.0, anchor.getY() + 1.0, (anchor.getZ() + centre.getZ() + 1) / 2.0,
                    nowOpen ? block.type().doorOpen() : block.type().doorClose(), SoundSource.BLOCKS,
                    1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.gameEvent(entity, nowOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, anchor);
        }
        return true;
    }
}
