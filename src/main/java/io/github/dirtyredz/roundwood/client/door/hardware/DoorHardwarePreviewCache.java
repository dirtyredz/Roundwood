package io.github.dirtyredz.roundwood.client.door.hardware;

import io.github.dirtyredz.roundwood.client.door.preview.DoorPreviewMemo;
import io.github.dirtyredz.roundwood.block.door.adapter.*;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareDonors;
import io.github.dirtyredz.roundwood.block.door.interaction.DoorHardwareTarget;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Optional;

/** Render-only memo; server actions always call the authoritative proposal again. */
final class DoorHardwarePreviewCache {
    record Preview(DoorHardwareTarget.Proposal proposal,VoxelShape shape) {}
    private record ShapeKey(Object facing,Object depth,java.util.List<io.github.dirtyredz.roundwood.geometry.Box> boxes) {}
    private static final io.github.dirtyredz.roundwood.client.material.AppearanceModelCache<ShapeKey,VoxelShape> SHAPES=
            new io.github.dirtyredz.roundwood.client.material.AppearanceModelCache<>(128);
    static VoxelShape shape(DoorHardwareTarget.Proposal proposal) {
        return SHAPES.get(new ShapeKey(proposal.door().facing(),proposal.door().depth(),proposal.boxes()),
                ignored->DoorHardwareTarget.shape(proposal));
    }
    private record Key(Level level,Player player,long tick,BlockPos pos,Vec3 point,Direction face,Vec3 eye,
                       Object state,Object revision,Object hardware,boolean moving,Object item,Object tag,
                       InteractionHand hand,boolean alive,boolean spectator,boolean mayBuild,long models) {}
    private final DoorPreviewMemo<Key,Optional<Preview>> memo=new DoorPreviewMemo<>();
    Optional<Preview> of(Player player,Level level,BlockHitResult hit) {
        var hand=DoorHardwareDonors.of(player,InteractionHand.MAIN_HAND).isPresent()?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        if(DoorHardwareDonors.of(player,hand).isEmpty() || !level.isLoaded(hit.getBlockPos())){clear();return Optional.empty();}
        var state=level.getBlockState(hit.getBlockPos());
        if(!(state.getBlock() instanceof AssemblyDoorBlock)){clear();return Optional.empty();}
        var door=DoorLayout.of(hit.getBlockPos(),state);
        if(!(level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity)){clear();return Optional.empty();}
        var stack=player.getItemInHand(hand);
        // A tick is a conservative world-obstruction/permission revision. Target payload changes,
        // hand changes and camera movement invalidate immediately, including within that tick.
        var key=new Key(level,player,level.getGameTime(),hit.getBlockPos(),hit.getLocation(),hit.getDirection(),player.getEyePosition(),
                state,entity.staticRenderPublication().current(),entity.hardware(),entity.swing().moving(level.getGameTime()),
                stack.getItem(),stack.getTag()==null?null:stack.getTag().copy(),hand,player.isAlive(),player.isSpectator(),player.mayBuild(),
                io.github.dirtyredz.roundwood.client.material.AppearanceModelCache.generation());
        return memo.get(key,()->DoorHardwareTarget.of(level,player,hand,hit).filter(DoorHardwareTarget.Proposal::eligible)
                .map(proposal->new Preview(proposal,shape(proposal))));
    }
    void clear(){memo.clear();}
}
