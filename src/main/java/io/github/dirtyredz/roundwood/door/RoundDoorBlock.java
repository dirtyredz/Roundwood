package io.github.dirtyredz.roundwood.door;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * One quarter of a round door. Stays a {@link DoorBlock} (same properties, wooden-door tags, hand
 * opening) so villager AI, pathfinding and other mods see an ordinary wooden door; the callbacks
 * here only hand off to the assembly services.
 */
public class RoundDoorBlock extends DoorBlock {
    public RoundDoorBlock(Properties properties, BlockSetType type) {
        super(properties, type);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return DoorGeometry.shape(state.getValue(FACING), state.getValue(HINGE), state.getValue(HALF), state.getValue(OPEN));
    }

    /** Assemblies are placed only through {@link RoundDoorItem}; there is no single-cell placement. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    /** The item writes all four cells itself; the vanilla upper-half write must not run. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return DoorStateController.use(this, state, level, pos, player);
    }

    @Override
    public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
        DoorStateController.setOpen(this, state, level, pos, open, entity);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving) {
        DoorStateController.neighborChanged(this, state, level, pos, source);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        DoorRemoval.onRemove(this, state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moving);
    }
}
