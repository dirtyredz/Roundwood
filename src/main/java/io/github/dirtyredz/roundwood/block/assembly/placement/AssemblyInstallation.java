package io.github.dirtyredz.roundwood.block.assembly.placement;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.assembly.Assembly;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes a validated {@link AssemblyPlacementPlan} as one transaction: every cell, in part order, with
 * ordinary {@code setBlock(UPDATE_ALL)} calls inside an {@link AssemblyMutationBatch} (so a player
 * placement's cells all join Forge's block-snapshot capture and its one multi-place event), then the
 * look on the new anchor entity. Success means all of it happened. No payment, sound or event here
 * ({@link AssemblyPlacement} does that for players; player-less placement stays silent and free).
 *
 * <p>All or nothing: a refused write, a refused batch (nested too deep), a missing anchor entity or an
 * exception puts every cell back as it was, block state and block-entity data, with teardown and drops
 * suppressed while restoring, before returning false (or rethrowing). The rollback removes only the
 * block snapshots this installation added: an enclosing Forge transaction keeps its own. Entry during
 * a snapshot restore is refused. Forge's own cancellation of a whole player placement is unchanged:
 * that is the outer transaction's rollback, not this one.
 */
public final class AssemblyInstallation {
    private record Captured(BlockPos pos, BlockState state, @Nullable CompoundTag entity) {
    }

    private AssemblyInstallation() {
    }

    /** Installs {@code plan} with {@code appearance} on its anchor entity of {@code anchorType}; false (nothing changed) when refused. */
    public static <P> boolean install(Level level, AssemblyPlacementPlan<P> plan, Appearance appearance,
                                      Class<? extends AppearanceAnchorBlockEntity> anchorType) {
        if (level.restoringBlockSnapshots) {
            return false;
        }
        Assembly<P> assembly = plan.assembly();
        List<BlockPos> cells = assembly.positions();
        List<Captured> before = capture(level, cells);
        int mark = level.capturedBlockSnapshots.size();
        boolean installed;
        try {
            boolean[] written = {true};
            boolean ran = AssemblyMutationBatch.run(level, cells, () -> {
                for (P part : assembly.parts()) {
                    if (!level.setBlock(assembly.pos(part), plan.states().get(part), Block.UPDATE_ALL)) {
                        written[0] = false;
                        return;
                    }
                }
            });
            installed = ran && written[0] && installAppearance(level, assembly.anchor(), appearance, anchorType);
        } catch (RuntimeException | Error failure) {
            rollback(level, cells, before, mark);
            throw failure;
        }
        if (!installed) {
            rollback(level, cells, before, mark);
        }
        return installed;
    }

    private static boolean installAppearance(Level level, BlockPos anchor, Appearance appearance,
                                             Class<? extends AppearanceAnchorBlockEntity> anchorType) {
        if (level.getBlockEntity(anchor) instanceof AppearanceAnchorBlockEntity entity && anchorType.isInstance(entity)) {
            entity.setAppearance(appearance);
            return true;
        }
        return false;
    }

    private static List<Captured> capture(Level level, List<BlockPos> cells) {
        List<Captured> captured = new ArrayList<>(cells.size());
        for (BlockPos pos : cells) {
            BlockEntity entity = level.getBlockEntity(pos);
            captured.add(new Captured(pos.immutable(), level.getBlockState(pos), entity == null ? null : entity.saveWithFullMetadata()));
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
                entity.load(cell.entity());
                entity.setChanged();
            }
        }
    }
}
