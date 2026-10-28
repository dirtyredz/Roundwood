package io.github.dirtyredz.roundwood.migration.item;

import io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowFaceIdentity;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowShapes;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Set;
import java.util.stream.Collectors;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassNbt;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.migration.conversion.WindowCellConversion;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Optional;

/** Read-only placement proposal and checked installation, before the ordinary placement payment. */
public final class CrossWindowPlacement {
    public record Plan(BlockPos pos, BlockState state, CompoundTag contents) {
        public Plan { pos = pos.immutable(); contents = contents.copy(); }
        @Override public CompoundTag contents() { return contents.copy(); }

        /** Exact selection geometry from the preflight contents, without reading a destination entity. */
        public VoxelShape selection() {
            var pane = (WindowPaneBlock) state.getBlock();
            var window = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), pos, state);
            window.load(contents.copy());
            var design = new WindowDesign(pane.profile(), WindowPattern.CROSS);
            return WindowShapes.selection(design, pane.pose(state),
                    sides(window, TrimSlot.PERIMETER), sides(window, TrimSlot.SILL));
        }

        private Set<Direction> sides(RoundWindowBlockEntity window, TrimSlot slot) {
            if (!WindowPaneBlock.trimmed(state)) return Set.of();
            var pane = (WindowPaneBlock) state.getBlock();
            return slot.addresses().stream().filter(window.trims(slot)::has)
                    .map(face -> WindowFaceIdentity.side(pane.pose(state), WindowPaneBlock.faceReversed(state), face))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private CrossWindowPlacement() { }

    public static Optional<Plan> plan(RetiredCrossWindowItem item, BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        if (!level.isLoaded(pos) || !context.canPlace() || level.isOutsideBuildHeight(pos)
                || !level.getWorldBorder().isWithinBounds(pos) || level.restoringBlockSnapshots
                || player != null && (!player.mayBuild() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack))) return Optional.empty();
        var state = item.getBlock().getStateForPlacement(context);
        if (state == null) return Optional.empty();
        var properties = stack.getTagElement("BlockStateTag");
        if (properties != null) {
            for (String key : properties.getAllKeys()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
                if (property != null) state = property(state, property, properties.getString(key));
            }
        }
        if (!state.canSurvive(level, pos) || !level.isUnobstructed(state, pos,
                player == null ? CollisionContext.empty() : CollisionContext.of(player))) return Optional.empty();
        CompoundTag disk = stack.getTagElement("BlockEntityTag");
        disk = disk == null ? new CompoundTag() : disk.copy();
        WindowGlassNbt.write(disk, CrossWindowCompatibility.glass(stack));
        var contents = WindowCellConversion.contents(pos, state, disk, CrossWindowCompatibility.appearance(stack), true);
        if (contents.isEmpty()) return Optional.empty();
        return Optional.of(new Plan(pos, WindowCellConversion.summary(pos, state, contents.get()), contents.get()));
    }

    private static <T extends Comparable<T>> BlockState property(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    public static InteractionResult place(RetiredCrossWindowItem item, BlockPlaceContext context) {
        var proposed = plan(item, context);
        if (proposed.isEmpty()) return InteractionResult.FAIL;
        var level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Plan plan = proposed.get();
        if (!WindowCellConversion.install(level, plan.pos(), plan.state(), plan.contents())) return InteractionResult.FAIL;
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        if (player instanceof ServerPlayer server) CriteriaTriggers.PLACED_BLOCK.trigger(server, plan.pos(), stack);
        var sound = plan.state().getSoundType(level, plan.pos(), player);
        level.playSound(player, plan.pos(), sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, plan.pos(), GameEvent.Context.of(player, plan.state()));
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
