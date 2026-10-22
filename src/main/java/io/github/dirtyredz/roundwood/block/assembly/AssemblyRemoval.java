package io.github.dirtyredz.roundwood.block.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Removing one part removes the assembly, for every family. The removal that started it (a player's
 * break, an explosion, lost support) keeps its normal loot; {@link AssemblyTeardown} clears the loaded
 * matching partners silently, with drops suppressed, before anything else is notified. That ordering
 * is what makes exactly one item drop: no loot is spawned here. A neighbouring assembly is a different
 * assembly and is never touched. A family with work to do while its parts are still all in place (the
 * door's camouflage refund) passes it as the pre-teardown hook.
 */
public final class AssemblyRemoval {
    private AssemblyRemoval() {
    }

    /**
     * Called from {@code onRemove} of the part at {@code pos}, whose new state is already in the world.
     * Stands down for a state change of the same block, a cancelled placement being rolled back, and
     * Roundwood's own batches; otherwise runs {@code beforeTeardown}, then clears the partners.
     *
     * @param assembly the removed part's assembly, derived from its old state (asked only when tearing down)
     */
    public static void onRemove(Block block, Level level, BlockPos pos, BlockState newState, Supplier<? extends Assembly<?>> assembly,
                                Runnable beforeTeardown) {
        if (level.isClientSide || newState.is(block) || level.restoringBlockSnapshots
                || AssemblyMutationBatch.isMutating(level, pos)) {
            return;
        }
        beforeTeardown.run();
        tearDown(block, level, pos, assembly.get());
    }

    /** As {@link #onRemove(Block, Level, BlockPos, BlockState, Supplier, Runnable)}, with no pre-teardown work. */
    public static void onRemove(Block block, Level level, BlockPos pos, BlockState newState, Supplier<? extends Assembly<?>> assembly) {
        onRemove(block, level, pos, newState, assembly, () -> {
        });
    }

    private static <P> void tearDown(Block block, Level level, BlockPos removed, Assembly<P> assembly) {
        List<BlockPos> partners = new ArrayList<>(assembly.parts().size());
        for (P part : assembly.presentParts(level, block)) {
            BlockPos pos = assembly.pos(part);
            if (!pos.equals(removed)) {
                partners.add(pos);
            }
        }
        AssemblyTeardown.clear(level, assembly.positions(), partners, block);
    }
}
