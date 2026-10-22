package io.github.dirtyredz.roundwood.block.archway;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimNbt;
import io.github.dirtyredz.roundwood.block.attachment.TrimAttachmentSource;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.entity.AppearanceAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * The anchor (lower-left cell) of an archway (0.12.0): the one place its look (a trim appearance) and
 * its two face trims ({@link ArchwayTrimNbt}) are kept, and where the client draws all four cells
 * from. Never ticks. Persistence and synchronisation are the shared anchor's
 * ({@link AppearanceAnchorBlockEntity}): loading and update packets always replace both, so a change
 * back to the default (paint removed, oak again, a trim sheared off) reaches clients.
 */
public class ArchwayBlockEntity extends AppearanceAnchorBlockEntity implements TrimAttachmentSource {
    public ArchwayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, AppearanceFamily.TRIM, Map.of(TrimSlot.PERIMETER, ArchwayTrimNbt.FORMAT));
    }

    /** The four cells and the casings standing proud of them, so the archway is drawn whenever any of it is in view. */
    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof ArchwayBlock ? ArchwayLayout.of(getBlockPos(), state).renderBounds() : new AABB(getBlockPos());
    }
}
