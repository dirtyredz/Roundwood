package io.github.dirtyredz.roundwood.window;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A single-block framed round window. It stays in the vanilla pane hierarchy ({@link IronBarsBlock},
 * the class vanilla glass panes use in 1.20.1), so panes and other windows connect to it and it
 * connects to them, and waterlogging, bucket use and fluid ticks are inherited. It adds only the
 * plane orientation and its own round geometry.
 */
public class RoundWindowBlock extends IronBarsBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public RoundWindowBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // Face the placing player: the plane runs across their line of sight.
        Direction.Axis axis = context.getHorizontalDirection().getClockWise().getAxis();
        return state == null ? null : state.setValue(AXIS, axis);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            rotated = rotated.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return rotated;
    }

    private static VoxelShape shape(BlockState state) {
        return WindowGeometry.shape(state.getValue(AXIS) == Direction.Axis.Z,
                state.getValue(NORTH), state.getValue(EAST), state.getValue(SOUTH), state.getValue(WEST));
    }
}
