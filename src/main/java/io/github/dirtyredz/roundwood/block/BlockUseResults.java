package io.github.dirtyredz.roundwood.block;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

/**
 * Adapts Roundwood's per-hand block routers to 1.21's split block use. Minecraft now asks the block once per
 * hand with that hand's stack ({@code useItemOn}, empty stacks included) and, only when the main hand passes to
 * the default interaction, once more without an item ({@code useWithoutItem}). The routers keep the 1.20.1
 * contract (called per hand; {@code PASS} means "the item's own use"), so every Roundwood block routes through
 * {@code useItemOn} and maps a pass to {@link ItemInteractionResult#SKIP_DEFAULT_BLOCK_INTERACTION}: the router
 * never runs twice for one click and the item still gets its own use.
 */
public final class BlockUseResults {
    private BlockUseResults() {
    }

    public static ItemInteractionResult itemUse(InteractionResult routed) {
        return switch (routed) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            case PASS -> ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        };
    }
}
