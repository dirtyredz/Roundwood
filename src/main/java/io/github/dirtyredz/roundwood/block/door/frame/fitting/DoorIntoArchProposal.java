package io.github.dirtyredz.roundwood.block.door.frame.fitting;

import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimNbt;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorHingePlacement;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A door item used on a free-standing arch top: the door is built under it and takes the arch's custody. */
final class DoorIntoArchProposal {
    private DoorIntoArchProposal() {}

    static Optional<DoorArchProposal> propose(UseOnContext context, RoundDoorItem item, ArchPieceBlock archBlock,
                                              BlockPos sourcePos, BlockState source) {
        var level = context.getLevel();
        if (archBlock.design().shoulder() || !source.getValue(ArchPieceBlock.CELL).in(archBlock.design())) return Optional.empty();
        var arch = ArchPieceLayout.of(sourcePos, source);
        if (!arch.loaded(level) || !arch.complete(level, archBlock) || context.getHorizontalDirection() != arch.facing()
                || !(level.getBlockEntity(arch.anchor()) instanceof ArchPieceBlockEntity entity) || !entity.trimsEditable()) return Optional.empty();
        var block = item.door();
        var anchor = arch.anchor().below(DoorArchFitPlan.topOffset(block.design()));
        for (var edge : List.of(anchor.relative(arch.facing().getCounterClockWise()), anchor.relative(arch.facing().getClockWise(), block.design().columns())))
            if (!level.isLoaded(edge) || !level.isLoaded(edge.above())) return Optional.empty();
        var hinge = DoorHingePlacement.choose(block, new BlockPlaceContext(context), anchor, arch.facing(), block.design().columns());
        var landing = new BlockPlaceContext(context).getClickedPos();
        if (!sourcePos.equals(context.getClickedPos()) && !anchor.equals(landing)) return Optional.empty();
        var targetCell = sourcePos.equals(context.getClickedPos()) ? context.getClickedPos() : landing;
        var local = context.getClickLocation().subtract(Vec3.atLowerCornerOf(targetCell));
        var depth = DoorDepth.target(arch.facing(), context.getClickedFace(), local.x, local.y, local.z);
        var candidate = DoorArchFitPlan.fromArch(block.design(), arch, hinge, depth);
        if (candidate.isEmpty() || !knownArchEntity(entity)) return Optional.empty();
        var fit = candidate.get();
        var attachment = new DoorArchAttachment(arch.design(), arch.thickness(), arch.depth(), entity.appearance(), entity.trims());
        var data = new CompoundTag(); AppearanceNbt.write(data, AppearanceStacks.read(context.getItemInHand()));
        DoorHardwareStacks.copy(context.getItemInHand(), data);
        boolean powered = fit.door().positions().stream().filter(level::isLoaded).anyMatch(level::hasNeighborSignal);
        return DoorArchProposal.validated(context, fit, block, attachment, data, powered, powered, true);
    }

    /** Only an arch whose entity holds nothing beyond its look and trims can hand its custody over losslessly. */
    private static boolean knownArchEntity(ArchPieceBlockEntity entity) {
        return Set.of(AppearanceNbt.TAG, ArchPieceTrimNbt.TAG).containsAll(entity.saveWithoutMetadata(entity.getLevel().registryAccess()).getAllKeys());
    }
}
