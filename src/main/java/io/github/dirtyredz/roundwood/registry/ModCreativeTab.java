package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The Roundwood creative tab: exactly the five default items, door, window, left and right arch, trim (never the hidden aliases). */
public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Roundwood.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ROUNDWOOD = TABS.register(Roundwood.MOD_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Roundwood.MOD_ID))
            .icon(() -> new ItemStack(ModItems.ROUND_DOOR.get()))
            .displayItems((parameters, output) -> ModItems.all().forEach(output::accept))
            .build());

    private ModCreativeTab() {
    }
}
