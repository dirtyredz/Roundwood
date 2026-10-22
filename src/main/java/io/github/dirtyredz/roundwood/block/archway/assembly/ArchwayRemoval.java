package io.github.dirtyredz.roundwood.block.archway.assembly;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyTeardown;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Removing one cell removes the archway (0.12.0): the removal that started it keeps its own loot (the
 * one archway item and its face trims) and {@link AssemblyTeardown} clears the loaded partners
 * silently, so the archway and each trim drop once. A neighbouring archway is a different assembly
 * and is never touched.
 */
public final class ArchwayRemoval {
    private ArchwayRemoval() {
    }

    /** Called from {@code onRemove} of the cell at {@code pos}, whose new state is already in the world. */
    public static void onRemove(ArchwayBlock block, BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (level.isClientSide || newState.is(block) || level.restoringBlockSnapshots
                || AssemblyMutationBatch.isMutating(level, pos)) {
            return; // a state change, a cancelled placement being rolled back, or our own batch
        }
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(pos, state);
        List<BlockPos> partners = new ArrayList<>(3);
        for (ArchwayPart part : assembly.presentParts(level, block)) {
            BlockPos partPos = assembly.pos(part);
            if (!partPos.equals(pos)) {
                partners.add(partPos);
            }
        }
        AssemblyTeardown.clear(level, assembly.positions(), partners, block);
    }
}
