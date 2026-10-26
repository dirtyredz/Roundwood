package io.github.dirtyredz.roundwood.migration.conversion;

import com.mojang.logging.LogUtils;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.migration.block.LegacyDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts one 0.4.0 door to the canonical door, as one guarded operation: every cell of the
 * assembly is read first (each must be this legacy block or the canonical door in exactly its slot;
 * a partly converted door is finished, idempotently), the anchor's appearance and camouflage
 * (refund included) are taken, the legacy cells are replaced silently with every state property
 * kept (facing, hinge, column, half, open, powered, injected ones), then the anchor gets its
 * appearance and camouflage back and the neighbours are told. Nothing drops, nothing is refunded,
 * no sound plays. An unloaded cell defers; a loaded but incomplete door is left to integrity cleanup.
 */
public final class LegacyDoorConversion {
    private static final Logger LOGGER = LogUtils.getLogger();

    private LegacyDoorConversion() {
    }

    public static LegacyCells.Result convert(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof LegacyDoorBlock legacy)) {
            return LegacyCells.Result.NOT_LEGACY;
        }
        RoundDoorBlock canonical = legacy.canonical();
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level)) {
            return LegacyCells.Result.DEFERRED;
        }
        Map<DoorLayout.Part, BlockState> cells = new EnumMap<>(DoorLayout.Part.class);
        for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
            BlockState cell = level.getBlockState(assembly.pos(part));
            if (!assembly.isPart(cell, legacy, part) && !assembly.isPart(cell, canonical, part)) {
                return LegacyCells.Result.INCOMPLETE;
            }
            cells.put(part, cell);
        }
        BlockPos anchor = assembly.anchor();
        Optional<RoundDoorBlockEntity> anchorEntity = level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity
                ? Optional.of(entity) : Optional.empty();
        Appearance appearance = anchorEntity.map(RoundDoorBlockEntity::appearance).orElse(legacy.defaultAppearance());
        if (!appearance.equals(legacy.defaultAppearance()) && cells.get(DoorLayout.Part.LOWER_LEFT).is(canonical)) {
            LOGGER.warn("Roundwood: legacy door parts at {} disagree with their converted anchor ({} vs {}); keeping the anchor's",
                    anchor, legacy.defaultAppearance().key(), appearance.key());
        }
        boolean anchorLegacy = cells.get(DoorLayout.Part.LOWER_LEFT).is(legacy);
        Optional<CamouflageData> camouflage = anchorLegacy ? anchorEntity.flatMap(RoundDoorBlockEntity::takeCamouflage) : Optional.empty();

        Map<BlockPos, BlockState> writes = new LinkedHashMap<>();
        cells.forEach((part, cell) -> {
            if (cell.is(legacy)) {
                writes.put(assembly.pos(part), LegacyCells.copyProperties(cell, canonical.defaultBlockState()));
            }
        });
        if (!LegacyCells.replace(level, assembly.positions(), writes, canonical)) {
            camouflage.ifPresent(data -> anchorEntity.get().setCamouflage(data));
            return LegacyCells.Result.DEFERRED;
        }
        if (anchorLegacy && level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity converted) {
            converted.setAppearance(appearance);
            camouflage.ifPresent(converted::setCamouflage);
        }
        return LegacyCells.Result.CONVERTED;
    }
}
