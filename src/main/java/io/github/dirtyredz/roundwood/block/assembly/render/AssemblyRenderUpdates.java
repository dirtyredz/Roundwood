package io.github.dirtyredz.roundwood.block.assembly.render;

import io.github.dirtyredz.roundwood.block.assembly.Assembly;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.BiConsumer;

/** Level-thread publication and a client-only registration port; safe to load on servers. */
public final class AssemblyRenderUpdates {
    private static BiConsumer<AppearanceAnchorBlockEntity, Boolean> lifecycle = (entity, loaded) -> {};
    private AssemblyRenderUpdates() {}
    public static void bind(BiConsumer<AppearanceAnchorBlockEntity, Boolean> listener) { lifecycle = listener; }

    public static Assembly<?> layout(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock)
            return io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout.of(pos, state);
        if (state.getBlock() instanceof io.github.dirtyredz.roundwood.block.archway.ArchwayBlock)
            return io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout.of(pos, state);
        if (state.getBlock() instanceof AssemblyDoorBlock) return DoorLayout.of(pos, state);
        if (state.getBlock() instanceof ArchPieceBlock block
                && state.getValue(ArchPieceBlock.CELL).in(block.design())) return ArchPieceLayout.of(pos, state);
        return null;
    }

    public static void loaded(AppearanceAnchorBlockEntity entity) {
        refresh(entity, false);
        if (entity.getLevel() != null && entity.getLevel().isClientSide) lifecycle.accept(entity, true);
    }
    public static void removed(AppearanceAnchorBlockEntity entity) {
        refresh(entity, true);
        if (entity.getLevel() != null && entity.getLevel().isClientSide) lifecycle.accept(entity, false);
    }

    public static void refresh(AppearanceAnchorBlockEntity entity, boolean removed) {
        var level = entity.getLevel();
        if (level == null || !removed && (AssemblyMutationBatch.isMutating(level, entity.getBlockPos()) || level.restoringBlockSnapshots)) return;
        var assembly = layout(entity.getBlockPos(), entity.getBlockState());
        if (assembly == null || !assembly.anchor().equals(entity.getBlockPos())) return;
        var members = new LinkedHashMap<BlockPos, BlockState>();
        for (var pos : assembly.positions()) if (level.isLoaded(pos)) members.put(pos, level.getBlockState(pos));
        var old = entity.staticRenderPublication().current();
        var door = entity instanceof RoundDoorBlockEntity value ? value : null;
        var next = new AssemblyRenderSnapshot(old == null ? 1 : old.revision() + 1, assembly, members,
                !removed && !entity.isRemoved() && assembly.complete(level, entity.getBlockState().getBlock()),
                entity.appearance(), entity.trims(), door == null ? Optional.empty() : door.arch(),
                door == null ? Optional.empty() : door.camouflage().map(value -> value.material()));
        entity.staticRenderPublication().publish(next, java.util.Set.copyOf(assembly.positions()),
                () -> { if (level.isClientSide) entity.requestModelDataUpdate(); }, pos -> {
                    if (level.isLoaded(pos)) {
                        var state = level.getBlockState(pos);
                        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
                    }
                });
    }
}
