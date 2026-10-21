package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyTeardown;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Removing one part removes the assembly. The removal that started it (a player's break, an
 * explosion, lost support) keeps its normal loot; {@link AssemblyTeardown} clears the partners
 * silently, with drops suppressed, before anything else is notified. That ordering is what makes
 * exactly one item drop: no door loot is spawned here. Stored camouflage is released first, while
 * the anchor still exists (see {@link CamouflageDrops}).
 */
public final class DoorRemoval {
    private DoorRemoval() {
    }

    /** Called from {@code onRemove} of the part at {@code pos}, whose new state is already in the world. */
    public static void onRemove(RoundDoorBlock block, BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (level.isClientSide || newState.is(block) || level.restoringBlockSnapshots
                || AssemblyMutationBatch.isMutating(level, pos)) {
            // Same block (a state change), a cancelled placement being rolled back, or our own batch.
            return;
        }
        CamouflageDrops.releaseOnTeardown(block, state, level, pos);
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        List<BlockPos> partners = new ArrayList<>(3);
        for (DoorLayout.Part part : assembly.presentParts(level, block)) {
            BlockPos partPos = assembly.pos(part);
            if (!partPos.equals(pos)) {
                partners.add(partPos);
            }
        }
        AssemblyTeardown.clear(level, assembly.positions(), partners, block);
    }
}
