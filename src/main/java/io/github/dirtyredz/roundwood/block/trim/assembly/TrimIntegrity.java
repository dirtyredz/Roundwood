package io.github.dirtyredz.roundwood.block.trim.assembly;

import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps half-built frames out of the world, like the door's integrity check: a quarter that
 * appears from anything but an in-place state change is checked one tick later. If all four cells
 * are loaded and the frame is incomplete, the quarter breaks with its own loot (partners cleared by
 * {@link TrimRemoval}). Missing quarters are never made; an unloaded cell defers the check.
 */
public final class TrimIntegrity {
    static final int CHECK_DELAY = 1;
    static final int RETRY_DELAY = 20;

    private TrimIntegrity() {
    }

    public static void scheduleCheck(RoundTrimBlock block, BlockState oldState, Level level, BlockPos pos) {
        if (level.isClientSide || oldState.is(block) || level.restoringBlockSnapshots) {
            return;
        }
        level.scheduleTick(pos, block, CHECK_DELAY);
    }

    /** The scheduled check. Returns true when the frame is whole (or the check was deferred). */
    public static boolean check(RoundTrimBlock block, BlockState state, ServerLevel level, BlockPos pos) {
        TrimLayout.Assembly assembly = TrimLayout.of(pos, state);
        if (!assembly.loaded(level)) {
            level.scheduleTick(pos, block, RETRY_DELAY);
            return true;
        }
        if (assembly.complete(level, block)) {
            return true;
        }
        level.destroyBlock(pos, true);
        return false;
    }
}
