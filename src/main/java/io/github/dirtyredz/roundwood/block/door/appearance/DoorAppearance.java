package io.github.dirtyredz.roundwood.block.door.appearance;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
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
 * A door's appearance lives once, on the anchor's block entity, and is read from any part through
 * the anchor (never loading it). This resolves it for picks, drops and rendering, and offers the
 * whole loaded, complete assembly as a restyling target. Placement installs it itself
 * ({@code assembly/DoorPlacement}).
 */
public final class DoorAppearance {
    private DoorAppearance() {
    }

    /** The appearance of the assembly {@code pos} belongs to; the block's default when the anchor is not available. */
    public static Appearance of(BlockGetter level, BlockPos pos, BlockState state) {
        return anchorEntity(level, pos, state).map(RoundDoorBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    /** For loot: the anchor as found in the world, else the entity captured with the broken part (if it was the anchor). */
    public static Appearance forDrops(LootParams.Builder builder, BlockState state) {
        Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin != null && state.getBlock() instanceof RoundDoorBlock) {
            Optional<RoundDoorBlockEntity> anchor = anchorEntity(builder.getLevel(), BlockPos.containing(origin), state);
            if (anchor.isPresent()) {
                return anchor.get().appearance();
            }
        }
        BlockEntity captured = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return captured instanceof RoundDoorBlockEntity entity ? entity.appearance() : AppearanceDefaults.of(state.getBlock());
    }

    /** The whole door as a restyling target, if it is loaded and complete. */
    public static Optional<AppearanceActions.Target> target(RoundDoorBlock block, BlockState state, Level level, BlockPos pos) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)
                || !(level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity)) {
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

    private static Optional<RoundDoorBlockEntity> anchorEntity(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof RoundDoorBlock)) {
            return Optional.empty();
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        BlockPos anchor = assembly.anchor();
        if (level instanceof Level live && !live.isLoaded(anchor)) {
            return Optional.empty();
        }
        BlockState anchorState = level.getBlockState(anchor);
        if (!(anchorState.getBlock() instanceof RoundDoorBlock) || !DoorLayout.of(anchor, anchorState).equals(assembly)) {
            return Optional.empty();
        }
        return level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }
}
