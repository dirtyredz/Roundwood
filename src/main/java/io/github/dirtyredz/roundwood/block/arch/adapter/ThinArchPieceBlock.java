package io.github.dirtyredz.roundwood.block.arch.adapter;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import java.util.function.Supplier;

/** Thin product adapter; lifecycle and placement remain shared with full pieces. */
public final class ThinArchPieceBlock extends ArchPieceBlock {
    public ThinArchPieceBlock(Properties properties, ArchPieceDesign design, Supplier<BlockEntityType<ArchPieceBlockEntity>> entityType) {
        super(properties, design, entityType);
        registerDefaultState(defaultBlockState().setValue(ArchPieceDepth.PROPERTY, ArchPieceDepth.FRONT));
    }
    @Override public ArchPieceThickness thickness() { return ArchPieceThickness.THIN; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ArchPieceDepth.PROPERTY);
    }
}
