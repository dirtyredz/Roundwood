package io.github.dirtyredz.roundwood.client.compat.jei;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Optional JEI adapter (loaded only when JEI is present): a styled stack is its own subtype, keyed by
 * its normalised appearance plus, for windows, its glass colour (0.8.0) (the default oak stack has
 * none, so the default entries stay single), and the 0.4.0 alias items are hidden. Both viewers
 * show each item once, as its plain default.
 */
@JeiPlugin
public final class RoundwoodJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> byAppearance = new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext context) {
                return subtype(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                String key = subtype(stack);
                return key == null ? "" : key;
            }
        };
        for (Item item : ModItems.all()) {
            registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item, byAppearance);
        }
    }

    /** The appearance (plus glass or hardware) key, or null for the default oak stack (no subtype). */
    private static @Nullable String subtype(ItemStack stack) {
        String key = stack.getItem() instanceof io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem
                ? io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStacks.key(stack) : WindowGlassStacks.key(stack);
        return key.equals(AppearanceDefaults.of(stack.getItem()).key()) ? null : key;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> aliases = LegacyContent.items().stream().map(ItemStack::new).toList();
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, aliases);
    }
}
