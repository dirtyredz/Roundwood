package io.github.dirtyredz.roundwood.block.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps half-built assemblies out of the world, for every family (doors, archways, frames). A part
 * that appears from anything other than an in-place state change (our own placement, a structure
 * template, a command) is checked one tick later, after whatever placed it has finished writing the
 * rest. If all its cells are loaded and the assembly is incomplete, the part is broken through the
 * normal path: it keeps its own loot and {@link AssemblyRemoval} clears its matching partners
 * silently, so at most one item drops. Missing parts are never created, other assemblies are never
 * absorbed, and an unloaded cell defers the check instead of being read or loaded.
 */
public final class AssemblyIntegrity {
    /** One tick: a structure or our own placement writes every cell within the same tick. */
    static final int CHECK_DELAY = 1;
    /** While a partner's chunk is unloaded, look again about once a second. */
    static final int RETRY_DELAY = 20;

    private AssemblyIntegrity() {
    }

    /** From {@code onPlace}: schedule a check unless this is a state change of an existing part or a snapshot restore. */
    public static void scheduleCheck(Block block, BlockState oldState, Level level, BlockPos pos) {
        if (level.isClientSide || oldState.is(block) || level.restoringBlockSnapshots) {
            return;
        }
        level.scheduleTick(pos, block, CHECK_DELAY);
    }

    /** The scheduled check of {@code assembly}'s part at {@code pos}. Returns true when it is whole (or the check was deferred). */
    public static boolean check(Block block, Assembly<?> assembly, ServerLevel level, BlockPos pos) {
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
