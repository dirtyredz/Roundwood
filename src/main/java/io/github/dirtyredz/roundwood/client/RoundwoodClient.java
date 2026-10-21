package io.github.dirtyredz.roundwood.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.client.camouflage.CamouflageModels;
import io.github.dirtyredz.roundwood.client.door.DoorLeafModels;
import io.github.dirtyredz.roundwood.client.door.RoundDoorRenderer;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only wiring on the mod bus: the door leaf renderer, the leaf models it draws (no
 * blockstate references them, so they are registered for baking here), and dropping the cached
 * camouflage models whenever models are re-baked. Never loaded on a server.
 */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RoundwoodClient {
    private RoundwoodClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ROUND_DOOR.get(), RoundDoorRenderer::new);
    }

    @SubscribeEvent
    public static void registerLeafModels(ModelEvent.RegisterAdditional event) {
        for (Finish finish : FinishCatalog.ALL) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                event.register(DoorLeafModels.location(finish.doorId(), hinge));
            }
        }
    }

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        CamouflageModels.clear();
    }
}
