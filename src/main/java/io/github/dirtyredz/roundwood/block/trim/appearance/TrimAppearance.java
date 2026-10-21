package io.github.dirtyredz.roundwood.block.trim.appearance;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A frame's appearance lives once, on the anchor quarter's block entity, and is read from any
 * quarter through it (never loading it): for picks, drops and rendering, and as the whole loaded,
 * complete frame for restyling. Placement installs it itself ({@code assembly/TrimPlacement}).
 */
public final class TrimAppearance {
    private TrimAppearance() {
    }

    public static Appearance of(BlockGetter level, BlockPos pos, BlockState state) {
        return anchorEntity(level, pos, state).map(RoundTrimBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    public static Appearance forDrops(LootParams.Builder builder, BlockState state) {
        Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin != null && state.getBlock() instanceof RoundTrimBlock) {
            Optional<RoundTrimBlockEntity> anchor = anchorEntity(builder.getLevel(), BlockPos.containing(origin), state);
            if (anchor.isPresent()) {
                return anchor.get().appearance();
            }
        }
        BlockEntity captured = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return captured instanceof RoundTrimBlockEntity entity ? entity.appearance() : AppearanceDefaults.of(state.getBlock());
    }

    public static Optional<AppearanceActions.Target> target(RoundTrimBlock block, BlockState state, Level level, BlockPos pos) {
        TrimLayout.Assembly assembly = TrimLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)
                || !(level.getBlockEntity(assembly.anchor()) instanceof RoundTrimBlockEntity entity)) {
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

    private static Optional<RoundTrimBlockEntity> anchorEntity(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof RoundTrimBlock)) {
            return Optional.empty();
        }
        TrimLayout.Assembly assembly = TrimLayout.of(pos, state);
        BlockPos anchor = assembly.anchor();
        if (level instanceof Level live && !live.isLoaded(anchor)) {
            return Optional.empty();
        }
        BlockState anchorState = level.getBlockState(anchor);
        if (!(anchorState.getBlock() instanceof RoundTrimBlock) || !TrimLayout.of(anchor, anchorState).equals(assembly)) {
            return Optional.empty();
        }
        return level.getBlockEntity(anchor) instanceof RoundTrimBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }
}
