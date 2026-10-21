package io.github.dirtyredz.roundwood.block.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Clears the remaining parts of an assembly after one part was removed, so that exactly one item
 * drops: the removal that started it keeps its own loot, and the partners vanish silently, drops
 * suppressed, inside an {@link AssemblyMutationBatch} (their own callbacks stand down). Any fluid in
 * a cleared cell is restored. Only once every partner is gone are the neighbours told (break event,
 * neighbour and shape updates), so nothing reacts to a half-removed assembly. The caller filters the
 * partners (loaded, matching, not the removed part) and does any family-specific work first.
 */
public final class AssemblyTeardown {
    /** Clients only, no shape cascade (it would re-break partners with drops), drops suppressed. */
    private static final int CLEAR_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int LEVEL_EVENT_BREAK = 2001;

    private AssemblyTeardown() {
    }

    /**
     * @param cells    every cell of the assembly (marked while clearing)
     * @param partners the loaded matching partners to clear, in order
     * @param block    the assembly's block, named as the source of the neighbour updates
     */
    public static void clear(Level level, Collection<BlockPos> cells, List<BlockPos> partners, Block block) {
        if (partners.isEmpty()) {
            return;
        }
        List<BlockState> removed = new ArrayList<>(partners.size());
        AssemblyMutationBatch.run(level, cells, () -> {
            for (BlockPos partner : partners) {
                removed.add(level.getBlockState(partner));
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
