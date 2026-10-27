package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.ArrayList;
import java.util.List;

/** Components refund only in the initiating cell's ordinary loot evaluation. Never in teardown. */
public final class DoorArchLoot {
    private DoorArchLoot() {}
    public static List<ItemStack> append(List<ItemStack> ordinary, RoundDoorBlockEntity entity, AssemblyDoorBlock block, LootParams.Builder context) {
        var result=new ArrayList<>(ordinary);
        if(entity.arch().isPresent()) {
            var frame=entity.arch().get();
            add(result,AppearanceStacks.create(ModBlocks.archPiece(frame.product(),frame.thickness()).asItem(),frame.appearance()),context);
            for(var face:AttachmentFace.values()) frame.casings().get(face).ifPresent(look->add(result,AppearanceStacks.create(TrimSlot.PERIMETER.item(),look),context));
        } else if(!entity.archEditable()) {
            // Unknown custody is opaque on the door item, never decoded into additional refunds.
            var raw=entity.saveWithoutMetadata().get(DoorArchStore.TAG);
            if(raw!=null) for(var stack:result) if(stack.getItem()==block.familyItem()) stack.getOrCreateTag().put(DoorArchStore.TAG,raw.copy());
        }
        return result;
    }
    private static void add(List<ItemStack> result,ItemStack stack,LootParams.Builder context) {
        Float radius=context.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if(radius==null || context.getLevel().random.nextFloat()<=1.0F/radius) result.add(stack);
    }
}
