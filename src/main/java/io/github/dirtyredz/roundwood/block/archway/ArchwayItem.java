package io.github.dirtyredz.roundwood.block.archway;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPlacement;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Places a whole archway (0.12.0, ordinary or keyhole) in the stack's look with an ordinary click; all
 * checks and writes live in {@link ArchwayPlacement}, from one placement plan the client's preview
 * draws too. A trim-family item: wood, paint or one of the four masonry materials.
 */
public class ArchwayItem extends AppearanceBlockItem {
    public ArchwayItem(ArchwayBlock block, Properties properties) {
        super(block, properties, AppearanceFamily.TRIM, Appearance.DEFAULT, null);
    }

    public ArchwayBlock archway() {
        return (ArchwayBlock) getBlock();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return ArchwayPlacement.place(this, context);
    }
}
