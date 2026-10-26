package io.github.dirtyredz.roundwood.block.arch.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.host.TrimFitting;
import io.github.dirtyredz.roundwood.block.attachment.host.TrimHost;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * The piece as a face-trim host (0.12.0, like the door): the trims live on the anchor entity of a
 * complete, loaded piece, and the player must be able to edit every cell. Hands the shared
 * transaction ({@link TrimFitting}) a {@link TrimHost} for that anchor, so fitting, payment, removal
 * with its refund and wear, and restyling are the doors' and windows' exactly; a refusal changes and
 * charges nothing.
 */
public final class ArchPieceTrimActions {
    private ArchPieceTrimActions() {
    }

    public static InteractionResult fit(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack,
                                        AttachmentFace face) {
        return TrimFitting.fit(level, player, stack, face, () -> editable(level, pos, state, player));
    }

    public static InteractionResult remove(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand,
                                           ItemStack shears, AttachmentFace face) {
        return TrimFitting.remove(level, player, hand, shears, face, () -> editable(level, pos, state, player));
    }

    /** Fits {@code look} on {@code face} of the complete piece at {@code pos} without a player (showcase, fixtures). */
    public static boolean install(Level level, BlockPos pos, AttachmentFace face, Appearance look) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ArchPieceBlock block)) {
            return false;
        }
        ArchPieceLayout.Assembly assembly = ArchPieceLayout.of(pos, state);
        return assembly.complete(level, block) && level.getBlockEntity(assembly.anchor()) instanceof ArchPieceBlockEntity anchor
                && TrimFitting.install(TrimHost.anchored(anchor, TrimSlot.PERIMETER, pos), face, look);
    }

    /** One face's casing as a restyling target, if the piece is complete and loaded. */
    public static Optional<AppearanceActions.Target> target(Level level, BlockPos pos, BlockState state, AttachmentFace face) {
        if (!(state.getBlock() instanceof ArchPieceBlock block)) {
            return Optional.empty();
        }
        ArchPieceLayout.Assembly assembly = ArchPieceLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)
                || !(level.getBlockEntity(assembly.anchor()) instanceof ArchPieceBlockEntity anchor)) {
            return Optional.empty();
        }
        return Optional.of(TrimFitting.target(TrimHost.anchored(anchor, TrimSlot.PERIMETER, pos), face,
                player -> assembly.mayEditAll(level, player)));
    }

    /** The anchor of the complete, loaded piece at {@code pos}, if {@code player} may edit all of it. */
    private static Optional<TrimHost> editable(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(state.getBlock() instanceof ArchPieceBlock block) || !player.mayBuild()) {
            return Optional.empty();
        }
        ArchPieceLayout.Assembly assembly = ArchPieceLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)
                || !assembly.mayEditAll(level, player)
                || !(level.getBlockEntity(assembly.anchor()) instanceof ArchPieceBlockEntity anchor)) {
            return Optional.empty();
        }
        return Optional.of(TrimHost.anchored(anchor, TrimSlot.PERIMETER, pos));
    }
}
