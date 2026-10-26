package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorColumn;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.world.level.block.Block;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.function.Supplier;

/**
 * One quarter of a round door (the {@link DoorDesign#ROUND} 2 x 2 assembly). Everything a door part
 * does is the shared {@link AssemblyDoorBlock}; this adds only the {@link #COLUMN} identity, which the
 * round door saves (unchanged since 0.2.0) because it is two columns wide. The 0.4.0 aliases and the
 * 0.7.0 frame migration recognise exactly this class.
 */
public class RoundDoorBlock extends AssemblyDoorBlock {
    public static final EnumProperty<DoorColumn> COLUMN = EnumProperty.create("column", DoorColumn.class);

    public RoundDoorBlock(Properties properties, BlockSetType type, Supplier<BlockEntityType<RoundDoorBlockEntity>> entityType) {
        super(properties, type, entityType);
        // Extend the inherited default (never stateDefinition.any()): other mods inject properties into DoorBlock.
        registerDefaultState(defaultBlockState().setValue(COLUMN, DoorColumn.LEFT).setValue(DoorDepth.PROPERTY, DoorDepth.FRONT));
    }

    @Override
    public DoorDesign design() {
        return DoorDesign.ROUND;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLUMN, DoorDepth.PROPERTY);
    }

    /** Mirroring swaps which column is on the left as well as the physical hinge (vanilla cycles the hinge). */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored : mirrored.cycle(COLUMN);
    }
}
