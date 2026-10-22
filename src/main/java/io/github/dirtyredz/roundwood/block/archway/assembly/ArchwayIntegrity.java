package io.github.dirtyredz.roundwood.block.archway.assembly;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps half-built archways out of the world (0.12.0, the frame's and door's rule): a cell that appears
 * from anything but an in-place state change is checked one tick later. If all four cells are loaded
 * and the archway is incomplete, the cell breaks with its own loot (partners cleared by
 * {@link ArchwayRemoval}). Missing cells are never made; an unloaded cell defers the check.
 */
public final class ArchwayIntegrity {
    static final int CHECK_DELAY = 1;
    static final int RETRY_DELAY = 20;

    private ArchwayIntegrity() {
    }

    public static void scheduleCheck(ArchwayBlock block, BlockState oldState, Level level, BlockPos pos) {
        if (level.isClientSide || oldState.is(block) || level.restoringBlockSnapshots) {
            return;
        }
        level.scheduleTick(pos, block, CHECK_DELAY);
    }

    /** The scheduled check. Returns true when the archway is whole (or the check was deferred). */
    public static boolean check(ArchwayBlock block, BlockState state, ServerLevel level, BlockPos pos) {
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(pos, state);
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
