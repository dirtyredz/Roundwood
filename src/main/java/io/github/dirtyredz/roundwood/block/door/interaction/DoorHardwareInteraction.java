package io.github.dirtyredz.roundwood.block.door.interaction;

import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.hardware.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import java.util.Optional;

/** Proposal revalidation and one-item payment. A refused hardware request never falls through to toggle. */
public final class DoorHardwareInteraction {
    private DoorHardwareInteraction() {}
    public static Optional<InteractionResult> use(Level level,Player player,InteractionHand hand,BlockHitResult hit){
        var proposal=DoorHardwareTarget.of(level,player,hand,hit);if(proposal.isEmpty())return Optional.empty();
        var plan=proposal.get();
        if(!plan.eligible())return Optional.of(InteractionResult.CONSUME);
        if(level.isClientSide)return Optional.of(InteractionResult.SUCCESS);
        if(!DoorHardwareTarget.of(level,player,hand,hit).filter(plan::equals).isPresent())return Optional.of(InteractionResult.CONSUME);
        var entity=(RoundDoorBlockEntity)level.getBlockEntity(plan.door().anchor());
        var next=plan.before().with(plan.slot(),Optional.of(plan.after()));
        if(!next.equals(plan.before()) && entity.setHardware(next) && !player.getAbilities().instabuild)player.getItemInHand(hand).shrink(1);
        return Optional.of(InteractionResult.CONSUME);
    }
}
