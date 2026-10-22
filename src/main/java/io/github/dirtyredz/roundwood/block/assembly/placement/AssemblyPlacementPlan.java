package io.github.dirtyredz.roundwood.block.assembly.placement;

import io.github.dirtyredz.roundwood.block.assembly.Assembly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * What placing a whole assembly would write, decided once and read-only, shared by every family: the
 * assembly and the state of each part, in the assembly's part order. The family's typed plan (door,
 * archway) is a facade over this; the frame's player-less writer uses it directly. The validation
 * skeleton is shared too; a family supplies only its part states and its per-cell policy (power for a
 * door, entity obstruction and floor for doors and archways, floor only for the frame).
 * <ul>
 *     <li>Every cell is checked against the world first ({@link #inWorld}: build height, border,
 *     loaded, dry), before anything that reads it further.</li>
 *     <li>A player's click ({@link #playerCells}) additionally needs each cell replaceable in that
 *     context and the player's permission at each cell ({@code mayInteract} and {@code mayUseItemAt});
 *     a player-less placement ({@link #playerlessCells}) needs each cell plainly replaceable.</li>
 *     <li>Then every part's state must pass the family's {@link CellPolicy} ({@link #of}).</li>
 * </ul>
 */
public record AssemblyPlacementPlan<P>(Assembly<P> assembly, Map<P, BlockState> states) {
    /** The family's check of one part's state at its cell (obstruction, survival). */
    @FunctionalInterface
    public interface CellPolicy<P> {
        boolean accepts(P part, BlockPos pos, BlockState state);
    }

    public AssemblyPlacementPlan {
        Map<P, BlockState> ordered = new LinkedHashMap<>();
        for (P part : assembly.parts()) {
            BlockState state = states.get(part);
            if (state == null) {
                throw new IllegalArgumentException("no state for " + part);
            }
            ordered.put(part, state);
        }
        states = Collections.unmodifiableMap(ordered);
    }

    /** The anchor's state (the first part's). */
    public BlockState anchorState() {
        return states.get(assembly.parts().get(0));
    }

    /** Inside the build height and the world border, in a loaded chunk, and dry (assemblies are never waterlogged). */
    public static boolean inWorld(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.isLoaded(pos)
                && level.getFluidState(pos).isEmpty();
    }

    /**
     * True when the click in {@code context} may place on every cell: it can place at all, each cell is in
     * the world and replaceable in that context, and its player (if any) may interact and use the item
     * there. {@code visit} sees each accepted cell in order (a door reads its power there).
     */
    public static boolean playerCells(BlockPlaceContext context, Assembly<?> assembly, Consumer<BlockPos> visit) {
        if (!context.canPlace()) {
            return false;
        }
        Level level = context.getLevel();
        Player player = context.getPlayer();
        for (BlockPos pos : assembly.positions()) {
            if (!inWorld(level, pos)
                    || !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, context.getClickedFace()))) {
                return false;
            }
            if (player != null && (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) {
                return false;
            }
            visit.accept(pos);
        }
        return true;
    }

    /** True when every cell is in the world and plainly replaceable (a placement without a player or item). */
    public static boolean playerlessCells(Level level, Assembly<?> assembly) {
        for (BlockPos pos : assembly.positions()) {
            if (!inWorld(level, pos) || !level.getBlockState(pos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    /** The plan writing {@code stateFor} each part, if every part passes {@code policy}. */
    public static <P> Optional<AssemblyPlacementPlan<P>> of(Assembly<P> assembly, Function<P, BlockState> stateFor, CellPolicy<P> policy) {
        Map<P, BlockState> states = new LinkedHashMap<>();
        for (P part : assembly.parts()) {
            BlockPos pos = assembly.pos(part);
            BlockState state = stateFor.apply(part);
            if (!policy.accepts(part, pos, state)) {
                return Optional.empty();
            }
            states.put(part, state);
        }
        return Optional.of(new AssemblyPlacementPlan<>(assembly, states));
    }

    /**
     * The door and archway policy: no entity in the way of the part's own collision (as {@code collision}
     * sees it: someone in an archway's passage does not block it), and each part {@code standing} on the
     * floor must survive there.
     */
    public static <P> CellPolicy<P> unobstructed(Level level, CollisionContext collision, Predicate<P> standing) {
        return (part, pos, state) -> level.isUnobstructed(state, pos, collision) && (!standing.test(part) || state.canSurvive(level, pos));
    }
}
