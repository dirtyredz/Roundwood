package io.github.dirtyredz.roundwood.block.trim.assembly;

import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyTeardown;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Removing one quarter removes the frame: the removal that started it keeps its own loot and
 * {@link AssemblyTeardown} clears the loaded partners silently, so at most one frame item drops. A
 * door the frame was fitted to is a different assembly and is never touched (nor the reverse).
 */
public final class TrimRemoval {
    private TrimRemoval() {
    }

    /** Called from {@code onRemove} of the quarter at {@code pos}, whose new state is already in the world. */
    public static void onRemove(RoundTrimBlock block, BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (level.isClientSide || newState.is(block) || level.restoringBlockSnapshots
                || AssemblyMutationBatch.isMutating(level, pos)) {
            return; // a state change, a cancelled placement being rolled back, or our own batch
        }
        TrimLayout.Assembly assembly = TrimLayout.of(pos, state);
        List<BlockPos> partners = new ArrayList<>(3);
        for (TrimPart part : assembly.presentParts(level, block)) {
            BlockPos partPos = assembly.pos(part);
            if (!partPos.equals(pos)) {
                partners.add(partPos);
            }
        }
        AssemblyTeardown.clear(level, assembly.positions(), partners, block);
    }
}
