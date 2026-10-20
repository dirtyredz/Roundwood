package io.github.dirtyredz.roundwood.door;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Places a whole 2x2 assembly; all checks and writes live in {@link DoorPlacement}. */
public class RoundDoorItem extends BlockItem {
    public RoundDoorItem(RoundDoorBlock block, Properties properties) {
        super(block, properties);
    }

    public RoundDoorBlock door() {
        return (RoundDoorBlock) getBlock();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return DoorPlacement.place(this, context);
    }
}
