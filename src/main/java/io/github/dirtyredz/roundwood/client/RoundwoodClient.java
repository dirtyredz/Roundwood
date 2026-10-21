package io.github.dirtyredz.roundwood.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.client.camouflage.CamouflageModels;
import io.github.dirtyredz.roundwood.client.door.DoorAppearanceModels;
import io.github.dirtyredz.roundwood.client.door.DoorLeafModels;
import io.github.dirtyredz.roundwood.client.door.RoundDoorRenderer;
import io.github.dirtyredz.roundwood.client.item.AppearanceItemOverrides;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.trim.RoundTrimRenderer;
import io.github.dirtyredz.roundwood.client.trim.TrimAppearanceModels;
import io.github.dirtyredz.roundwood.client.trim.TrimModelNames;
import io.github.dirtyredz.roundwood.client.window.WindowAppearanceModel;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Client-only wiring on the mod bus: the door and trim renderers, the source models they draw (no
 * blockstate references them, so they are registered for baking here), the appearance-aware window
 * blockstate and item models, the door items' panel tint, and dropping every appearance, donor and
 * camouflage cache whenever models are re-baked. Never loaded on a server.
 */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RoundwoodClient {
    private RoundwoodClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ROUND_DOOR.get(), RoundDoorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ROUND_TRIM.get(), RoundTrimRenderer::new);
    }

    @SubscribeEvent
    public static void registerSourceModels(ModelEvent.RegisterAdditional event) {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            event.register(DoorLeafModels.location(hinge));
        }
        for (TrimPart part : TrimPart.values()) {
            event.register(TrimModelNames.quarterLocation(part, false));
            event.register(TrimModelNames.quarterLocation(part, true));
        }
        event.register(TrimModelNames.wholeItemLocation());
    }

    /** Windows read their appearance through model data; every family item resolves its stack's appearance. */
    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<net.minecraft.resources.ResourceLocation, BakedModel> models = event.getModels();
        List<Block> windows = new ArrayList<>(LegacyContent.windows());
        windows.add(ModBlocks.roundWindow());
        for (Block window : windows) {
            for (BlockState state : window.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(location);
                if (model != null) {
                    models.put(location, new WindowAppearanceModel(model));
                }
            }
        }
        for (Item item : Stream.concat(ModItems.all().stream(), LegacyContent.items().stream()).toList()) {
            if (!(item instanceof AppearanceBlockItem family)) {
                continue;
            }
            ModelResourceLocation location = new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(item), "inventory");
            BakedModel model = models.get(location);
            if (model == null) {
                continue;
            }
            models.put(location, AppearanceItemOverrides.wrap(model, switch (family.appearanceFamily()) {
                case DOOR -> DoorAppearanceModels::item;
                case WINDOW -> WindowAppearanceModel::item;
                case TRIM -> TrimAppearanceModels::item;
            }));
        }
    }

    /** Door items in an unfamiliar wood tint their panel (tint index 0) with the donor colour; brass is never tinted. */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        List<Item> doors = Stream.concat(ModItems.all().stream(), LegacyContent.items().stream())
                .filter(item -> item instanceof AppearanceBlockItem family && family.appearanceFamily() == AppearanceFamily.DOOR)
                .toList();
        event.register((stack, tintIndex) -> {
            if (tintIndex != DoorAppearanceModels.PANEL_TINT) {
                return -1;
            }
            int tint = DoorAppearanceModels.look(AppearanceStacks.read(stack)).tint();
            return tint < 0 ? -1 : tint;
        }, doors.toArray(Item[]::new));
    }

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        CamouflageModels.clear();
        AppearanceModelCache.clearAll();
    }
}
