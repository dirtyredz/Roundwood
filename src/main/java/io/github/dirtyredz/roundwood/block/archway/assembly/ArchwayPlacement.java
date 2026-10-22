package io.github.dirtyredz.roundwood.block.archway.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.ArchwayItem;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyInstallation;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacement;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Places a complete archway or nothing, exactly as its {@link ArchwayPlacementPlan} says (the plan the
 * client's preview draws), like the doors: a player's placement through {@link AssemblyPlacement}, a
 * player-less one straight through {@link AssemblyInstallation} (silent and free); the look goes on the
 * new anchor in the same transaction, and a write that fails part-way puts every cell back.
 */
public final class ArchwayPlacement {
    private ArchwayPlacement() {
    }

    public static InteractionResult place(ArchwayItem item, BlockPlaceContext context) {
        return AssemblyPlacement.place(context, ArchwayPlacementPlan.of(item.archway(), context).map(ArchwayPlacementPlan::shared),
                ArchwayBlockEntity.class);
    }

    /**
     * Places a complete archway without a player or item (the showcase builder, other commands),
     * through {@link ArchwayPlacementPlan#at}: returns false, changing nothing, if any check or write fails.
     */
    public static boolean placeAt(Level level, ArchwayBlock block, ArchwayLayout.Assembly assembly, Appearance appearance) {
        Optional<ArchwayPlacementPlan> plan = ArchwayPlacementPlan.at(level, block, assembly);
        return plan.isPresent() && AssemblyInstallation.install(level, plan.get().shared(), appearance, ArchwayBlockEntity.class);
    }
}
