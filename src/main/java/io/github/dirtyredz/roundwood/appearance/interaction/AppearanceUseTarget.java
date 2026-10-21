package io.github.dirtyredz.roundwood.appearance.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block whose clicks are decided by what the hit lands on (a window's frame, glass or face trim, a
 * door's leaf or trim ring), so the sneak-use bridge ({@link AppearanceUseEvents}) asks the block
 * itself whether its router would handle this click, using the same targeting and rules as the
 * router: sneaking then does exactly what a plain click does.
 */
public interface AppearanceUseTarget {
    boolean claimsUse(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit);
}
