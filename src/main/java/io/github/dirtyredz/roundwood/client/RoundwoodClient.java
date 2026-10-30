package io.github.dirtyredz.roundwood.client;

import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceWorldModel;
import io.github.dirtyredz.roundwood.client.arch.archway.ArchwayWorldModel;
import io.github.dirtyredz.roundwood.client.door.DoorStaticModel;
import io.github.dirtyredz.roundwood.client.render.world.StaticModelSources;
import io.github.dirtyredz.roundwood.client.render.world.StaticWorldModel;
import io.github.dirtyredz.roundwood.client.trim.TrimWorldModel;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.client.door.DoorAppearanceModels;
import io.github.dirtyredz.roundwood.client.door.leaf.DoorLeafModels;
import io.github.dirtyredz.roundwood.client.door.leaf.RoundDoorRenderer;
import io.github.dirtyredz.roundwood.client.item.FamilyItemModels;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.trim.RingModelNames;
import io.github.dirtyredz.roundwood.client.window.WindowAppearanceModel;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import io.github.dirtyredz.roundwood.client.door.DoorParticles;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Client-only wiring on the mod bus: the animated door renderer, the source models it draws (no
 * blockstate references them, so they are registered for baking here), the appearance-aware window
 * blockstate and item models (every trim id draws the Universal Trim's square icon), the door items'
 * panel tint, and dropping every appearance, donor and camouflage cache whenever models are re-baked.
 * Never loaded on a server.
 */
@EventBusSubscriber(modid = Roundwood.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RoundwoodClient {
    private RoundwoodClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        io.github.dirtyredz.roundwood.client.render.world.AssemblyRenderLifecycle.bind();
        event.registerBlockEntityRenderer(ModBlockEntities.ROUND_DOOR.get(), RoundDoorRenderer::new);
    }

    @SubscribeEvent
    public static void registerSourceModels(ModelEvent.RegisterAdditional event) {
        for (DoorDesign design : DoorDesign.values()) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                event.register(ModelResourceLocation.standalone(DoorLeafModels.location(design, hinge)));
            }
        }
        for (Quarter part : Quarter.values()) {
            event.register(ModelResourceLocation.standalone(RingModelNames.quarterLocation(part, false)));
            event.register(ModelResourceLocation.standalone(RingModelNames.quarterLocation(part, true)));
        }
    }

    /** Windows read their appearance through model data; every family item resolves its stack's appearance. */
    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        WindowAppearanceModel.wrapBlocks(models);
        StaticWorldModel.wrapBlocks(models, staticSources());
        for (Item item : Stream.of(ModItems.all().stream(), LegacyContent.items().stream()).flatMap(stream -> stream).distinct().toList()) {
            ModelResourceLocation location = ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item));
            BakedModel model = models.get(location);
            if (model == null) {
                continue;
            }
            models.put(location, FamilyItemModels.wrap(item, model));
        }
    }

    /** Every registered host block is bound explicitly; the shared static model names no registry or family. */
    private static StaticModelSources staticSources() {
        return StaticModelSources.builder()
                .bindAll(ModBlocks.doors(), DoorStaticModel.SOURCE)
                .bindAll(LegacyContent.doors(), DoorStaticModel.SOURCE)
                .bindAll(ModBlocks.archPieces(), ArchPieceWorldModel.SOURCE)
                .bindAll(ModBlocks.archways(), ArchwayWorldModel.SOURCE)
                .bind(ModBlocks.roundTrim(), TrimWorldModel.SOURCE)
                .bindAll(LegacyContent.trims(), TrimWorldModel.SOURCE)
                .build();
    }

    /** Door items in an unfamiliar wood tint their panel (tint index 0) with the donor colour; brass is never tinted. */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        DoorAppearanceModels.registerItemColors(event);
    }

    @SubscribeEvent public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        io.github.dirtyredz.roundwood.client.material.tint.StaticBlockColors.register(event);
    }

    /** Break and hit particles from each door cell's own piece, not from the whole-door outline. */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(DoorParticles.INSTANCE, Stream.concat(ModBlocks.doors().stream(), LegacyContent.doors().stream())
                .toArray(net.minecraft.world.level.block.Block[]::new));
    }

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        AppearanceModelCache.clearAll();
    }
}
