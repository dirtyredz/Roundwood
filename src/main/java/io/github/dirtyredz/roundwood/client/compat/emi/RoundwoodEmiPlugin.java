package io.github.dirtyredz.roundwood.client.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.Set;

/**
 * Optional EMI adapter (loaded only when EMI is present): every canonical item keeps the single
 * default (oak) entry the creative tab gives it, the same for doors, windows, archways and trims;
 * there are no cycling previews. The 0.4.0 aliases are removed from the index, and styled stacks
 * compare by their normalised appearance key (plus a window's glass colour, 0.8.0).
 */
@EmiEntrypoint
public final class RoundwoodEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        Set<Item> aliases = Set.copyOf(LegacyContent.items());
        registry.removeEmiStacks(stack -> aliases.contains(stack.getItemStack().getItem()));
        for (Item item : ModItems.all()) {
            registry.setDefaultComparison(item, Comparison.compareData(RoundwoodEmiPlugin::appearanceKey));
        }
    }

    private static String appearanceKey(EmiStack stack) {
        return stack.getItemStack().getItem() instanceof io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem
                ? io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStacks.key(stack.getItemStack()) : WindowGlassStacks.key(stack.getItemStack());
    }
}
