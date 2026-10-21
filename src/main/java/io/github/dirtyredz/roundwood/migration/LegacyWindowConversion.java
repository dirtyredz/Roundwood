package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.migration.block.LegacyWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts one 0.4.0 window to the canonical window: the same state (axis, position, the four
 * connections, waterlogged so the water stays, and a fitted trim), written silently, then its
 * appearance and any trim look on the new block entity and its neighbours told.
 */
public final class LegacyWindowConversion {
    private LegacyWindowConversion() {
    }

    static LegacyCells.Result convert(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof LegacyWindowBlock legacy)) {
            return LegacyCells.Result.NOT_LEGACY;
        }
        RoundWindowBlock canonical = legacy.canonical();
        RoundWindowBlockEntity old = level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity ? entity : null;
        Appearance appearance = old != null ? old.appearance() : legacy.defaultAppearance();
        Optional<Appearance> trim = old != null ? old.trim() : Optional.empty();
        BlockState next = LegacyCells.copyProperties(state, canonical.defaultBlockState());
        if (!LegacyCells.replace(level, List.of(pos), Map.of(pos, next), canonical)) {
            return LegacyCells.Result.DEFERRED;
        }
        if (level.getBlockEntity(pos) instanceof RoundWindowBlockEntity converted) {
            converted.setAppearance(appearance);
            trim.ifPresent(converted::setTrim);
        }
        return LegacyCells.Result.CONVERTED;
    }
}
