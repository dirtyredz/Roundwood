package io.github.dirtyredz.roundwood.migration.conversion;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.trim.WindowAttachmentState;
import net.minecraft.world.item.DyeColor;
import io.github.dirtyredz.roundwood.migration.block.LegacyWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts one 0.4.0 window to the canonical window: the same state (axis, position, the four
 * connections, waterlogged so the water stays, the trim summary and face identity), written
 * silently, then its appearance, glass colour and face trims on the new block entity and its
 * neighbours told.
 */
public final class LegacyWindowConversion {
    private LegacyWindowConversion() {
    }

    public static LegacyCells.Result convert(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof LegacyWindowBlock legacy)) {
            return LegacyCells.Result.NOT_LEGACY;
        }
        return WindowCellConversion.convert(level, pos, state, legacy.canonical(), false);
    }
}
