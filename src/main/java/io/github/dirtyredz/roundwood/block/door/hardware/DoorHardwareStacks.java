package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

/** Checked stack custody shared by ordinary placement, fitted placement, loot, pick and viewers. */
public final class DoorHardwareStacks {
    private DoorHardwareStacks() {}
    public static DoorHardware read(ItemStack stack){var store=new DoorHardwareStore();store.load(stack.getTag());return store.value();}
    public static boolean readable(ItemStack stack){
        if(stack.isEmpty())return false;
        var all=stack.serializeNBT();
        if(all.contains("ForgeCaps") && !all.getCompound("ForgeCaps").isEmpty())return false;
        var tag=stack.getTag();if(tag==null)return true;
        var keys=stack.getItem() instanceof RoundDoorItem?Set.of(AppearanceNbt.TAG,DoorHardwareStore.TAG):Set.of(AppearanceNbt.TAG);
        if(!keys.containsAll(tag.getAllKeys()))return false;
        if(tag.contains(AppearanceNbt.TAG) && !AppearanceNbt.decode(tag.getCompound(AppearanceNbt.TAG))
                .map(a->a.equals(AppearanceStacks.read(stack)) && AppearanceNbt.encode(a).equals(tag.get(AppearanceNbt.TAG))).orElse(false))return false;
        var store=new DoorHardwareStore();store.load(tag);return store.editable();
    }
    public static void copy(ItemStack source,CompoundTag target){
        target.remove(DoorHardwareStore.TAG);
        if(source.hasTag() && source.getTag().contains(DoorHardwareStore.TAG))
            target.put(DoorHardwareStore.TAG,source.getTag().get(DoorHardwareStore.TAG).copy());
    }
    public static ItemStack decorate(ItemStack stack,RoundDoorBlockEntity entity){
        var tag=stack.hasTag()?stack.getTag().copy():new CompoundTag();entity.writeHardware(tag);
        stack.setTag(tag.isEmpty()?null:tag);return stack;
    }
    public static String key(ItemStack stack){
        String base=AppearanceStacks.read(stack).key();
        return stack.hasTag() && stack.getTag().contains(DoorHardwareStore.TAG)
                ? base+"|hardware/"+stack.getTag().get(DoorHardwareStore.TAG) : base;
    }
}
