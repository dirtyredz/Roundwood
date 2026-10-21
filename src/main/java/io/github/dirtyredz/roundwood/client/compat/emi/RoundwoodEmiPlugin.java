package io.github.dirtyredz.roundwood.client.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceSamples;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.appearance.material.PlankMaterials;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Optional EMI adapter (loaded only when EMI is present): exactly one index entry per canonical item,
 * drawn as a cycling preview of sample appearances ({@link CyclingAppearanceStack}) while staying the
 * default stack for retrieval, comparison and recipes; the 0.4.0 aliases and any duplicate default
 * entries are removed; styled stacks compare by their normalised appearance key (plus a window's
 * glass colour, 0.8.0); previews save as
 * plain stacks. Samples are rediscovered whenever EMI reloads (tags and resources).
 */
@EmiEntrypoint
public final class RoundwoodEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        Set<Item> aliases = Set.copyOf(LegacyContent.items());
        registry.removeEmiStacks(stack -> aliases.contains(stack.getItemStack().getItem()));
        for (Item item : ModItems.all()) {
            List<ItemStack> samples = AppearanceSamples.forFamily(((AppearanceDefaults) item).appearanceFamily(),
                    PlankMaterials.present(), MasonryMaterials.blocks()).stream()
                    .map(appearance -> AppearanceStacks.create(item, appearance)).toList();
            CyclingAppearanceStack preview = new CyclingAppearanceStack(new ItemStack(item), samples);
            registry.removeEmiStacks(stack -> stack.getItemStack().is(item) && !(stack instanceof CyclingAppearanceStack));
            registry.addEmiStack(preview);
            registry.setDefaultComparison(item, Comparison.compareData(RoundwoodEmiPlugin::appearanceKey));
        }
        registry.addIngredientSerializer(CyclingAppearanceStack.class, new CyclingAppearanceSerializer());
    }

    private static String appearanceKey(EmiStack stack) {
        return WindowGlassStacks.key(stack.getItemStack());
    }
}
