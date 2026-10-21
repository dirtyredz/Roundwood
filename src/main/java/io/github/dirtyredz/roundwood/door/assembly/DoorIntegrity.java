package io.github.dirtyredz.roundwood.door.assembly;

import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps half-built assemblies out of the world. A part that appears from anything other than an
 * in-place state change (our own placement, a structure template, a command) is checked one tick
 * later, after whatever placed it has finished writing the rest. If all four cells are loaded and
 * the assembly is incomplete, the part is broken through the normal path: it keeps its own loot and
 * {@link DoorRemoval} clears its matching partners silently, so at most one door drops. Missing
 * parts are never created, other assemblies are never absorbed, and an unloaded cell defers the
 * check instead of being read or loaded.
 */
public final class DoorIntegrity {
    /** One tick: a structure or our own placement writes all four cells within the same tick. */
    static final int CHECK_DELAY = 1;
    /** While a partner's chunk is unloaded, look again about once a second. */
    static final int RETRY_DELAY = 20;

    private DoorIntegrity() {
    }

    /** From {@code onPlace}: schedule a check unless this is a state change of an existing part. */
    public static void scheduleCheck(RoundDoorBlock block, BlockState oldState, Level level, BlockPos pos) {
        if (level.isClientSide || oldState.is(block) || level.restoringBlockSnapshots) {
            return;
        }
        level.scheduleTick(pos, block, CHECK_DELAY);
    }

    /** The scheduled check. Returns true when the assembly is whole (or the check was deferred). */
    public static boolean check(RoundDoorBlock block, BlockState state, ServerLevel level, BlockPos pos) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
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
