package io.github.dirtyredz.roundwood.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.client.door.DoorAppearanceModels;
import io.github.dirtyredz.roundwood.client.door.DoorLeafModels;
import io.github.dirtyredz.roundwood.client.door.RoundDoorRenderer;
import io.github.dirtyredz.roundwood.client.item.FamilyItemModels;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.render.RingModelNames;
import io.github.dirtyredz.roundwood.client.window.WindowAppearanceModel;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-only wiring on the mod bus: the animated door renderer, the source models it draws (no
 * blockstate references them, so they are registered for baking here), the appearance-aware window
 * blockstate and item models (every trim id draws the Universal Trim's square icon), the door items'
 * panel tint, and dropping every appearance, donor and camouflage cache whenever models are re-baked.
 * Never loaded on a server.
 */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RoundwoodClient {
    private RoundwoodClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        io.github.dirtyredz.roundwood.client.render.assembly.AssemblyRenderLifecycle.bind();
        event.registerBlockEntityRenderer(ModBlockEntities.ROUND_DOOR.get(), RoundDoorRenderer::new);
    }

    @SubscribeEvent
    public static void registerSourceModels(ModelEvent.RegisterAdditional event) {
        for (DoorDesign design : DoorDesign.values()) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                event.register(DoorLeafModels.location(design, hinge));
            }
        }
        for (Quarter part : Quarter.values()) {
            event.register(RingModelNames.quarterLocation(part, false));
            event.register(RingModelNames.quarterLocation(part, true));
        }
    }

    /** Windows read their appearance through model data; every family item resolves its stack's appearance. */
    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        WindowAppearanceModel.wrapBlocks(models);
        io.github.dirtyredz.roundwood.client.render.StaticWorldModel.wrapBlocks(models);
        for (Item item : Stream.of(ModItems.all().stream(), LegacyContent.items().stream()).flatMap(stream -> stream).distinct().toList()) {
            ModelResourceLocation location = new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(item), "inventory");
            BakedModel model = models.get(location);
            if (model == null) {
                continue;
            }
            models.put(location, FamilyItemModels.wrap(item, model));
        }
    }

    /** Door items in an unfamiliar wood tint their panel (tint index 0) with the donor colour; brass is never tinted. */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        DoorAppearanceModels.registerItemColors(event);
    }

    @SubscribeEvent public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        io.github.dirtyredz.roundwood.client.material.StaticBlockColors.register(event);
    }

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        AppearanceModelCache.clearAll();
    }
}
