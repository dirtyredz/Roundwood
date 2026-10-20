package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The Roundwood creative tab: every door, then every window, each once. */
public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Roundwood.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ROUNDWOOD = TABS.register(Roundwood.MOD_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Roundwood.MOD_ID))
            .icon(() -> new ItemStack(ModBlocks.door(FinishCatalog.PAINT_BASE)))
            .displayItems((parameters, output) -> ModItems.all().forEach(output::accept))
            .build());

    private ModCreativeTab() {
    }
}
