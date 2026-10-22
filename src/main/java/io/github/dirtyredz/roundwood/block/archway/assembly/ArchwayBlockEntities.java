package io.github.dirtyredz.roundwood.block.archway.assembly;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyAnchors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Optional;

/**
 * An archway's anchor entity from any cell, typed (0.12.0; the lookup itself is {@link AssemblyAnchors}'),
 * shared by its appearance, its face trims and its loot: an archway block whose reconstructed layout is
 * the cell's, in a loaded cell. For loot, the anchor as found in the world, else the entity captured
 * with the broken cell.
 */
public final class ArchwayBlockEntities {
    /** Either archway design; the layout from a cell's own state. */
    private static final AssemblyAnchors.Family<ArchwayLayout.Assembly> FAMILY =
            new AssemblyAnchors.Family<>(state -> state.getBlock() instanceof ArchwayBlock, ArchwayLayout::of);

    private ArchwayBlockEntities() {
    }

    /** The anchor entity of the archway {@code pos} belongs to, if it is there. */
    public static Optional<ArchwayBlockEntity> anchor(BlockGetter level, BlockPos pos, BlockState state) {
        return AssemblyAnchors.anchor(level, pos, state, FAMILY, ArchwayBlockEntity.class);
    }

    /** For loot from any cell: the anchor as found in the world, else the captured entity if it is an archway anchor. */
    public static Optional<ArchwayBlockEntity> forLoot(LootParams.Builder builder, BlockState state) {
        return AssemblyAnchors.forLoot(builder, state, FAMILY, ArchwayBlockEntity.class);
    }
}
