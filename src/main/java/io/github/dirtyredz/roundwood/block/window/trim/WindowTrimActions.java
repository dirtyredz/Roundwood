package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimFitting;
import io.github.dirtyredz.roundwood.block.attachment.TrimHost;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * The window as a face-trim host (0.9.0): finds the window and checks build permission over its
 * cell, then hands the shared transaction ({@link TrimFitting}) a {@link TrimHost} whose writes keep
 * the entity's face trims and the blockstate's {@link WindowPaneBlock#TRIMMED} summary in step
 * ({@link #store}). Fitting, removal, payment, refunds and refusals are the shared rules.
 */
public final class WindowTrimActions {
    private WindowTrimActions() {
    }

    public static InteractionResult fit(Level level, BlockPos pos, Player player, ItemStack stack, AttachmentFace face) {
        return TrimFitting.fit(level, player, stack, face, () -> editable(level, pos, player));
    }

    public static InteractionResult remove(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack shears,
                                           AttachmentFace face) {
        return TrimFitting.remove(level, player, hand, shears, face, () -> editable(level, pos, player));
    }

    /**
     * Fits a trim in {@code look} on {@code face} of the window at {@code pos} without a player (the
     * fit's own write; the showcase and tests build through it). False when there is no window there
     * or that face is taken.
     */
    public static boolean install(Level level, BlockPos pos, AttachmentFace face, Appearance look) {
        return level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window && TrimFitting.install(host(level, pos, window), face, look);
    }

    /** One face's trim as a restyling target (permission over the cell). */
    public static AppearanceActions.Target target(Level level, BlockPos pos, RoundWindowBlockEntity window, AttachmentFace face) {
        return TrimFitting.target(host(level, pos, window), face, window::mayEdit);
    }

    /** Writes the faces and brings the {@code trimmed} summary into step with them. */
    public static boolean store(Level level, BlockPos pos, RoundWindowBlockEntity window, FaceTrims next) {
        if (!window.setTrims(next)) {
            return false;
        }
        reconcile(level, pos);
        return true;
    }

    /** Sets {@code trimmed} to "either face present" when it disagrees (clients only: no shape or neighbour change). */
    public static void reconcile(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WindowPaneBlock && level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window
                && window.trimsEditable() && WindowPaneBlock.trimmed(state) != window.trims().any()) {
            level.setBlock(pos, state.setValue(WindowPaneBlock.TRIMMED, window.trims().any()), Block.UPDATE_CLIENTS);
        }
    }

    private static Optional<TrimHost> editable(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window) || !player.mayBuild() || !level.mayInteract(player, pos)) {
            return Optional.empty();
        }
        return Optional.of(host(level, pos, window));
    }

    private static TrimHost host(Level level, BlockPos pos, RoundWindowBlockEntity window) {
        return new TrimHost() {
            @Override
            public TrimSlot slot() {
                return TrimSlot.PERIMETER;
            }

            @Override
            public FaceTrims trims() {
                return window.trims();
            }

            @Override
            public boolean editable() {
                return window.trimsEditable();
            }

            @Override
            public boolean store(FaceTrims next) {
                return WindowTrimActions.store(level, pos, window, next);
            }

            @Override
            public BlockPos origin() {
                return pos;
            }
        };
    }
}
