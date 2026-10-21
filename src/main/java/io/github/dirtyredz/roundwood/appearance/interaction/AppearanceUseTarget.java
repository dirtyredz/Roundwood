package io.github.dirtyredz.roundwood.appearance.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block whose parts carry looks of different families, so a restyling gesture is judged by what
 * the hit lands on (a window's pane is a window look, its fitted trim a trim look that takes
 * masonry). {@link AppearanceUseEvents} asks this before letting a sneaking gesture through.
 */
public interface AppearanceUseTarget {
    AppearanceFamily appearanceFamilyAt(BlockState state, BlockGetter level, BlockPos pos, BlockHitResult hit);
}
