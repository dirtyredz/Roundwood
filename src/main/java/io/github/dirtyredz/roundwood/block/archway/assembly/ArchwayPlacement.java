package io.github.dirtyredz.roundwood.block.archway.assembly;

import net.minecraft.world.InteractionResult;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacement;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyInstallation;
import io.github.dirtyredz.roundwood.block.archway.ArchwayItem;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Optional;

/**
 * What placing an archway would do, decided once, read-only (0.12.0): the assembly (anchor, facing,
 * design) and the state every cell gets. The server's placement writes exactly these states
 * ({@link ArchwayPlacement}); the client's preview draws exactly this plan's assembly
 * ({@code client/archway/ArchwayPlacementPreview}), so what is shown is what is placed. No plan means
 * the click places nothing and nothing is previewed. The checks are the shared skeleton's
 * ({@link AssemblyPlacementPlan}).
 *
 * <p>An ordinary click: the clicked (or replaceable) cell is the lower-left anchor as the placer sees
 * it, and the archway extends right and up, facing the placer's horizontal direction (four facings,
 * always upright, no hinge). Every cell is checked: world bounds and border, loaded, the player's
 * permission, replaceable, dry (archways are never waterlogged), and unobstructed by entities (only
 * the frame counts: someone standing in the passage does not block it); both lower cells need a
 * sturdy floor. Sneaking changes nothing.
 */
public final class ArchwayPlacement {
    private ArchwayPlacement() {}

    /** The plan for using an archway item in {@code context}, or empty when the click would place nothing. */
    public static Optional<AssemblyPlacementPlan<ArchwayPart, ArchwayLayout.Assembly>> of(ArchwayBlock block, BlockPlaceContext context) {
        ArchwayLayout.Assembly assembly = new ArchwayLayout.Assembly(context.getClickedPos().immutable(),
                context.getHorizontalDirection(), block.design());
        if (!AssemblyPlacementPlan.playerCells(context, assembly, pos -> {
        })) {
            return Optional.empty();
        }
        Player player = context.getPlayer();
        return plan(context.getLevel(), block, assembly, player == null ? CollisionContext.empty() : CollisionContext.of(player));
    }

    /**
     * The plan for an archway placed without a player or item (the showcase builder, other commands):
     * the same bounds, loaded, replaceable, dry, unobstructed and floor checks. Empty when
     * {@code assembly} is not {@code block}'s design.
     */
    public static Optional<AssemblyPlacementPlan<ArchwayPart, ArchwayLayout.Assembly>> at(Level level, ArchwayBlock block, ArchwayLayout.Assembly assembly) {
        if (assembly.design() != block.design() || !AssemblyPlacementPlan.playerlessCells(level, assembly)) {
            return Optional.empty();
        }
        return plan(level, block, assembly, CollisionContext.empty());
    }

    /** Every part's state, if none is obstructed and both lower cells have a sturdy floor. */
    private static Optional<AssemblyPlacementPlan<ArchwayPart, ArchwayLayout.Assembly>> plan(Level level, ArchwayBlock block, ArchwayLayout.Assembly assembly,
                                                       CollisionContext collision) {
        return AssemblyPlacementPlan.of(assembly, part -> assembly.stateFor(part, block),
                        AssemblyPlacementPlan.unobstructed(level, collision, ArchwayPart::lower));
    }

    public static InteractionResult place(ArchwayItem item, BlockPlaceContext context) {
        return AssemblyPlacement.place(context, of(item.archway(), context),
                ArchwayBlockEntity.class);
    }

    /**
     * Places a complete archway without a player or item (the showcase builder, other commands),
     * through {@link ArchwayPlacement#at}: returns false, changing nothing, if any check or write fails.
     */
    public static boolean placeAt(Level level, ArchwayBlock block, ArchwayLayout.Assembly assembly, Appearance appearance) {
        Optional<AssemblyPlacementPlan<ArchwayPart, ArchwayLayout.Assembly>> plan = at(level, block, assembly);
        return plan.isPresent() && AssemblyInstallation.install(level, plan.get(), appearance, ArchwayBlockEntity.class);
    }
}
