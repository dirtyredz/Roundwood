package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Places a whole 2x2 frame, fitted to a door or freestanding; all checks and writes live in {@link TrimPlacement}. */
public class RoundTrimItem extends BlockItem {
    public RoundTrimItem(RoundTrimBlock block, Properties properties) {
        super(block, properties);
    }

    public RoundTrimBlock trim() {
        return (RoundTrimBlock) getBlock();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return TrimPlacement.place(this, context);
    }
}
