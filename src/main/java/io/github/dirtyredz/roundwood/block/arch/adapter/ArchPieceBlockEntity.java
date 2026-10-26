package io.github.dirtyredz.roundwood.block.arch.adapter;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.Map;

/** One appearance owner per piece; attachment declarations join this in the fitting batch. */
public final class ArchPieceBlockEntity extends AppearanceAnchorBlockEntity {
    public ArchPieceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, AppearanceFamily.TRIM, Map.of());
    }
    @Override public AABB getRenderBoundingBox() {
        return getBlockState().getBlock() instanceof ArchPieceBlock
                ? ArchPieceLayout.of(getBlockPos(), getBlockState()).renderBounds() : new AABB(getBlockPos());
    }
}
