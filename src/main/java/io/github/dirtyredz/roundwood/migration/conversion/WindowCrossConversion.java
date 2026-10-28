package io.github.dirtyredz.roundwood.migration.conversion;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The 0.7–0.12 crossed products become a plain pane carrying one independent cross. */
public final class WindowCrossConversion {
    private WindowCrossConversion() { }

    public static LegacyCells.Result convert(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || level.restoringBlockSnapshots) return LegacyCells.Result.DEFERRED;
        var state = level.getBlockState(pos);
        if (!CrossWindowCompatibility.isRetired(state)) return LegacyCells.Result.NOT_LEGACY;
        var block = (WindowPaneBlock) state.getBlock();
        return WindowCellConversion.convert(level, pos, state, ModBlocks.window(WindowDesign.plain(block.profile())), true);
    }
}
