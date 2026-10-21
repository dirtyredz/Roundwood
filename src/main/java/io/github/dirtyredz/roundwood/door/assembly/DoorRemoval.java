package io.github.dirtyredz.roundwood.door.assembly;

import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.door.camouflage.CamouflageDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Removing one part removes the assembly. The removal that started it (a player's break, an
 * explosion, lost support) keeps its normal loot; the partners are cleared silently, with drops
 * suppressed, before anything else is notified. That ordering is what makes exactly one item drop:
 * no door loot is spawned here. Stored camouflage is released first, while the anchor still exists
 * (see {@link CamouflageDrops}).
 */
public final class DoorRemoval {
    /** Clients only, no shape cascade (it would re-break partners with drops), drops suppressed. */
    private static final int CLEAR_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int LEVEL_EVENT_BREAK = 2001;

    private DoorRemoval() {
    }

    /** Called from {@code onRemove} of the part at {@code pos}, whose new state is already in the world. */
    public static void onRemove(RoundDoorBlock block, BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (level.isClientSide || newState.is(block) || level.restoringBlockSnapshots
                || DoorMutationBatch.isMutating(level, pos)) {
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
        if (partners.isEmpty()) {
            return;
        }
        List<BlockState> removed = new ArrayList<>(partners.size());
        DoorMutationBatch.run(level, assembly.positions(), () -> {
            for (BlockPos partner : partners) {
                BlockState old = level.getBlockState(partner);
                removed.add(old);
                level.setBlock(partner, level.getFluidState(partner).createLegacyBlock(), CLEAR_FLAGS);
            }
        });
        // Now that the whole assembly is gone, tell the neighbours (redstone, attached blocks, panes).
        for (int i = 0; i < removed.size(); i++) {
            BlockPos partner = partners.get(i);
            level.levelEvent(LEVEL_EVENT_BREAK, partner, Block.getId(removed.get(i)));
            level.updateNeighborsAt(partner, block);
            level.getBlockState(partner).updateNeighbourShapes(level, partner, Block.UPDATE_CLIENTS, Block.UPDATE_LIMIT - 1);
        }
    }
}
