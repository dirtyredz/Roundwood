package io.github.dirtyredz.roundwood.block.arch.adapter;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;

/** Styled modular product; placement is wired by the following placement batch. */
public final class ArchPieceItem extends AppearanceBlockItem {
    public ArchPieceItem(ArchPieceBlock block, Properties properties) {
        super(block, properties, AppearanceFamily.TRIM, Appearance.DEFAULT, null);
    }
    public ArchPieceBlock piece() { return (ArchPieceBlock) getBlock(); }
}
