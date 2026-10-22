package io.github.dirtyredz.roundwood.block.archway.appearance;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayBlockEntities;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.assembly.appearance.AssemblyAppearanceTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Optional;

/**
 * An archway's look (0.12.0) lives once, on the anchor's entity, and is read from any cell through it
 * (never loading it): for picks, drops and rendering, and as the whole loaded, complete archway for
 * restyling ({@link AssemblyAppearanceTarget}). Placement installs it itself.
 */
public final class ArchwayAppearance {
    private ArchwayAppearance() {
    }

    public static Appearance of(BlockGetter level, BlockPos pos, BlockState state) {
        return ArchwayBlockEntities.anchor(level, pos, state).map(ArchwayBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    public static Appearance forDrops(LootParams.Builder builder, BlockState state) {
        return ArchwayBlockEntities.forLoot(builder, state).map(ArchwayBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    /** The whole archway as a restyling target, if it is loaded and complete; announced at the clicked cell. */
    public static Optional<AppearanceActions.Target> target(ArchwayBlock block, BlockState state, Level level, BlockPos pos) {
        return AssemblyAppearanceTarget.of(level, pos, block, ArchwayLayout.of(pos, state), ArchwayBlockEntity.class);
    }
}
