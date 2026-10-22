package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyInstallation;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacement;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Places a complete door or nothing, exactly as its {@link DoorPlacementPlan} says (the plan the
 * client's preview draws): a player's placement through {@link AssemblyPlacement} (one transaction,
 * then the criterion, sound, game event and one item charged), a player-less one straight through
 * {@link AssemblyInstallation} (silent and free). Either way the stack's or the requested look goes on
 * the new anchor in the same transaction, and a write that fails part-way puts every cell back.
 */
public final class DoorPlacement {
    private DoorPlacement() {
    }

    public static InteractionResult place(RoundDoorItem item, BlockPlaceContext context) {
        return AssemblyPlacement.place(context, DoorPlacementPlan.of(item.door(), context).map(DoorPlacementPlan::shared),
                RoundDoorBlockEntity.class);
    }

    /**
     * Places a complete assembly without a player or item (the showcase builder, other commands),
     * through {@link DoorPlacementPlan#at}: returns false, changing nothing, if any check or write
     * fails. The cells are left unpowered, in the requested open state.
     */
    public static boolean placeAt(Level level, AssemblyDoorBlock block, DoorLayout.Assembly assembly, boolean open) {
        return placeAt(level, block, assembly, open, block.defaultAppearance());
    }

    /** As {@link #placeAt(Level, AssemblyDoorBlock, DoorLayout.Assembly, boolean)}, in {@code appearance}. */
    public static boolean placeAt(Level level, AssemblyDoorBlock block, DoorLayout.Assembly assembly, boolean open,
                                  Appearance appearance) {
        Optional<DoorPlacementPlan> plan = DoorPlacementPlan.at(level, block, assembly, open);
        return plan.isPresent() && AssemblyInstallation.install(level, plan.get().shared(), appearance, RoundDoorBlockEntity.class);
    }
}
