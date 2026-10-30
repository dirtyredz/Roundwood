package io.github.dirtyredz.roundwood.fixture;

import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirtyredz.roundwood.Roundwood;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Test-only listener that cancels block placement by one designated fake player, standing in for a
 * protection mod. Keyed by player name so tests running in parallel are unaffected.
 */
@EventBusSubscriber(modid = Roundwood.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PlacementCanceller {
    public static final String PLAYER = "rw_cancelled";
    private static volatile int multiPlaceEvents;

    private PlacementCanceller() {
    }

    public static int multiPlaceEvents() {
        return multiPlaceEvents;
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() != null && PLAYER.equals(event.getEntity().getName().getString())) {
            if (event instanceof BlockEvent.EntityMultiPlaceEvent) {
                multiPlaceEvents++;
            }
            event.setCanceled(true);
        }
    }
}
