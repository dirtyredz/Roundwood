package io.github.dirtyredz.roundwood.block.door.appearance;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorBlockEntities;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

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
        return DoorBlockEntities.anchor(level, pos, state).map(RoundDoorBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    /** For loot: the anchor as found in the world, else the entity captured with the broken part (if it was the anchor). */
    public static Appearance forDrops(LootParams.Builder builder, BlockState state) {
        return DoorBlockEntities.forLoot(builder, state).map(RoundDoorBlockEntity::appearance)
                .orElseGet(() -> AppearanceDefaults.of(state.getBlock()));
    }

    /** The whole door as a restyling target, if it is loaded and complete. */
    public static Optional<AppearanceActions.Target> target(AssemblyDoorBlock block, BlockState state, Level level, BlockPos pos) {
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
}
