package io.github.dirtyredz.roundwood;

import io.github.dirtyredz.roundwood.data.RoundwoodDataProviders;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModCreativeTab;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Bootstrap only: registers content and the datagen listener. */
@Mod(Roundwood.MOD_ID)
public final class Roundwood {
    public static final String MOD_ID = "roundwood";

    public Roundwood() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(RoundwoodDataProviders::gatherData);
    }
}
