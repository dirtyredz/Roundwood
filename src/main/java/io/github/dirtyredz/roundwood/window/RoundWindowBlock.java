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
 * A single-block framed round window: a thin square wooden pane with round glazing. It stays in the
 * vanilla pane hierarchy ({@link IronBarsBlock}, the class glass panes use in 1.20.1), so panes,
 * bars, walls and other windows connect to it and it connects to them, and waterlogging, buckets,
 * fluid ticks and water left behind are inherited. It adds the plane orientation ({@link #AXIS},
 * facing the placing player) and its own geometry ({@link WindowShapes}, models from datagen).
 */
public class RoundWindowBlock extends IronBarsBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public RoundWindowBlock(Properties properties) {
        super(properties);
        // Extend the inherited default (connections off, dry) rather than replacing it.
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    /** Vanilla pane connections and waterlogging, facing the placing player: the plane runs across their line of sight. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction.Axis axis = context.getHorizontalDirection().getClockWise().getAxis();
        return state == null ? null : state.setValue(AXIS, axis);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WindowShapes.shape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WindowShapes.shape(state);
    }

    /** Connections turn with the block (inherited); a quarter turn also swaps the plane's axis. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            rotated = rotated.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return rotated;
    }

    /**
     * Vanilla panes hide every face towards a pane of the same block above or below. A window above
     * with the other axis covers only a 2 x 2 square of this one's top, so faces are hidden only
     * between windows in the same plane.
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        if (direction.getAxis().isVertical() && adjacent.is(this)) {
            return adjacent.getValue(AXIS) == state.getValue(AXIS);
        }
        return super.skipRendering(state, adjacent, direction);
    }
}
