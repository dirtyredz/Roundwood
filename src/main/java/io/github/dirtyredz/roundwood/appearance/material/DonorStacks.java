package io.github.dirtyredz.roundwood.appearance.material;

import io.github.dirtyredz.roundwood.appearance.item.StackPayloads;
import net.minecraft.world.item.ItemStack;

/**
 * Shared donor-stack rule: a stack carrying placed contents (native {@code block_entity_data}) or a
 * non-default block state ({@code block_state}) is never a donor.
 */
final class DonorStacks {
    private DonorStacks() {
    }

    static boolean carriesPayload(ItemStack stack) {
        return StackPayloads.hasPlacedBlockPayload(stack);
    }
}
