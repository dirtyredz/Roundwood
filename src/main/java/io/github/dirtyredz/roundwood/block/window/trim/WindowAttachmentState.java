package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A window's attachment writes and its {@link WindowPaneBlock#TRIMMED} summary ("any attachment, either
 * slot, either face"), kept in step. The summary follows only a successful write, and only while both
 * slots' payloads are understood: an unknown sill still lets the perimeter be edited (and the reverse),
 * but the summary is then left alone, since what the unknown payload holds cannot be known.
 */
public final class WindowAttachmentState {
    private WindowAttachmentState() {
    }

    /** Writes the faces of {@code slot}; on success brings the {@code trimmed} summary into step. */
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
}
