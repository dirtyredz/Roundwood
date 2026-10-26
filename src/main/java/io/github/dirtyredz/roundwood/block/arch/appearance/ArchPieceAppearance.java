package io.github.dirtyredz.roundwood.block.arch.appearance;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyAnchors;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyAppearanceTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.Optional;

/** Typed adapter to shared loaded-anchor lookup and whole-piece restyling. */
public final class ArchPieceAppearance {
    public static final AssemblyAnchors.Lookup<ArchPieceLayout.Assembly, ArchPieceBlockEntity> ANCHORS =
            new AssemblyAnchors.Lookup<>(state -> state.getBlock() instanceof ArchPieceBlock block
                    && state.getValue(ArchPieceBlock.CELL).in(block.design()), ArchPieceLayout::of, ArchPieceBlockEntity.class);
    private ArchPieceAppearance() {}
    public static Appearance of(BlockGetter level, BlockPos pos, BlockState state) {
        return ANCHORS.anchor(level, pos, state).map(ArchPieceBlockEntity::appearance).orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }
    public static Appearance forDrops(LootParams.Builder builder, BlockState state) {
        return ANCHORS.forLoot(builder, state).map(ArchPieceBlockEntity::appearance).orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }
    public static Optional<AppearanceActions.Target> target(ArchPieceBlock block, BlockState state, Level level, BlockPos pos) {
        return AssemblyAppearanceTarget.of(level, pos, block, ArchPieceLayout.of(pos, state), ArchPieceBlockEntity.class);
    }
}
