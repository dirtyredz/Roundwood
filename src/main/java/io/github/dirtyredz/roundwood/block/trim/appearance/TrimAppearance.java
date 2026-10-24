package io.github.dirtyredz.roundwood.block.trim.appearance;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyAnchors;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyAppearanceTarget;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Optional;

/**
 * A frame's appearance lives once, on the anchor quarter's block entity, and is read from any
 * quarter through it ({@link AssemblyAnchors}, never loading it): for picks, drops and rendering, and
 * as the whole loaded, complete frame for restyling ({@link AssemblyAppearanceTarget}). Placement
 * installs it itself ({@code assembly/TrimPlacement}).
 */
public final class TrimAppearance {
    /** Any frame block, canonical or alias; the layout from a quarter's own state. */
    public static final AssemblyAnchors.Lookup<TrimLayout.Assembly, RoundTrimBlockEntity> ANCHORS =
            new AssemblyAnchors.Lookup<>(state -> state.getBlock() instanceof RoundTrimBlock, TrimLayout::of, RoundTrimBlockEntity.class);

    private TrimAppearance() {
    }

    public static Appearance of(BlockGetter level, BlockPos pos, BlockState state) {
        return ANCHORS.anchor(level, pos, state).map(RoundTrimBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    public static Appearance forDrops(LootParams.Builder builder, BlockState state) {
        return ANCHORS.forLoot(builder, state).map(RoundTrimBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    /** The whole frame as a restyling target, if it is loaded and complete; announced at the clicked quarter. */
    public static Optional<AppearanceActions.Target> target(RoundTrimBlock block, BlockState state, Level level, BlockPos pos) {
        return AssemblyAppearanceTarget.of(level, pos, block, TrimLayout.of(pos, state), RoundTrimBlockEntity.class);
    }
}
