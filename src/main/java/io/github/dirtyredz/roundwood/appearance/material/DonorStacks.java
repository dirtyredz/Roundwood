package io.github.dirtyredz.roundwood.appearance.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Shared donor-stack rule: a stack carrying placed contents or a non-default block state is never a donor. */
final class DonorStacks {
    private DonorStacks() {
    }

    static boolean carriesPayload(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && (tag.contains(BlockItem.BLOCK_ENTITY_TAG) || tag.contains(BlockItem.BLOCK_STATE_TAG));
    }
}
