package io.github.dirtyredz.roundwood.door;

import io.github.dirtyredz.roundwood.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.door.leaf.DoorSwing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The anchor (lower-left part) of every assembly carries this entity, so the client has one place
 * to draw the whole leaf from. It stores nothing and never ticks: the blockstate stays authoritative
 * for open/closed, and the client-side {@link DoorSwing} only animates between states it is told
 * about through {@link #setBlockState}.
 */
public class RoundDoorBlockEntity extends BlockEntity {
    private final DoorSwing swing;

    public RoundDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Whatever state the entity is created in (placement, chunk load) is drawn at rest.
        this.swing = new DoorSwing(open(state));
    }

    public DoorSwing swing() {
        return swing;
    }

    /** Called by the chunk whenever this cell's state changes; on the client an open change starts a swing. */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        boolean was = open(getBlockState());
        super.setBlockState(state);
        boolean now = open(state);
        if (level != null && level.isClientSide && now != was) {
            swing.retarget(now, level.getGameTime());
        }
    }

    /** Both resting poses and the sweep between them, so the leaf is not culled while only its projection is in view. */
    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof RoundDoorBlock) {
            return DoorLayout.of(getBlockPos(), state).sweepBounds();
        }
        return new AABB(getBlockPos());
    }

    private static boolean open(BlockState state) {
        return state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN);
    }
}
