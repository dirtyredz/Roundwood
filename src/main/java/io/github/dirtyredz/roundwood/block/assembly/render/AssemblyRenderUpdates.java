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
    // Optional level-thread probe for the server lifecycle gate; never replaces the real notification.
    static BiConsumer<AppearanceAnchorBlockEntity, BlockPos> notificationProbe;
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
        if (entity.getLevel() != null && entity.getLevel().isClientSide
                && layout(entity.getBlockPos(), entity.getBlockState()) != null) lifecycle.accept(entity, true);
    }
    public static void removed(AppearanceAnchorBlockEntity entity) {
        refresh(entity, true);
        if (entity.getLevel() != null && entity.getLevel().isClientSide) lifecycle.accept(entity, false);
    }

    /** Bounded member-state poll. A settled pose must not publish or dirty any chunk. */
    public static void refreshMembers(AppearanceAnchorBlockEntity entity) {
        var snapshot=entity.staticRenderPublication().current();
        var level=entity.getLevel();
        if(level==null)return;
        if(snapshot==null){refresh(entity,false);return;}
        for(var part:entity.staticRenderPublication().footprint()) {
            var current=level.isLoaded(part)?level.getBlockState(part):null;
            if(snapshot.members().get(part)!=current){refresh(entity,false);return;}
        }
    }

    public static void refresh(AppearanceAnchorBlockEntity entity, boolean removed) {
        var level = entity.getLevel();
        if (level == null || !removed && (AssemblyMutationBatch.isMutating(level, entity.getBlockPos()) || level.restoringBlockSnapshots)) return;
        var old = entity.staticRenderPublication().current();
        var assembly = layout(entity.getBlockPos(), entity.getBlockState());
        boolean ownsAnchor = assembly != null && assembly.anchor().equals(entity.getBlockPos());
        if (!ownsAnchor) {
            if (old == null) return;
            // A state packet can invalidate anchor identity without replacing the entity. Retire its
            // old footprint and remember current members so polling can detect restoration once.
            assembly = old.assembly();
        }
        var members = new LinkedHashMap<BlockPos, BlockState>();
        for (var pos : assembly.positions()) if (level.isLoaded(pos)) members.put(pos, level.getBlockState(pos));
        var door = entity instanceof RoundDoorBlockEntity value ? value : null;
        var next = new AssemblyRenderSnapshot(old == null ? 1 : old.revision() + 1, assembly, members,
                ownsAnchor && !removed && !entity.isRemoved() && assembly.complete(level, entity.getBlockState().getBlock()),
                entity.appearance(), entity.trims(), door == null ? Optional.empty() : door.arch(),
                door == null ? Optional.empty() : door.camouflage().map(value -> value.material()));
        entity.staticRenderPublication().publish(next, java.util.Set.copyOf(assembly.positions()),
                () -> { if (level.isClientSide) entity.requestModelDataUpdate(); }, pos -> {
                    if (level.isLoaded(pos)) {
                        var state = level.getBlockState(pos);
                        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
                        if (notificationProbe != null) notificationProbe.accept(entity, pos);
                    }
                });
    }
}
