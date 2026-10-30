package io.github.dirtyredz.roundwood.block.door.frame.fitting;

import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** An arch-top item used on a standing door: the door is rebuilt in the fitted design, keeping its full NBT. */
final class ArchOntoDoorProposal {
    private ArchOntoDoorProposal() {}

    static Optional<DoorArchProposal> propose(UseOnContext context, ArchPieceItem item, AssemblyDoorBlock block,
                                              BlockPos sourcePos, BlockState source) {
        var level = context.getLevel();
        var door = DoorLayout.of(sourcePos, source);
        if (item.piece().design() != DoorArchFitPlan.topFor(block.design()) || !door.loaded(level) || !door.complete(level, block)
                || !(level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity)
                || !entity.archEditable() || entity.arch().isPresent() || !entity.trimsEditable()) return Optional.empty();
        var targetCell = sourcePos.equals(context.getClickedPos()) ? context.getClickedPos()
                : new BlockPlaceContext(context).getClickedPos();
        var local = context.getClickLocation().subtract(Vec3.atLowerCornerOf(targetCell));
        var depth = item.piece().thickness() == ArchPieceThickness.FULL ? ArchPieceDepth.FRONT
                : ArchPieceDepth.target(door.facing(), context.getClickedFace(), local.x, local.y, local.z);
        var candidate = DoorArchFitPlan.fromDoor(door, item.piece().thickness(), depth, entity.camouflage().isPresent());
        if (candidate.isEmpty()) return Optional.empty();
        var attachment = new DoorArchAttachment(item.piece().design(), item.piece().thickness(), depth,
                AppearanceStacks.read(context.getItemInHand()), FaceTrims.NONE);
        return DoorArchProposal.validated(context, candidate.get(), block, attachment, entity.saveWithFullMetadata(level.registryAccess()),
                source.getValue(DoorBlock.OPEN), source.getValue(DoorBlock.POWERED), false);
    }
}
