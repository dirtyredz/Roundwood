package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.item.StackPayloads;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

/** Checked stack custody shared by ordinary placement, fitted placement, loot, pick and viewers. */
public final class DoorHardwareStacks {
    private DoorHardwareStacks() {}
    public static DoorHardware read(ItemStack stack){var store=new DoorHardwareStore();store.load(StackPayloads.customData(stack));return store.value();}
    /**
     * Whether placement may read this stack: its only change from the item's defaults is custom data holding
     * the accepted keys (any name, lore, enchantment or removed default is refused, as the old whole-tag key
     * check refused them), with a canonical appearance and editable hardware.
     */
    public static boolean readable(ItemStack stack){
        if(stack.isEmpty())return false;
        if(!StackPayloads.onlyChanges(stack,Set.of(DataComponents.CUSTOM_DATA),Set.of()))return false;
        var tag=StackPayloads.customData(stack);if(tag==null)return true;
        var keys=stack.getItem() instanceof RoundDoorItem?Set.of(AppearanceNbt.TAG,DoorHardwareStore.TAG):Set.of(AppearanceNbt.TAG);
        if(!keys.containsAll(tag.getAllKeys()))return false;
        if(tag.contains(AppearanceNbt.TAG) && !AppearanceNbt.decode(tag.getCompound(AppearanceNbt.TAG))
                .map(a->a.equals(AppearanceStacks.read(stack)) && AppearanceNbt.encode(a).equals(tag.get(AppearanceNbt.TAG))).orElse(false))return false;
        var store=new DoorHardwareStore();store.load(tag);return store.editable();
    }
    public static void copy(ItemStack source,CompoundTag target){
        target.remove(DoorHardwareStore.TAG);
        var hardware=StackPayloads.custom(source,DoorHardwareStore.TAG);
        if(hardware!=null)target.put(DoorHardwareStore.TAG,hardware);
    }
    public static ItemStack decorate(ItemStack stack,RoundDoorBlockEntity entity){
        StackPayloads.editCustom(stack,entity::writeHardware);return stack;
    }
    public static String key(ItemStack stack){
        String base=AppearanceStacks.read(stack).key();
        var hardware=StackPayloads.custom(stack,DoorHardwareStore.TAG);
        return hardware!=null ? base+"|hardware/"+hardware : base;
    }
}
