package io.github.dirtyredz.roundwood.block.door.assembly;

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

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * What placing a door would do, decided once, read-only (0.11.0): the assembly (anchor, facing,
 * hinge, design) and the state every cell gets. The server's placement writes exactly these states
 * ({@link DoorPlacement}); the client's preview draws exactly this plan's assembly
 * ({@code selection/DoorPlacementOutline}), so what is shown is what is placed. No plan means the
 * click places nothing and nothing is previewed.
 *
 * <p>The clicked (or replaceable) cell is the lower anchor and the assembly extends up and to the
 * right; the hinge follows {@link DoorHingePlacement} at the footprint's real outer edges. Every cell
 * is checked: world bounds and border, loaded, the player's permission, replaceable, dry, free of
 * entities, and each column standing on a sturdy floor. Sneaking changes nothing.
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
        Player player = context.getPlayer();
        boolean powered = false;
        for (BlockPos pos : assembly.positions()) {
            if (!placeable(level, pos)
                    || !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, context.getClickedFace()))) {
                return Optional.empty();
            }
            if (player != null && (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) {
                return Optional.empty();
            }
            powered |= level.hasNeighborSignal(pos);
        }
        return states(level, block, assembly, powered, powered,
                player == null ? CollisionContext.empty() : CollisionContext.of(player));
    }

    /**
     * The plan for a door placed without a player or item (the showcase builder, other commands): the
     * same bounds, loaded, replaceable, dry, unobstructed and floor checks; the cells unpowered, in
     * the requested open state. Empty when {@code assembly} is not {@code block}'s design.
     */
    public static Optional<DoorPlacementPlan> at(Level level, AssemblyDoorBlock block, DoorLayout.Assembly assembly, boolean open) {
        if (assembly.design() != block.design()) {
            return Optional.empty();
        }
        for (BlockPos pos : assembly.positions()) {
            if (!placeable(level, pos) || !level.getBlockState(pos).canBeReplaced()) {
                return Optional.empty();
            }
        }
        return states(level, block, assembly, open, false, CollisionContext.empty());
    }

    /** The anchor's state (the lower-left part). */
    public BlockState anchorState() {
        return states.get(DoorLayout.Part.LOWER_LEFT);
    }

    /** True when the door would be placed open (a powered spot opens it at once). */
    public boolean open() {
        return anchorState().getValue(DoorBlock.OPEN);
    }

    private static boolean placeable(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.isLoaded(pos)
                && level.getFluidState(pos).isEmpty(); // doors are never waterlogged
    }

    /** Every part's state, if none is obstructed and each column has a sturdy floor. */
    private static Optional<DoorPlacementPlan> states(Level level, AssemblyDoorBlock block, DoorLayout.Assembly assembly,
                                                      boolean open, boolean powered, CollisionContext collision) {
        Map<DoorLayout.Part, BlockState> states = new EnumMap<>(DoorLayout.Part.class);
        for (DoorLayout.Part part : assembly.parts()) {
            BlockPos pos = assembly.pos(part);
            BlockState state = assembly.stateFor(part, block, open, powered);
            if (!level.isUnobstructed(state, pos, collision)
                    || part.half == DoubleBlockHalf.LOWER && !state.canSurvive(level, pos)) {
                return Optional.empty();
            }
            states.put(part, state);
        }
        return Optional.of(new DoorPlacementPlan(assembly, states));
    }
}
