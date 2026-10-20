package io.github.dirtyredz.roundwood.fixture;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Test-only listener that cancels block placement by one designated fake player, standing in for a
 * protection mod. Keyed by player name so tests running in parallel are unaffected.
 */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
