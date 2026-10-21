package io.github.dirtyredz.roundwood.appearance.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * The one narrow exception to Forge's sneak-use bypass. A sneaking player holding anything never
 * reaches a block's {@code use}; for a recognised restyling gesture on a Roundwood block (sneak +
 * planks, sneak + dye, sneak + masonry on a trim, judged for the part under the hit when the block
 * says which family that is, {@link AppearanceUseTarget}) this lets the block handle it and stops the item
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
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof AppearanceDefaults bearer)) {
            return;
        }
        AppearanceFamily family = state.getBlock() instanceof AppearanceUseTarget target
                ? target.appearanceFamilyAt(state, event.getLevel(), event.getPos(), event.getHitVec())
                : bearer.appearanceFamily();
        if (AppearanceGestures.of(family, player, event.getHand()).isPresent()) {
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
