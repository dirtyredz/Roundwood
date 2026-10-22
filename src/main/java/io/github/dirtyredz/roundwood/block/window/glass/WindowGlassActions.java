package io.github.dirtyredz.roundwood.block.window.glass;

import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;

/**
 * Tinting a window's glass with dye (0.8.0), on the server, as one checked change: the whole window
 * (connector glass included) takes the dye's colour. Like every restyle: build permission first, one
 * dye per actual change in survival, none in creative, no refund; the same colour again is handled
 * and free. Another dye replaces the tint; there is no way back to clear but a new window.
 */
public final class WindowGlassActions {
    private WindowGlassActions() {
    }

    public static InteractionResult tint(Level level, BlockPos pos, Player player, ItemStack dye, DyeColor color) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window) || !player.mayBuild()
                || !window.mayEdit(player) || window.glass().equals(Optional.of(color))) {
            return InteractionResult.CONSUME;
        }
        window.setGlass(Optional.of(color));
        if (!player.getAbilities().instabuild) {
            dye.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.CONSUME;
    }
}
