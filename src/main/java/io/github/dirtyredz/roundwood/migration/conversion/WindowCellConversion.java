package io.github.dirtyredz.roundwood.migration.conversion;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.trim.WindowBeamNbt;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/** Lossless single-window transfer. Preflight uses disk data; writes publish only after validation. */
public final class WindowCellConversion {
    private static final int SILENT = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private WindowCellConversion() { }

    /** Materialize the source's historical defaults before changing its identity or summary. */
    public static Optional<CompoundTag> contents(BlockPos pos, BlockState source, CompoundTag disk,
                                                 Appearance appearance, boolean cross, HolderLookup.Provider registries) {
        var normalized = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), pos, source);
        normalized.loadWithComponents(disk.copy(), registries);
        CompoundTag next = normalized.saveWithFullMetadata(registries);
        AppearanceNbt.write(next, appearance);
        if (cross) {
            var beams = WindowBeamNbt.read(next);
            if (beams.unknown() != null) return Optional.empty();
            if (!beams.trims().any()) {
                WindowBeamNbt.write(next, FaceTrims.NONE.with(AttachmentFace.FRONT, appearance));
            }
        }
        return Optional.of(next);
    }

    public static LegacyCells.Result convert(Level level, BlockPos pos, BlockState source, WindowPaneBlock destination, boolean cross) {
        if (!level.isLoaded(pos) || level.restoringBlockSnapshots || level.getBlockState(pos) != source
                || !(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity old)) return LegacyCells.Result.DEFERRED;
        var data = contents(pos, source, old.saveWithFullMetadata(level.registryAccess()), old.appearance(), cross, level.registryAccess());
        if (data.isEmpty()) return LegacyCells.Result.DEFERRED;
        BlockState next = LegacyCells.copyProperties(source, destination.defaultBlockState());
        next = summary(pos, next, data.get(), level.registryAccess());
        return install(level, pos, next, data.get()) ? LegacyCells.Result.CONVERTED : LegacyCells.Result.DEFERRED;
    }

    /** Reconcile only understood stores; unknown slots retain the historical summary. */
    public static BlockState summary(BlockPos pos, BlockState state, CompoundTag data, HolderLookup.Provider registries) {
        var probe = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), pos, state);
        probe.loadWithComponents(data, registries);
        return probe.attachmentsEditable() ? state.setValue(WindowPaneBlock.TRIMMED, probe.anyAttachment()) : state;
    }

    /** Used by migration and hidden-item placement, inside Forge's enclosing placement capture when present. */
    public static boolean install(Level level, BlockPos pos, BlockState next, CompoundTag data) {
        if (!level.isLoaded(pos) || level.restoringBlockSnapshots) return false;
        BlockState before = level.getBlockState(pos);
        BlockEntity old = level.getBlockEntity(pos);
        CompoundTag disk = old == null ? null : old.saveWithFullMetadata(level.registryAccess());
        int mark = level.capturedBlockSnapshots.size();
        boolean[] valid = {false};
        boolean ran;
        try {
            ran = AssemblyMutationBatch.run(level, List.of(pos), () -> {
                if (!level.setBlock(pos, next, level.captureBlockSnapshots ? Block.UPDATE_ALL : SILENT) || level.getBlockState(pos) != next
                        || !(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window)) return;
                window.loadWithComponents(data.copy(), level.registryAccess());
                CompoundTag saved = window.saveWithFullMetadata(level.registryAccess());
                // Metadata follows the destination; all persisted content must match the preflight.
                valid[0] = data.getAllKeys().stream().filter(key -> !List.of("id", "x", "y", "z").contains(key))
                        .allMatch(key -> data.get(key).equals(saved.get(key)));
                if (valid[0]) window.setChanged();
            });
        } catch (RuntimeException | Error failure) {
            rollback(level, pos, before, disk, mark);
            throw failure;
        }
        if (!ran || !valid[0]) {
            rollback(level, pos, before, disk, mark);
            return false;
        }
        if (!level.captureBlockSnapshots) {
            level.sendBlockUpdated(pos, before, next, Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(pos, next.getBlock());
            next.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS, Block.UPDATE_LIMIT - 1);
        }
        return true;
    }

    private static void rollback(Level level, BlockPos pos, BlockState before, CompoundTag disk, int mark) {
        boolean capture = level.captureBlockSnapshots;
        boolean restoring = level.restoringBlockSnapshots;
        level.captureBlockSnapshots = false;
        level.restoringBlockSnapshots = true;
        try {
            if (level.capturedBlockSnapshots.size() > mark) level.capturedBlockSnapshots.subList(mark, level.capturedBlockSnapshots.size()).clear();
            if (level.getBlockState(pos) != before && !level.setBlock(pos, before, SILENT)) {
                throw new IllegalStateException("Unable to restore window at " + pos);
            }
            if (disk != null) {
                BlockEntity restored = level.getBlockEntity(pos);
                if (restored == null) throw new IllegalStateException("Unable to restore window entity at " + pos);
                restored.loadWithComponents(disk, level.registryAccess());
                restored.setChanged();
            }
        } finally {
            level.captureBlockSnapshots = capture;
            level.restoringBlockSnapshots = restoring;
        }
    }
}
