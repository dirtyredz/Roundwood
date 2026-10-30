package io.github.dirtyredz.roundwood.block.assembly.placement;

import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Full-NBT rollback, including cells whose state is unchanged but whose entity is initialized. */
public final class AssemblyWriteTransaction {
    private record Captured(BlockPos pos, BlockState state, @Nullable CompoundTag entity) {}
    private AssemblyWriteTransaction() {}
    public static boolean write(Level level, Map<BlockPos, BlockState> states, BooleanSupplier initialize, BooleanSupplier approve) {
        return write(level, states, true, initialize, approve);
    }
    public static boolean write(Level level, Map<BlockPos, BlockState> states, boolean allowUnchanged, BooleanSupplier initialize, BooleanSupplier approve) {
        if (level.restoringBlockSnapshots) return false;
        var cells = List.copyOf(states.keySet());
        var before = capture(level, cells);
        int mark = level.capturedBlockSnapshots.size();
        boolean installed;
        try {
            boolean[] written = {true};
            boolean ran = AssemblyMutationBatch.run(level, cells, () -> {
                for (var entry : states.entrySet()) {
                    if (allowUnchanged && level.getBlockState(entry.getKey()).equals(entry.getValue())) continue;
                    if (!level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL)) { written[0] = false; return; }
                }
                if (written[0]) written[0] = initialize.getAsBoolean() && approve.getAsBoolean();
            });
            installed = ran && written[0];
        } catch (RuntimeException | Error failure) {
            rollback(level, cells, before, mark);
            throw failure;
        }
        if (!installed) rollback(level, cells, before, mark);
        refreshRendering(level, cells);
        return installed;
    }
    private static void refreshRendering(Level level, List<BlockPos> cells) {
        for (var pos : cells) {
            if (level.getBlockEntity(pos) instanceof io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity entity)
                io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderUpdates.refresh(entity, false);
        }
    }
    private static List<Captured> capture(Level level, List<BlockPos> cells) {
        List<Captured> captured = new ArrayList<>(cells.size());
        for (BlockPos pos : cells) {
            BlockEntity entity = level.getBlockEntity(pos);
            captured.add(new Captured(pos.immutable(), level.getBlockState(pos), entity == null ? null : entity.saveWithFullMetadata(level.registryAccess())));
        }
        return captured;
    }

    /**
     * Puts every cell back, last first. While restoring, snapshot capture is off and the level counts as
     * restoring snapshots (no drops, and the assemblies' own removal and integrity callbacks stand down),
     * inside a mutation batch where nesting allows; both flags are restored afterwards whatever happens.
     * Outside a capture the neighbours are told once everything is back, as the writes told them.
     */
    private static void rollback(Level level, List<BlockPos> cells, List<Captured> before, int mark) {
        boolean capturing = level.captureBlockSnapshots;
        boolean restoring = level.restoringBlockSnapshots;
        level.captureBlockSnapshots = false;
        level.restoringBlockSnapshots = true;
        try {
            if (level.capturedBlockSnapshots.size() > mark) {
                level.capturedBlockSnapshots.subList(mark, level.capturedBlockSnapshots.size()).clear();
            }
            Runnable restore = () -> {
                for (int i = before.size() - 1; i >= 0; i--) {
                    restore(level, before.get(i));
                }
            };
            if (!AssemblyMutationBatch.run(level, cells, restore)) {
                restore.run();
            }
        } finally {
            level.captureBlockSnapshots = capturing;
            level.restoringBlockSnapshots = restoring;
        }
        refreshRendering(level, cells);
        if (!capturing) {
            for (Captured cell : before) {
                level.updateNeighborsAt(cell.pos(), cell.state().getBlock());
                cell.state().updateNeighbourShapes(level, cell.pos(), Block.UPDATE_CLIENTS, Block.UPDATE_LIMIT - 1);
            }
        }
    }

    private static void restore(Level level, Captured cell) {
        level.setBlock(cell.pos(), cell.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (cell.entity() != null) {
            BlockEntity entity = level.getBlockEntity(cell.pos());
            if (entity != null) {
                entity.loadWithComponents(cell.entity(), level.registryAccess());
                entity.setChanged();
            }
        }
    }
}
