package io.github.dirtyredz.roundwood.migration.item;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayItem;

/** Existing stacks place their original assembly through the unchanged placement path. */
public final class RetiredArchwayItem extends ArchwayItem {
    public RetiredArchwayItem(ArchwayBlock block, Properties properties) { super(block, properties); }
}
