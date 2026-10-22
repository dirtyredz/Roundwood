package io.github.dirtyredz.roundwood.block.archway.appearance;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayBlockEntities;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Optional;

/**
 * An archway's look (0.12.0) lives once, on the anchor's entity, and is read from any cell through it
 * (never loading it): for picks, drops and rendering, and as the whole loaded, complete archway for
 * restyling. Placement installs it itself ({@code assembly/ArchwayPlacement}).
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

    /** The whole archway as a restyling target, if it is loaded and complete. */
    public static Optional<AppearanceActions.Target> target(ArchwayBlock block, BlockState state, Level level, BlockPos pos) {
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)
                || !(level.getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity entity)) {
            return Optional.empty();
        }
        return Optional.of(new AppearanceActions.Target() {
            @Override
            public Optional<Appearance> current() {
                return Optional.of(entity.appearance());
            }

            @Override
            public boolean mayEdit(Player player) {
                return assembly.positions().stream().allMatch(cell -> level.mayInteract(player, cell));
            }

            @Override
            public void set(Appearance appearance) {
                entity.setAppearance(appearance);
            }

            @Override
            public BlockPos origin() {
                return pos;
            }
        });
    }
}
