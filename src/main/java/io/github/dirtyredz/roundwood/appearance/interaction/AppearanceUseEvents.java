package io.github.dirtyredz.roundwood.appearance.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * The one narrow exception to Forge's sneak-use bypass. A sneaking player holding anything never
 * reaches a block's {@code use}; for a click a Roundwood block would handle (a block that decides by
 * the hit, {@link AppearanceUseTarget}, is asked directly: a window's targeted actions, a door's trim
 * ring; otherwise a recognised restyling gesture: sneak + planks, sneak + dye, sneak + masonry on a
 * trim) this lets the block handle it and stops the item from being used (so a plank is never placed
 * against the door). The block's own router then performs the action exactly once. Runs the same on both logical sides; only the server mutates or
 * charges. Cancelled events and existing denials (protection mods) are left alone.
 */
public final class AppearanceUseEvents {
    private AppearanceUseEvents() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.isCanceled() || event.getUseBlock().isFalse() || !player.isSecondaryUseActive()) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof AppearanceDefaults bearer)) {
            return;
        }
        boolean claimed = state.getBlock() instanceof AppearanceUseTarget target
                ? target.claimsUse(state, event.getLevel(), event.getPos(), player, event.getHand(), event.getHitVec())
                : AppearanceGestures.of(bearer.appearanceFamily(), player, event.getHand()).isPresent();
        if (claimed) {
            event.setUseBlock(TriState.TRUE);
            event.setUseItem(TriState.FALSE);
        }
    }
}
