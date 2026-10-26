package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.function.Supplier;

/**
 * One half of the arched door (0.11.0, the {@link DoorDesign#ARCHED} 1 x 2 assembly): the vanilla
 * door's own upper/lower identity and nothing else (no {@code column}: a one-column door has no
 * fictitious right column). Mirroring is vanilla's (the hinge flips). Everything a door part does is
 * the shared {@link AssemblyDoorBlock}.
 */
public class ArchedDoorBlock extends AssemblyDoorBlock {
    public ArchedDoorBlock(Properties properties, BlockSetType type, Supplier<BlockEntityType<RoundDoorBlockEntity>> entityType) {
        super(properties, type, entityType);
        registerDefaultState(defaultBlockState().setValue(DoorDepth.PROPERTY, DoorDepth.FRONT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DoorDepth.PROPERTY);
    }

    @Override
    public DoorDesign design() {
        return DoorDesign.ARCHED;
    }
}
