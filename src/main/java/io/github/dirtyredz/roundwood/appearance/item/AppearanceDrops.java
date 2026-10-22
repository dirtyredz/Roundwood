package io.github.dirtyredz.roundwood.appearance.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Puts the broken object's appearance on its ordinary loot. Loot tables stay plain (each part drops
 * its family item; a door or trim still drops one item because only the initiating part is
 * evaluated); this only decorates the family items in the result, so loot conditions, explosion
 * decay, {@code doTileDrops} and other mods' loot modifiers are untouched. Camouflage refunds are a
 * separate drop and never pass through here.
 */
public final class AppearanceDrops {
    private AppearanceDrops() {
    }

    /** {@code drops}, with every item of {@code family} carrying {@code appearance}. */
    public static List<ItemStack> decorate(List<ItemStack> drops, AppearanceFamily family, Appearance appearance) {
        for (ItemStack stack : drops) {
            if (stack.getItem() instanceof AppearanceBlockItem item && item.appearanceFamily() == family && !item.isAlias()) {
                AppearanceStacks.write(stack, appearance);
            }
        }
        return drops;
    }

    /**
     * {@code drops}, with every {@code item} carrying {@code appearance} (0.12.0: an archway is a trim
     * family object whose loot also holds its face trims, which keep their own looks).
     */
    public static List<ItemStack> decorate(List<ItemStack> drops, Item item, Appearance appearance) {
        for (ItemStack stack : drops) {
            if (stack.is(item)) {
                AppearanceStacks.write(stack, appearance);
            }
        }
        return drops;
    }
}
