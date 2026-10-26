package io.github.dirtyredz.roundwood.migration.conversion;

import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimNbt;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimNbt;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.migration.block.RetiredArchwayBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Atomic replacement with the current wider crown and full-height keyhole bases.
 * Unknown trim versions and unloaded assemblies stay untouched.
 */
public final class ArchwayConversion {
    private static final int SILENT = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private ArchwayConversion() {}

    public static LegacyCells.Result convert(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || level.restoringBlockSnapshots || level.captureBlockSnapshots)
            return LegacyCells.Result.DEFERRED;
        var source = level.getBlockState(pos);
        if (!(source.getBlock() instanceof RetiredArchwayBlock)) return LegacyCells.Result.NOT_LEGACY;
        var assembly = ArchwayLayout.of(pos, source);
        if (!assembly.loaded(level)) return LegacyCells.Result.DEFERRED;
        if (!assembly.complete(level, source.getBlock())) return LegacyCells.Result.INCOMPLETE;
        if (!(level.getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity old) || !old.trimsEditable())
            return LegacyCells.Result.DEFERRED;
        Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> saved = new LinkedHashMap<>();
        Map<BlockPos, BlockState> writes = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> contents = new LinkedHashMap<>();
        for (var cell : assembly.positions()) {
            before.put(cell, level.getBlockState(cell));
            var entity = level.getBlockEntity(cell);
            if (entity != null) saved.put(cell, entity.saveWithFullMetadata());
            writes.put(cell, Blocks.AIR.defaultBlockState());
        }
        CompoundTag body = new CompoundTag();
        AppearanceNbt.write(body, old.appearance());
        CompoundTag disk = old.saveWithFullMetadata();
        CompoundTag top = disk.copy();
        for (String metadata : java.util.List.of("id", "x", "y", "z", ArchwayTrimNbt.TAG)) top.remove(metadata);
        AppearanceNbt.write(top, old.appearance());
        if (disk.contains(ArchwayTrimNbt.TAG)) top.put(ArchPieceTrimNbt.TAG, disk.get(ArchwayTrimNbt.TAG).copy());
        add(new ArchPieceLayout.Assembly(assembly.anchor().above(), assembly.facing(), ArchPieceDesign.TOP_1X2, Side.LEFT), top, writes, contents);
        if (assembly.design() == ArchwayDesign.KEYHOLE) {
            add(new ArchPieceLayout.Assembly(assembly.anchor(), assembly.facing(), ArchPieceDesign.KEYHOLE_SHOULDER, Side.LEFT), body, writes, contents);
            add(new ArchPieceLayout.Assembly(assembly.anchor().relative(assembly.facing().getClockWise()), assembly.facing(),
                    ArchPieceDesign.KEYHOLE_SHOULDER, Side.RIGHT), body, writes, contents);
        }
        boolean[] valid = {false};
        boolean ran;
        try {
            ran = AssemblyMutationBatch.run(level, before.keySet(), () -> {
                for (var entry : writes.entrySet()) {
                    if (!level.setBlock(entry.getKey(), entry.getValue(), SILENT) || level.getBlockState(entry.getKey()) != entry.getValue()) return;
                }
                for (var entry : contents.entrySet()) {
                    if (!(level.getBlockEntity(entry.getKey()) instanceof ArchPieceBlockEntity entity)) return;
                    entity.load(entry.getValue().copy());
                    var actual = entity.saveWithFullMetadata();
                    if (!entry.getValue().getAllKeys().stream().allMatch(key -> entry.getValue().get(key).equals(actual.get(key)))) return;
                    entity.setChanged();
                }
                valid[0] = true;
            });
        } catch (RuntimeException | Error failure) {
            restore(level, before, saved);
            throw failure;
        }
        if (!ran || !valid[0]) {
            restore(level, before, saved);
            return LegacyCells.Result.DEFERRED;
        }
        writes.forEach((cell, state) -> level.sendBlockUpdated(cell, before.get(cell), state, Block.UPDATE_CLIENTS));
        writes.forEach((cell, state) -> {
            level.updateNeighborsAt(cell, state.getBlock());
            state.updateNeighbourShapes(level, cell, Block.UPDATE_CLIENTS, Block.UPDATE_LIMIT - 1);
        });
        return LegacyCells.Result.CONVERTED;
    }

    private static void add(ArchPieceLayout.Assembly piece, CompoundTag data, Map<BlockPos, BlockState> writes,
                            Map<BlockPos, CompoundTag> contents) {
        for (var cell : piece.parts()) writes.put(piece.pos(cell), piece.stateFor(cell, ModBlocks.archPiece(piece.design())));
        contents.put(piece.anchor(), data.copy());
    }

    private static void restore(Level level, Map<BlockPos, BlockState> before, Map<BlockPos, CompoundTag> saved) {
        boolean restoring = level.restoringBlockSnapshots;
        level.restoringBlockSnapshots = true;
        try {
            before.forEach((cell, state) -> {
                if (level.getBlockState(cell) != state && !level.setBlock(cell, state, SILENT))
                    throw new IllegalStateException("Unable to restore archway at " + cell);
            });
            saved.forEach((cell, data) -> {
                var entity = level.getBlockEntity(cell);
                if (entity == null) throw new IllegalStateException("Unable to restore archway entity at " + cell);
                entity.load(data);
                entity.setChanged();
            });
        } finally {
            level.restoringBlockSnapshots = restoring;
        }
    }
}
