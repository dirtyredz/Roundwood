package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPlacement;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The round window: a square wooden pane with round glazing, plain or crossed (fixed per registration). Its state keeps the 0.5.0 encoding
 * (the four connections, {@code waterlogged} and {@link #AXIS}) and adds {@link #POSITION}: centred
 * (the default, so windows saved before 0.6.0 load centred and {@code axis} keeps its meaning) or
 * flush against one of the six sides of the cell, which alone fixes the plane.
 */
public class RoundWindowBlock extends WindowPaneBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final EnumProperty<WindowPosition> POSITION = EnumProperty.create("position", WindowPosition.class);

    public RoundWindowBlock(Properties properties, WindowPattern pattern, Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType) {
        super(properties, new WindowDesign(WindowProfile.ROUND, pattern), entityType);
        // Extend the inherited default (connections off, dry) rather than replacing it.
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X).setValue(POSITION, WindowPosition.CENTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS, POSITION);
    }

    @Override
    public WindowPose pose(BlockState state) {
        return state.getValue(POSITION).pose(state.getValue(AXIS));
    }

    /** The targeted side, or centred across the player's line of sight. */
    @Override
    protected BlockState placed(BlockState state, WindowPlacement.Request request) {
        return state.setValue(AXIS, WindowPlacement.roundAxis(request))
                .setValue(POSITION, WindowPlacement.roundPosition(request));
    }

    /** Connections turn with the block (inherited); a quarter turn also swaps the axis, and a flush side turns. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            rotated = rotated.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        Optional<Direction> side = state.getValue(POSITION).side();
        return side.isEmpty() ? rotated : rotated.setValue(POSITION, WindowPosition.flush(rotation.rotate(side.get())));
    }

    /** Connections mirror (inherited); the axis is unchanged and a flush side mirrors. */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        Optional<Direction> side = state.getValue(POSITION).side();
        return side.isEmpty() ? mirrored : mirrored.setValue(POSITION, WindowPosition.flush(mirror.mirror(side.get())));
    }
}
