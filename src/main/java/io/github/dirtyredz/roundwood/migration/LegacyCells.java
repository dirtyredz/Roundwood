package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The write discipline shared by the three conversions: every cell is replaced silently inside one
 * {@link AssemblyMutationBatch} (so teardown, camouflage refunds and shape or integrity reactions
 * stand down), with no neighbour or shape cascade, no drops and no sounds; only once the whole
 * object is canonical are its neighbours told. States keep every property the canonical block also
 * has, including ones other mods inject (Dramatic Doors' {@code waterlogged}).
 */
final class LegacyCells {
    /** Clients only, no shape cascade, drops suppressed: nothing reacts to the half-converted object. */
    private static final int SILENT = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /** What one attempt at converting an object did. */
    enum Result {
        /** Now canonical. */
        CONVERTED,
        /** A cell is unloaded: try again later, never loading it. */
        DEFERRED,
        /** Loaded but not a whole object: ordinary cleanup applies. */
        INCOMPLETE,
        /** Nothing legacy was found there. */
        NOT_LEGACY
    }

    private LegacyCells() {
    }

    /** {@code target} with every property of {@code source} it also has. */
    static BlockState copyProperties(BlockState source, BlockState target) {
        BlockState out = target;
        for (Property<?> property : source.getProperties()) {
            if (out.hasProperty(property)) {
                out = copy(source, out, property);
            }
        }
        return out;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState source, BlockState target, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }

    /**
     * Writes {@code writes} silently, as one batch over {@code cells}, then notifies the neighbours of
     * each written cell. False (nothing written) if the batch was refused for nesting depth.
     */
    static boolean replace(Level level, Collection<BlockPos> cells, Map<BlockPos, BlockState> writes, Block source) {
        List<BlockPos> written = List.copyOf(writes.keySet());
        boolean ran = AssemblyMutationBatch.run(level, cells, () -> {
            for (BlockPos pos : written) {
                level.setBlock(pos, writes.get(pos), SILENT);
            }
        });
        if (!ran) {
            return false;
        }
        for (BlockPos pos : written) {
            level.updateNeighborsAt(pos, source);
            level.getBlockState(pos).updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS, Block.UPDATE_LIMIT - 1);
        }
        return true;
    }
}
