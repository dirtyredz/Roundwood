package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.attachment.entity.AppearanceAnchorBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * The anchor (lower-left quarter) of a compatibility frame: the one place its appearance is kept, and
 * where the client draws all four quarters from. Never ticks. It declares no attachment store: a frame
 * is never a trim host. Persistence and synchronisation are the shared anchor's
 * ({@link AppearanceAnchorBlockEntity}): loading and update packets always replace the whole
 * appearance, so a change back to the default (paint removed, oak again) reaches clients.
 */
public class RoundTrimBlockEntity extends AppearanceAnchorBlockEntity {
    public RoundTrimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, AppearanceFamily.TRIM, Map.of());
    }

    /** The four cells, so the frame is drawn whenever any of it is in view. */
    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof RoundTrimBlock) {
            TrimLayout.Assembly assembly = TrimLayout.of(getBlockPos(), state);
            AABB box = new AABB(assembly.anchor());
            for (BlockPos pos : assembly.positions()) {
                box = box.minmax(new AABB(pos));
            }
            return box;
        }
        return new AABB(getBlockPos());
    }
}
