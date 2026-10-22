package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Map;
import java.util.Optional;

/**
 * What placing a door would do, decided once, read-only (0.11.0): the assembly (anchor, facing,
 * hinge, design) and the state every cell gets. The server's placement writes exactly these states
 * ({@link DoorPlacement}); the client's preview draws exactly this plan's assembly
 * ({@code selection/DoorPlacementOutline}), so what is shown is what is placed. No plan means the
 * click places nothing and nothing is previewed. The checks are the shared skeleton's
 * ({@link AssemblyPlacementPlan}); the door adds its hinge choice, its power and its policy.
 *
 * <p>The clicked (or replaceable) cell is the lower anchor and the assembly extends up and to the
 * right; the hinge follows {@link DoorHingePlacement} at the footprint's real outer edges. Every cell
 * is checked: world bounds and border, loaded, the player's permission, replaceable, dry, free of
 * entities, and each column standing on a sturdy floor. A powered cell places the door open and
 * powered. Sneaking changes nothing.
 */
public record DoorPlacementPlan(DoorLayout.Assembly assembly, Map<DoorLayout.Part, BlockState> states) {
    public DoorPlacementPlan {
        states = Map.copyOf(states);
    }

    /** The plan for using a door item in {@code context}, or empty when the click would place nothing. */
    public static Optional<DoorPlacementPlan> of(AssemblyDoorBlock block, BlockPlaceContext context) {
        if (!context.canPlace()) {
            return Optional.empty();
        }
        Direction facing = context.getHorizontalDirection();
        BlockPos anchor = context.getClickedPos().immutable();
        DoorDesign design = block.design();
        DoorLayout.Assembly assembly = new DoorLayout.Assembly(anchor, facing,
                DoorHingePlacement.choose(block, context, anchor, facing, design.columns()), design);
        Level level = context.getLevel();
        boolean[] powered = {false};
        if (!AssemblyPlacementPlan.playerCells(context, assembly, pos -> powered[0] |= level.hasNeighborSignal(pos))) {
            return Optional.empty();
        }
        Player player = context.getPlayer();
        return plan(level, block, assembly, powered[0], powered[0], player == null ? CollisionContext.empty() : CollisionContext.of(player));
    }

    /**
     * The plan for a door placed without a player or item (the showcase builder, other commands): the
     * same bounds, loaded, replaceable, dry, unobstructed and floor checks; the cells unpowered, in
     * the requested open state. Empty when {@code assembly} is not {@code block}'s design.
     */
    public static Optional<DoorPlacementPlan> at(Level level, AssemblyDoorBlock block, DoorLayout.Assembly assembly, boolean open) {
        if (assembly.design() != block.design() || !AssemblyPlacementPlan.playerlessCells(level, assembly)) {
            return Optional.empty();
        }
        return plan(level, block, assembly, open, false, CollisionContext.empty());
    }

    /** The anchor's state (the lower-left part). */
    public BlockState anchorState() {
        return states.get(DoorLayout.Part.LOWER_LEFT);
    }

    /** True when the door would be placed open (a powered spot opens it at once). */
    public boolean open() {
        return anchorState().getValue(DoorBlock.OPEN);
    }

    /** The same decision as the shared plan the installer writes. */
    public AssemblyPlacementPlan<DoorLayout.Part> shared() {
        return new AssemblyPlacementPlan<>(assembly, states);
    }

    /** Every part's state, if none is obstructed and each column has a sturdy floor. */
    private static Optional<DoorPlacementPlan> plan(Level level, AssemblyDoorBlock block, DoorLayout.Assembly assembly,
                                                    boolean open, boolean powered, CollisionContext collision) {
        return AssemblyPlacementPlan.of(assembly, part -> assembly.stateFor(part, block, open, powered),
                        AssemblyPlacementPlan.unobstructed(level, collision, part -> part.half == DoubleBlockHalf.LOWER))
                .map(plan -> new DoorPlacementPlan(assembly, plan.states()));
    }
}
