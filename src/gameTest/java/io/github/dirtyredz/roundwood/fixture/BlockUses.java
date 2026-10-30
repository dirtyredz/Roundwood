package io.github.dirtyredz.roundwood.fixture;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block's own use for one hand, as the 1.20.1 {@code BlockState.use} reported it: 1.21's item-use callback with
 * that hand's stack, reduced to its {@link InteractionResult} (a skipped or passed default interaction is a pass).
 */
public final class BlockUses {
    private BlockUses() {
    }

    public static InteractionResult use(BlockState state, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        return state.useItemOn(player.getItemInHand(hand), level, player, hand, hit).result();
    }
}
