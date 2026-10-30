package io.github.dirtyredz.roundwood;

import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceUseEvents;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyHarvest;
import io.github.dirtyredz.roundwood.block.door.adapter.DoorUseChannel;
import io.github.dirtyredz.roundwood.data.RoundwoodDataProviders;
import io.github.dirtyredz.roundwood.migration.ChunkMigrations;
import io.github.dirtyredz.roundwood.migration.retired.ArchwayStackMigration;
import io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModCreativeTab;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.registry.ModLootConditions;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCommand;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Bootstrap only: registers content (the canonical entries, the hidden 0.4.0 aliases and retired
 * compatibility blocks), the door-use payload, the datagen listener, the showcase command and the
 * game-bus hooks (restyling gestures, deferred harvests, legacy migration).
 */
@Mod(Roundwood.MOD_ID)
public final class Roundwood {
    public static final String MOD_ID = "roundwood";

    public Roundwood(IEventBus modBus) {
        io.github.dirtyredz.roundwood.block.arch.adapter.ArchDoorFitting.bind(
                io.github.dirtyredz.roundwood.block.door.frame.fitting.DoorArchFitting::claims,
                io.github.dirtyredz.roundwood.block.door.frame.fitting.DoorArchFitting::use);
        DoorUseChannel.register(modBus);
        CrossWindowCompatibility.registerWindowHandler();
        LegacyContent.register(ModBlocks.BLOCKS, ModItems.ITEMS);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        ModLootConditions.CONDITIONS.register(modBus);
        io.github.dirtyredz.roundwood.registry.ModRecipeSerializers.SERIALIZERS.register(modBus);
        modBus.addListener(RoundwoodDataProviders::gatherData);

        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(ShowcaseCommand::register);
        gameBus.addListener(AppearanceUseEvents::onRightClickBlock);
        gameBus.addListener(AssemblyHarvest::onLevelTick);
        gameBus.addListener(ChunkMigrations::onChunkLoad);
        gameBus.addListener(ChunkMigrations::onLevelTick);
        gameBus.addListener(ChunkMigrations::onLevelUnload);
        gameBus.addListener(ArchwayStackMigration::onChunkLoad);
        gameBus.addListener(ArchwayStackMigration::onLevelTick);
        gameBus.addListener(ArchwayStackMigration::onLevelUnload);
        gameBus.addListener(ArchwayStackMigration::onPlayerTick);
        gameBus.addListener(ArchwayStackMigration::onEntityJoin);
    }
}
