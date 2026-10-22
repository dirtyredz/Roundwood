package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimFitting;
import io.github.dirtyredz.roundwood.block.attachment.TrimHost;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
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
 * The window as a face-attachment host (0.9.0; the arch sill slot since 0.10.0): finds the window and
 * checks build permission over its cell, then hands the shared transaction ({@link TrimFitting}) a
 * {@link TrimHost} for one {@link TrimSlot} whose writes keep that slot's faces and the blockstate's
 * {@link WindowPaneBlock#TRIMMED} summary ("any attachment") in step ({@link #store}). A slot the
 * window's profile cannot carry (a sill on anything but an upper arch) has no host. Fitting, removal,
 * payment, refunds and refusals are the shared rules.
 */
public final class WindowTrimActions {
    private WindowTrimActions() {
    }

    public static InteractionResult fit(Level level, BlockPos pos, Player player, ItemStack stack, TrimSlot slot,
                                        AttachmentFace face) {
        return TrimFitting.fit(level, player, stack, face, () -> editable(level, pos, player, slot));
    }

    public static InteractionResult remove(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack shears,
                                           TrimSlot slot, AttachmentFace face) {
        return TrimFitting.remove(level, player, hand, shears, face, () -> editable(level, pos, player, slot));
    }

    /** A perimeter trim ({@link #install(Level, BlockPos, TrimSlot, AttachmentFace, Appearance)}). */
    public static boolean install(Level level, BlockPos pos, AttachmentFace face, Appearance look) {
        return install(level, pos, TrimSlot.PERIMETER, face, look);
    }

    /**
     * Fits an attachment in {@code look} in {@code slot} on {@code face} of the window at {@code pos}
     * without a player (the fit's own write; the showcase and tests build through it). False when there
     * is no window there, its profile cannot carry the slot, or that face is taken.
     */
    public static boolean install(Level level, BlockPos pos, TrimSlot slot, AttachmentFace face, Appearance look) {
        return host(level, pos, slot).map(host -> TrimFitting.install(host, face, look)).orElse(false);
    }

    /** One face's attachment in {@code slot} as a restyling target (permission over the cell). */
    public static AppearanceActions.Target target(Level level, BlockPos pos, RoundWindowBlockEntity window, TrimSlot slot,
                                                  AttachmentFace face) {
        return TrimFitting.target(host(level, pos, window, slot), face, window::mayEdit);
    }

    /** Writes the faces of {@code slot} and brings the {@code trimmed} summary into step. */
    public static boolean store(Level level, BlockPos pos, RoundWindowBlockEntity window, TrimSlot slot, FaceTrims next) {
        if (!window.setTrims(slot, next)) {
            return false;
        }
        reconcile(level, pos);
        return true;
    }

    /** Sets {@code trimmed} to "any attachment present" when it disagrees (clients only: no shape or neighbour change). */
    public static void reconcile(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WindowPaneBlock && level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window
                && window.attachmentsEditable() && WindowPaneBlock.trimmed(state) != window.anyAttachment()) {
            level.setBlock(pos, state.setValue(WindowPaneBlock.TRIMMED, window.anyAttachment()), Block.UPDATE_CLIENTS);
        }
    }

    private static Optional<TrimHost> editable(Level level, BlockPos pos, Player player, TrimSlot slot) {
        if (!player.mayBuild() || !level.mayInteract(player, pos)) {
            return Optional.empty();
        }
        return host(level, pos, slot);
    }

    private static Optional<TrimHost> host(Level level, BlockPos pos, TrimSlot slot) {
        if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window)
                || !(window.getBlockState().getBlock() instanceof WindowPaneBlock block)
                || !WindowTrimGeometry.supports(slot, block.profile())) {
            return Optional.empty();
        }
        return Optional.of(host(level, pos, window, slot));
    }

    private static TrimHost host(Level level, BlockPos pos, RoundWindowBlockEntity window, TrimSlot slot) {
        return new TrimHost() {
            @Override
            public TrimSlot slot() {
                return slot;
            }

            @Override
            public FaceTrims trims() {
                return window.trims(slot);
            }

            @Override
            public boolean editable() {
                return window.trimsEditable(slot);
            }

            @Override
            public boolean store(FaceTrims next) {
                return WindowTrimActions.store(level, pos, window, slot, next);
            }

            @Override
            public BlockPos origin() {
                return pos;
            }
        };
    }
}
