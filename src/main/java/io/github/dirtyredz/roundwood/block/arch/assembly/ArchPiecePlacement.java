package io.github.dirtyredz.roundwood.block.arch.assembly;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyInstallation;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacement;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.Optional;

/** Read-only complete-footprint proposals; the shared installer owns every write and rollback. */
public final class ArchPiecePlacement {
    private ArchPiecePlacement() {}

    public static Optional<AssemblyPlacementPlan<ArchPieceCell, ArchPieceLayout.Assembly>> of(ArchPieceBlock block, BlockPlaceContext context) {
        BlockPos anchor = context.getClickedPos().immutable();
        Vec3 hit = context.getClickLocation();
        var target = block.design().shoulder() ? ArchPieceTargeting.target(context.getHorizontalDirection(), context.getClickedFace(),
                hit.x - anchor.getX(), hit.y - anchor.getY(), hit.z - anchor.getZ())
                : new ArchPieceTargeting.Target(context.getHorizontalDirection(), Side.LEFT, null);
        var depth = block.thickness() == ArchPieceThickness.FULL ? ArchPieceDepth.FRONT
                : ArchPieceDepth.target(target.facing(), context.getClickedFace(),
                        hit.x - anchor.getX(), hit.y - anchor.getY(), hit.z - anchor.getZ());
        var assembly = new ArchPieceLayout.Assembly(anchor, target.facing(), block.design(), target.side(), block.thickness(), depth);
        if (!AssemblyPlacementPlan.playerCells(context, assembly, pos -> {})) return Optional.empty();
        var player = context.getPlayer();
        return plan(context.getLevel(), block, assembly, player == null ? CollisionContext.empty() : CollisionContext.of(player));
    }

    public static Optional<AssemblyPlacementPlan<ArchPieceCell, ArchPieceLayout.Assembly>> at(Level level, ArchPieceBlock block, ArchPieceLayout.Assembly assembly) {
        if (block.design() != assembly.design() || block.thickness() != assembly.thickness() || !AssemblyPlacementPlan.playerlessCells(level, assembly)) return Optional.empty();
        return plan(level, block, assembly, CollisionContext.empty());
    }

    private static Optional<AssemblyPlacementPlan<ArchPieceCell, ArchPieceLayout.Assembly>> plan(Level level, ArchPieceBlock block,
            ArchPieceLayout.Assembly assembly, CollisionContext collision) {
        // No external support requirement. Entity obstruction uses each product's collision,
        // while the shared preflight still reserves every dry, replaceable, editable cell.
        return AssemblyPlacementPlan.of(assembly, cell -> assembly.stateFor(cell, block),
                AssemblyPlacementPlan.unobstructed(level, collision, cell -> false));
    }

    public static InteractionResult place(ArchPieceItem item, BlockPlaceContext context) {
        return AssemblyPlacement.place(context, of(item.piece(), context), ArchPieceBlockEntity.class);
    }

    public static boolean placeAt(Level level, ArchPieceBlock block, ArchPieceLayout.Assembly assembly, Appearance appearance) {
        return at(level, block, assembly).map(plan -> AssemblyInstallation.install(level, plan, appearance, ArchPieceBlockEntity.class)).orElse(false);
    }
}
