package io.github.dirtyredz.roundwood.appearance.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Appearance on item stacks. A stack carries only {@code RoundwoodAppearance} (never camouflage,
 * coordinates, assembly identity, open or swing state), and only when it differs from the item's
 * default: crafted, picked and dropped default items therefore stack together. A stored appearance
 * the item's family does not accept (masonry on a door) reads as the item's default.
 */
public final class AppearanceStacks {
    private AppearanceStacks() {
    }

    /** One {@code item} showing {@code appearance}. */
    public static ItemStack create(Item item, Appearance appearance) {
        ItemStack stack = new ItemStack(item);
        write(stack, appearance);
        return stack;
    }

    /** The appearance {@code stack} places: its own, else its item's default. */
    public static Appearance read(ItemStack stack) {
        Appearance fallback = AppearanceDefaults.of(stack.getItem());
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return fallback;
        }
        return AppearanceNbt.read(tag).filter(a -> !(stack.getItem() instanceof AppearanceDefaults d)
                || d.appearanceFamily().accepts(a)).orElse(fallback);
    }

    /** Stores {@code appearance} on {@code stack}, or removes the tag when it is the item's default. */
    public static void write(ItemStack stack, Appearance appearance) {
        if (appearance.equals(AppearanceDefaults.of(stack.getItem()))) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(AppearanceNbt.TAG);
                if (tag.isEmpty()) {
                    stack.setTag(null);
                }
            }
            return;
        }
        AppearanceNbt.write(stack.getOrCreateTag(), appearance);
    }
}
