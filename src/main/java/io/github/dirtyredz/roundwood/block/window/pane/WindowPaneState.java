package io.github.dirtyredz.roundwood.block.window.pane;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowFaceIdentity;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.Map;
import java.util.EnumMap;
import java.util.Optional;
import net.minecraft.world.level.Level;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;

/** World lookup for effective pane geometry. Registration identity remains the plain profile. */
public final class WindowPaneState {
    private WindowPaneState() { }

    /** No world or entity references: safe to hand to a chunk worker. */
    public record Snapshot(WindowDesign design, WindowPose pose, Set<Direction> connections) {
        public Snapshot { connections = Set.copyOf(connections); }
    }

    public static Optional<Appearance> beams(BlockState state, RoundWindowBlockEntity window) {
        Optional<Appearance> fitted = window.trims(TrimSlot.CROSS_BEAMS).get(AttachmentFace.FRONT);
        return fitted.isPresent() ? fitted : ((WindowPaneBlock) state.getBlock()).design().pattern() == WindowPattern.CROSS
                ? Optional.of(window.appearance()) : Optional.empty();
    }

    /** Called on the level thread at load, state change and packet application, never during baking. */
    public static Map<Direction, Snapshot> neighbours(Level level, BlockPos pos) {
        Map<Direction, Snapshot> out = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            BlockPos adjacent = pos.relative(side);
            if (!level.isLoaded(adjacent)) continue;
            BlockState state = level.getBlockState(adjacent);
            RoundWindowBlockEntity window = entity(level, adjacent);
            if (state.getBlock() instanceof WindowPaneBlock block && window != null && window.renderAvailable()
                    && window.trimsEditable(TrimSlot.CROSS_BEAMS)) {
                out.put(side, new Snapshot(design(state, window), block.pose(state), WindowPaneBlock.connections(state)));
            }
        }
        return Map.copyOf(out);
    }

    public static void refreshAround(Level level, BlockPos pos) {
        if (!level.isClientSide) return;
        refresh(level, pos);
        for (Direction side : Direction.values()) refresh(level, pos.relative(side));
    }

    private static void refresh(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return;
        RoundWindowBlockEntity window = entity(level, pos);
        if (window != null && !window.isRemoved()) {
            window.refreshRenderData();
        }
    }

    @Nullable
    public static RoundWindowBlockEntity entity(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window ? window : null;
    }

    public static WindowDesign design(BlockState state, @Nullable RoundWindowBlockEntity window) {
        WindowDesign registered = ((WindowPaneBlock) state.getBlock()).design();
        return window != null && window.trims(TrimSlot.CROSS_BEAMS).any()
                ? new WindowDesign(registered.profile(), WindowPattern.CROSS) : registered;
    }

    public static Set<Direction> attachedSides(BlockState state, BlockGetter level, BlockPos pos, TrimSlot slot) {
        RoundWindowBlockEntity window = entity(level, pos);
        if (!WindowPaneBlock.trimmed(state) || window == null || slot.scope() != TrimSlot.Scope.FACE) return Set.of();
        WindowPaneBlock block = (WindowPaneBlock) state.getBlock();
        Set<Direction> sides = EnumSet.noneOf(Direction.class);
        for (AttachmentFace face : slot.addresses()) {
            if (window.trims(slot).has(face)) {
                sides.add(WindowFaceIdentity.side(block.pose(state), WindowPaneBlock.faceReversed(state), face));
            }
        }
        return Set.copyOf(sides);
    }
}
