package io.github.dirtyredz.roundwood.client.compat.jei;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Optional JEI adapter (loaded only when JEI is present): a styled stack is its own subtype, keyed by
 * its normalised appearance (the default oak stack has none, so the three default entries stay
 * single), and the 0.4.0 alias items are hidden. Cycling previews are an EMI feature; JEI shows the
 * plain defaults.
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
        IIngredientSubtypeInterpreter<ItemStack> byAppearance = (stack, context) -> {
            String key = AppearanceStacks.read(stack).key();
            return key.equals(AppearanceDefaults.of(stack.getItem()).key()) ? IIngredientSubtypeInterpreter.NONE : key;
        };
        for (Item item : ModItems.all()) {
            registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item, byAppearance);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> aliases = LegacyContent.items().stream().map(ItemStack::new).toList();
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, aliases);
    }
}
