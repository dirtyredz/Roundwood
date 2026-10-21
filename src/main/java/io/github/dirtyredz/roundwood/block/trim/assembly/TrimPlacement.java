package io.github.dirtyredz.roundwood.block.trim.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Writes a whole compatibility frame or nothing (0.8.0: frames are no longer placed by players; the
 * {@code round_trim} item attaches to doors instead). Saved frames keep their lifecycle; this remains
 * for structure fixtures, the showcase's retained legacy exhibit and tests. Every cell is checked
 * first: inside the world, loaded, replaceable and dry, and both lower quarters need a sturdy floor.
 * The look goes on the new anchor inside the same write.
 */
public final class TrimPlacement {
    private TrimPlacement() {
    }

    /**
     * Places a complete frame without a player or item (the showcase builder, other commands): the
     * same bounds, loaded, replaceable, dry and floor checks, then the same single write per cell.
     * Returns false, writing nothing, if any check fails.
     */
    public static boolean placeAt(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly) {
        return placeAt(level, block, assembly, block.defaultAppearance());
    }

    /** As {@link #placeAt(Level, RoundTrimBlock, TrimLayout.Assembly)}, in {@code appearance}. */
    public static boolean placeAt(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly, Appearance appearance) {
        for (BlockPos pos : assembly.positions()) {
            if (!inWorld(level, pos) || !level.getBlockState(pos).canBeReplaced()) {
                return false;
            }
        }
        Optional<Map<TrimPart, BlockState>> states = states(level, block, assembly);
        states.ifPresent(plan -> install(level, assembly, plan, appearance));
        return states.isPresent();
    }

    private static boolean inWorld(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.isLoaded(pos);
    }

    /** The states to write, if every cell is dry and both lower quarters stand on a sturdy floor. */
    private static Optional<Map<TrimPart, BlockState>> states(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly) {
        Map<TrimPart, BlockState> states = new EnumMap<>(TrimPart.class);
        for (TrimPart part : TrimPart.values()) {
            BlockPos pos = assembly.pos(part);
            BlockState state = assembly.stateFor(part, block);
            if (!level.getFluidState(pos).isEmpty() || !state.canSurvive(level, pos)) {
                return Optional.empty(); // frames are never waterlogged; the lower row needs a floor
            }
            states.put(part, state);
        }
        return Optional.of(states);
    }

    private static void install(Level level, TrimLayout.Assembly assembly, Map<TrimPart, BlockState> states,
                                Appearance appearance) {
        AssemblyMutationBatch.run(level, assembly.positions(), () -> {
            for (TrimPart part : TrimPart.values()) {
                level.setBlock(assembly.pos(part), states.get(part), Block.UPDATE_ALL);
            }
        });
        if (level.getBlockEntity(assembly.anchor()) instanceof RoundTrimBlockEntity entity) {
            entity.setAppearance(appearance);
        }
    }
}
