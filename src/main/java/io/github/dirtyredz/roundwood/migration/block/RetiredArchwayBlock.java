package io.github.dirtyredz.roundwood.migration.block;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;

/** Compatibility identity only: saved states and callbacks retain the 0.13.3 contract. */
public final class RetiredArchwayBlock extends ArchwayBlock {
    public RetiredArchwayBlock(Properties properties, ArchwayDesign design,
                               Supplier<BlockEntityType<ArchwayBlockEntity>> entityType) {
        super(properties, design, entityType);
    }
}
