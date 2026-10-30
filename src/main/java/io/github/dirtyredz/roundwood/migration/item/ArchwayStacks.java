package io.github.dirtyredz.roundwood.migration.item;

import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.item.StackPayloads;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** A planned inventory rewrite: keep arbitrary source metadata on the top, split only shoulders. */
public final class ArchwayStacks {
    private ArchwayStacks() {}

    public static List<ItemStack> replacements(ItemStack source) {
        if (!(source.getItem() instanceof RetiredArchwayItem retired)) return List.of();
        var look = StackPayloads.readCustom(source, AppearanceNbt::read).orElseGet(() ->
                AppearanceNbt.read(StackPayloads.blockEntityData(source)).orElse(AppearanceStacks.read(source)));
        // The whole component patch (names, enchantments, unknown custom data, removed defaults) moves to the top.
        var top = source.transmuteCopy(ModItems.archPiece(ArchPieceDesign.TOP_1X2), source.getCount());
        AppearanceStacks.write(top, look);
        List<ItemStack> result = new ArrayList<>();
        result.add(top);
        if (retired.archway().design() == ArchwayDesign.KEYHOLE) {
            int remaining = source.getCount() * 2;
            while (remaining > 0) {
                var shoulder = AppearanceStacks.create(ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER), look);
                shoulder.setCount(Math.min(remaining, shoulder.getMaxStackSize()));
                remaining -= shoulder.getCount();
                result.add(shoulder);
            }
        }
        return result;
    }

    /** Overflow must be accepted as a whole before committing any inventory changes. */
    public static boolean convert(Container holder, Predicate<List<ItemStack>> overflow) {
        List<ItemStack> planned = new ArrayList<>();
        List<ItemStack> extra = new ArrayList<>();
        boolean changed = false;
        for (int slot = 0; slot < holder.getContainerSize(); slot++) {
            var source = holder.getItem(slot);
            var replacements = replacements(source);
            planned.add(replacements.isEmpty() ? source.copy() : replacements.get(0));
            if (!replacements.isEmpty()) {
                changed = true;
                extra.addAll(replacements.subList(1, replacements.size()));
            }
        }
        if (!changed) return false;
        List<ItemStack> dropped = new ArrayList<>();
        for (var stack : extra) {
            for (int slot = 0; slot < planned.size() && !stack.isEmpty(); slot++) {
                var target = planned.get(slot);
                // Inventory's default canPlaceItem accepts armor slots too; extra building pieces belong in storage.
                if (holder instanceof net.minecraft.world.entity.player.Inventory inventory && slot >= inventory.items.size()) continue;
                if (!holder.canPlaceItem(slot, stack)) continue;
                int max = Math.min(holder.getMaxStackSize(stack), stack.getMaxStackSize());
                if (target.isEmpty()) planned.set(slot, stack.split(Math.min(max, stack.getCount())));
                else if (ItemStack.isSameItemSameComponents(target, stack)) {
                    int count = Math.min(stack.getCount(), Math.max(0, max - target.getCount()));
                    target.grow(count);
                    stack.shrink(count);
                }
            }
            if (!stack.isEmpty()) dropped.add(stack);
        }
        if (!dropped.isEmpty() && !overflow.test(List.copyOf(dropped))) return false;
        for (int slot = 0; slot < planned.size(); slot++) holder.setItem(slot, planned.get(slot));
        holder.setChanged();
        return true;
    }
}
