package io.github.dirtyredz.roundwood.block.trim.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyInstallation;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import net.minecraft.world.level.Level;

/**
 * Writes a whole compatibility frame or nothing (0.8.0: frames are no longer placed by players; the
 * {@code round_trim} item attaches to doors instead). Saved frames keep their lifecycle; this remains
 * for structure fixtures, the showcase's retained legacy exhibit and tests. The checks are the shared
 * player-less skeleton's ({@link AssemblyPlacementPlan}: inside the world, loaded, dry, replaceable)
 * plus the frame's own policy: each quarter must survive (both lower quarters on a sturdy floor), and,
 * unlike doors and archways, entities never obstruct a frame (it has no collision). The look goes on
 * the new anchor in the same transaction ({@link AssemblyInstallation}).
 */
public final class TrimPlacement {
    private TrimPlacement() {
    }

    /**
     * Places a complete frame without a player or item (the showcase builder, other commands). Returns
     * false, changing nothing, if any check or write fails.
     */
    public static boolean placeAt(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly) {
        return placeAt(level, block, assembly, block.defaultAppearance());
    }

    /** As {@link #placeAt(Level, RoundTrimBlock, TrimLayout.Assembly)}, in {@code appearance}. */
    public static boolean placeAt(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly, Appearance appearance) {
        if (!AssemblyPlacementPlan.playerlessCells(level, assembly)) {
            return false;
        }
        return AssemblyPlacementPlan.of(assembly, part -> assembly.stateFor(part, block), (part, pos, state) -> state.canSurvive(level, pos))
                .map(plan -> AssemblyInstallation.install(level, plan, appearance, RoundTrimBlockEntity.class))
                .orElse(false);
    }
}
