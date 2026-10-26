package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import java.util.function.Supplier;

/** Nine or twelve cells, two outer hinges, one shared assembly lifecycle and open state. */
public final class DoubleArchedDoorBlock extends AssemblyDoorBlock {
    public static final IntegerProperty CELL_X = IntegerProperty.create("cell_x", 0, 2);
    public static final IntegerProperty CELL_Y = IntegerProperty.create("cell_y", 0, 3);
    private final DoorDesign design;
    public DoubleArchedDoorBlock(DoorDesign design, Properties properties, BlockSetType type, Supplier<BlockEntityType<RoundDoorBlockEntity>> entityType) {
        super(properties, type, entityType);
        if (!design.doubleLeaf()) throw new IllegalArgumentException("Paired door design required");
        this.design = design;
        registerDefaultState(defaultBlockState().setValue(CELL_X, 0).setValue(CELL_Y, 0).setValue(DoorDepth.PROPERTY, DoorDepth.FRONT));
    }
    @Override public DoorDesign design() { return design; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CELL_X, CELL_Y, DoorDepth.PROPERTY);
    }
    @Override public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        int row = state.getValue(CELL_Y);
        if (row == 0) return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
        var assembly = io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.of(pos, state);
        return assembly.isPart(level.getBlockState(pos.below()), this,
                io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Part.at(state.getValue(CELL_X), row - 1));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored : mirrored.setValue(CELL_X, 2 - state.getValue(CELL_X));
    }
}
