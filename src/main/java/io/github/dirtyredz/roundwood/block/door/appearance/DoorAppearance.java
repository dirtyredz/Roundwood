package io.github.dirtyredz.roundwood.block.door.appearance;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.assembly.appearance.AssemblyAppearanceTarget;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorBlockEntities;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Optional;

/**
 * A door's appearance lives once, on the anchor's block entity, and is read from any part through
 * the anchor (never loading it). This resolves it for picks, drops and rendering, with the block's
 * default when the anchor is not available, and offers the whole loaded, complete assembly as a
 * restyling target ({@link AssemblyAppearanceTarget}). Placement installs it itself.
 */
public final class DoorAppearance {
    private DoorAppearance() {
    }

    /** The appearance of the assembly {@code pos} belongs to; the block's default when the anchor is not available. */
    public static Appearance of(BlockGetter level, BlockPos pos, BlockState state) {
        return DoorBlockEntities.anchor(level, pos, state).map(RoundDoorBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    /** For loot: the anchor as found in the world, else the entity captured with the broken part (if it was the anchor). */
    public static Appearance forDrops(LootParams.Builder builder, BlockState state) {
        return DoorBlockEntities.forLoot(builder, state).map(RoundDoorBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    /** The whole door as a restyling target, if it is loaded and complete; announced at the clicked part. */
    public static Optional<AppearanceActions.Target> target(AssemblyDoorBlock block, BlockState state, Level level, BlockPos pos) {
        return AssemblyAppearanceTarget.of(level, pos, block, DoorLayout.of(pos, state), RoundDoorBlockEntity.class);
    }
}
