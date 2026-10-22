package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.block.assembly.AssemblyAnchors;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Optional;

/**
 * A door's anchor entity from any part, typed (0.8.0; the lookup itself is {@link AssemblyAnchors}'):
 * any door block (canonical or alias) whose reconstructed layout is the part's, in a loaded cell. For
 * loot, the anchor as found in the world, else the entity captured with the broken part.
 */
public final class DoorBlockEntities {
    /** Any door block of either design, canonical or alias; the layout from a part's own state. */
    private static final AssemblyAnchors.Family<DoorLayout.Assembly> FAMILY =
            new AssemblyAnchors.Family<>(state -> state.getBlock() instanceof AssemblyDoorBlock, DoorLayout::of);

    private DoorBlockEntities() {
    }

    /** The anchor entity of the assembly {@code pos} belongs to, if it is there. */
    public static Optional<RoundDoorBlockEntity> anchor(BlockGetter level, BlockPos pos, BlockState state) {
        return AssemblyAnchors.anchor(level, pos, state, FAMILY, RoundDoorBlockEntity.class);
    }

    /** For loot from any part: the anchor as found in the world, else the captured entity if it is a door anchor. */
    public static Optional<RoundDoorBlockEntity> forLoot(LootParams.Builder builder, BlockState state) {
        return AssemblyAnchors.forLoot(builder, state, FAMILY, RoundDoorBlockEntity.class);
    }
}
