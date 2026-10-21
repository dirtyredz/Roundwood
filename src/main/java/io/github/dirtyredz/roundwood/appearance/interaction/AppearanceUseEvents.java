package io.github.dirtyredz.roundwood.appearance.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * The one narrow exception to Forge's sneak-use bypass. A sneaking player holding anything never
 * reaches a block's {@code use}; for a recognised restyling gesture on a Roundwood block (sneak +
 * planks, sneak + dye, sneak + masonry on a trim) this lets the block handle it and stops the item
 * from being used (so a plank is never placed against the door). The block's own router then
 * performs the action exactly once. Runs the same on both logical sides; only the server mutates or
 * charges. Cancelled events and existing denials (protection mods) are left alone.
 */
public final class AppearanceUseEvents {
    private AppearanceUseEvents() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY || !player.isSecondaryUseActive()) {
            return;
        }
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof AppearanceDefaults bearer)) {
            return;
        }
        if (AppearanceGestures.of(bearer.appearanceFamily(), player, event.getHand()).isPresent()) {
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
