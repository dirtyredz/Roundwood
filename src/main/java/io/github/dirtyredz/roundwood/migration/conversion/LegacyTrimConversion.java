package io.github.dirtyredz.roundwood.migration.conversion;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.migration.block.LegacyTrimBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts one 0.4.0 trim to the canonical trim as one guarded operation, the door's way
 * ({@link LegacyDoorConversion}) without camouflage: whole frame read first (legacy or canonical in
 * exactly their slots), legacy quarters replaced silently with facing and part kept, the anchor
 * given the historical (or already stored) appearance, then neighbours told. Fitted and freestanding
 * frames are the same blocks; the door a frame is fitted to is a different object and untouched.
 */
public final class LegacyTrimConversion {
    private LegacyTrimConversion() {
    }

    public static LegacyCells.Result convert(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof LegacyTrimBlock legacy)) {
            return LegacyCells.Result.NOT_LEGACY;
        }
        RoundTrimBlock canonical = legacy.canonical();
        TrimLayout.Assembly assembly = TrimLayout.of(pos, state);
        if (!assembly.loaded(level)) {
            return LegacyCells.Result.DEFERRED;
        }
        Map<Quarter, BlockState> cells = new EnumMap<>(Quarter.class);
        for (Quarter part : Quarter.values()) {
            BlockState cell = level.getBlockState(assembly.pos(part));
            if (!assembly.isPart(cell, legacy, part) && !assembly.isPart(cell, canonical, part)) {
                return LegacyCells.Result.INCOMPLETE;
            }
            cells.put(part, cell);
        }
        BlockPos anchor = assembly.anchor();
        Appearance appearance = level.getBlockEntity(anchor) instanceof RoundTrimBlockEntity entity
                ? entity.appearance() : legacy.defaultAppearance();
        boolean anchorLegacy = cells.get(Quarter.LOWER_LEFT).is(legacy);

        Map<BlockPos, BlockState> writes = new LinkedHashMap<>();
        cells.forEach((part, cell) -> {
            if (cell.is(legacy)) {
                writes.put(assembly.pos(part), LegacyCells.copyProperties(cell, canonical.defaultBlockState()));
            }
        });
        if (!LegacyCells.replace(level, assembly.positions(), writes, canonical)) {
            return LegacyCells.Result.DEFERRED;
        }
        if (anchorLegacy && level.getBlockEntity(anchor) instanceof RoundTrimBlockEntity converted) {
            converted.setAppearance(appearance);
        }
        return LegacyCells.Result.CONVERTED;
    }
}
