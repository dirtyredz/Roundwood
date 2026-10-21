package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowOrientation;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPlacement;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.function.Supplier;

/**
 * One piece of a round window's upper half ({@link WindowProfile#ARCH_LEFT} or
 * {@link WindowProfile#ARCH_RIGHT}, fixed per registration): an independent single-cell pane, with
 * no partner lookup, that pairs with the other piece only by being placed beside it. Its state
 * encodes the full pose: {@link #ORIENTATION} (front and top, so handedness is stored) and
 * {@link #DEPTH} along the positive world axis of its normal, plus the pane connections and water.
 */
public class RoundWindowArchBlock extends WindowPaneBlock {
    public static final EnumProperty<WindowOrientation> ORIENTATION = EnumProperty.create("orientation", WindowOrientation.class);
    public static final EnumProperty<WindowDepth> DEPTH = EnumProperty.create("depth", WindowDepth.class);

    public RoundWindowArchBlock(Properties properties, WindowProfile profile,
                                Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType) {
        super(properties, profile, entityType);
        if (profile == WindowProfile.ROUND) {
            throw new IllegalArgumentException("an arch block needs an arch profile");
        }
        registerDefaultState(defaultBlockState().setValue(ORIENTATION, WindowOrientation.SOUTH)
                .setValue(DEPTH, WindowDepth.CENTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ORIENTATION, DEPTH);
    }

    @Override
    public WindowPose pose(BlockState state) {
        return state.getValue(ORIENTATION).pose(state.getValue(DEPTH));
    }

    public static BlockState withPose(BlockState state, WindowPose pose) {
        return state.setValue(ORIENTATION, WindowOrientation.of(pose)).setValue(DEPTH, pose.depth());
    }

    @Override
    protected BlockState placed(BlockState state, WindowPlacement.Request request) {
        return withPose(state, WindowPlacement.archPose(request));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        int turns = switch (rotation) {
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
            default -> 0;
        };
        return withPose(super.rotate(state, rotation), pose(state).rotateY(turns));
    }

    /** The geometry is reflected; the piece stays a left (or right) piece, now seen from its other face. */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return switch (mirror) {
            case LEFT_RIGHT -> withPose(mirrored, pose(state).mirror(Direction.Axis.Z));
            case FRONT_BACK -> withPose(mirrored, pose(state).mirror(Direction.Axis.X));
            default -> mirrored;
        };
    }
}
