package io.github.dirtyredz.roundwood.block.assembly.placement;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.assembly.Assembly;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.appearance.AppearanceAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes a validated {@link AssemblyPlacementPlan} as one transaction: every cell, in part order, with
 * ordinary {@code setBlock(UPDATE_ALL)} calls inside an {@link AssemblyMutationBatch} (so a player
 * placement's cells all join Forge's block-snapshot capture and its one multi-place event), then the
 * look on the new anchor entity. Success means all of it happened. No payment, sound or event here
 * ({@link AssemblyPlacement} does that for players; player-less placement stays silent and free).
 *
 * <p>All or nothing: a refused write, a refused batch (nested too deep), a missing anchor entity or an
 * exception puts every cell back as it was, block state and block-entity data, with teardown and drops
 * suppressed while restoring, before returning false (or rethrowing). The rollback removes only the
 * block snapshots this installation added: an enclosing Forge transaction keeps its own. Entry during
 * a snapshot restore is refused. Forge's own cancellation of a whole player placement is unchanged:
 * that is the outer transaction's rollback, not this one.
 */
public final class AssemblyInstallation {
    private AssemblyInstallation() {}
    public static <P> boolean install(Level level, AssemblyPlacementPlan<P, ? extends Assembly<P>> plan, Appearance appearance,
                                      Class<? extends AppearanceAnchor> anchorType) {
        return install(level,plan,appearance,anchorType,entity -> true);
    }
    public static <P> boolean install(Level level, AssemblyPlacementPlan<P, ? extends Assembly<P>> plan, Appearance appearance,
                                      Class<? extends AppearanceAnchor> anchorType, java.util.function.Predicate<AppearanceAnchor> extra) {
        var states = new java.util.LinkedHashMap<BlockPos, BlockState>();
        for (P part : plan.assembly().parts()) states.put(plan.assembly().pos(part), plan.states().get(part));
        return AssemblyWriteTransaction.write(level, states, false,
                () -> installAppearance(level, plan.assembly().anchor(), appearance, anchorType, extra), () -> true);
    }
    private static boolean installAppearance(Level level, BlockPos anchor, Appearance appearance,
                                             Class<? extends AppearanceAnchor> anchorType, java.util.function.Predicate<AppearanceAnchor> extra) {
        if (level.getBlockEntity(anchor) instanceof AppearanceAnchor entity && anchorType.isInstance(entity)) {
            entity.setAppearance(appearance);
            return extra.test(entity);
        }
        return false;
    }

}
