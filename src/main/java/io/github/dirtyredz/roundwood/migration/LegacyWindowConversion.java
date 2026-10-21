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

/**
 * Converts one 0.4.0 window to the canonical window: the same state (axis, the four connections,
 * waterlogged, so the water stays), written silently, then its historical appearance on the new
 * block entity and its neighbours told.
 */
public final class LegacyWindowConversion {
    private LegacyWindowConversion() {
    }

    static LegacyCells.Result convert(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof LegacyWindowBlock legacy)) {
            return LegacyCells.Result.NOT_LEGACY;
        }
        RoundWindowBlock canonical = legacy.canonical();
        Appearance appearance = level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity
                ? entity.appearance() : legacy.defaultAppearance();
        BlockState next = LegacyCells.copyProperties(state, canonical.defaultBlockState());
        if (!LegacyCells.replace(level, List.of(pos), Map.of(pos, next), canonical)) {
            return LegacyCells.Result.DEFERRED;
        }
        if (level.getBlockEntity(pos) instanceof RoundWindowBlockEntity converted) {
            converted.setAppearance(appearance);
        }
        return LegacyCells.Result.CONVERTED;
    }
}
