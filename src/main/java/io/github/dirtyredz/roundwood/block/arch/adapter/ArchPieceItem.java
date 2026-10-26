package io.github.dirtyredz.roundwood.block.arch.adapter;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Styled modular product; item use recomputes and installs one complete placement proposal. */
public final class ArchPieceItem extends AppearanceBlockItem {
    public ArchPieceItem(ArchPieceBlock block, Properties properties) {
        super(block, properties, AppearanceFamily.TRIM, Appearance.DEFAULT, null);
    }
    public ArchPieceBlock piece() { return (ArchPieceBlock) getBlock(); }
    @Override public InteractionResult place(BlockPlaceContext context) { return ArchPiecePlacement.place(this, context); }
}
