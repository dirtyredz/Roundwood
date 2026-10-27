package io.github.dirtyredz.roundwood.block.door.interaction;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.adapter.*;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.hardware.*;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.block.door.camouflage.*;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchGeometry;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimGeometry;
import io.github.dirtyredz.roundwood.geometry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import java.util.*;

/** Read-only endpoint proposal shared by overlay and authoritative server use. */
public final class DoorHardwareTarget {
    private DoorHardwareTarget() {}
    public record Proposal(DoorLayout.Assembly door,DoorHardwareSlot slot,DoorHardware before,Appearance after,
                           boolean open,boolean eligible,List<Box> boxes) {}
    public record Hit(DoorHardwareSlot slot,Vec3 point,boolean blocked) {}

    public static Optional<Hit> nearest(List<DoorHardwareGeometry.Part> hardware,List<Box> blockers,Vec3 from,Vec3 to) {
        Hit best=null;double distance=Double.POSITIVE_INFINITY;
        for(var part:hardware) {
            var point=clip(part.box(),from,to);
            if(point.isPresent() && from.distanceToSqr(point.get())<distance) {
                distance=from.distanceToSqr(point.get());best=new Hit(part.slot(),point.get(),false);
            }
        }
        if(best==null)return Optional.empty();
        for(var box:blockers) {
            var point=clip(box,from,to);
            if(point.isPresent() && from.distanceToSqr(point.get())<=distance+1e-7)return Optional.of(new Hit(best.slot,best.point,true));
        }
        return Optional.of(best);
    }
    private static Optional<Vec3> clip(Box b,Vec3 from,Vec3 to){return new AABB(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()).clip(from,to);}

    public static Optional<Proposal> of(Level level,Player player,InteractionHand hand,BlockHitResult requested) {
        var donor=DoorHardwareDonors.of(player,hand);if(donor.isEmpty() || !level.isLoaded(requested.getBlockPos()))return Optional.empty();
        var state=level.getBlockState(requested.getBlockPos());if(!(state.getBlock() instanceof AssemblyDoorBlock block))return Optional.empty();
        var door=DoorLayout.of(requested.getBlockPos(),state);
        if(!door.loaded(level) || !door.complete(level,block) || !(level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity))return Optional.empty();
        var from=door.toAuthored(player.getEyePosition());var aimed=door.toAuthored(requested.getLocation());
        var to=aimed.add(aimed.subtract(from).normalize().scale(.16));
        boolean open=state.getValue(DoorBlock.OPEN);
        var pieces=DoorHardwareGeometry.placed(door.design(),door.hinge(),open);
        var blockers=new ArrayList<>(DoorGeometry.placedEnvelope(door.design(),door.hinge(),open));
        for(var face:io.github.dirtyredz.roundwood.block.attachment.AttachmentFace.values())
            if(entity.trims().has(face))blockers.addAll(DoorTrimGeometry.ring(door.design(),face));
        entity.arch().ifPresent(frame->DoorArchGeometry.selection(door.design(),frame,false).stream()
                .map(b->b.translate(0,0,-door.depth().offset())).forEach(blockers::add));
        if(entity.camouflage().isPresent())blockers.addAll(CamouflageGeometry.corners(door.design(),CamouflageCoverage.of(door.design(),entity.arch().isPresent())));
        var hit=nearest(pieces,blockers,from,to);
        boolean current=hit.isPresent() && hit.get().point.distanceToSqr(aimed)<.001;
        if(!current) {
            // A stale request for the other endpoint is consumed, never converted into a toggle.
            hit=nearest(DoorHardwareGeometry.placed(door.design(),door.hinge(),!open),List.of(),from,to);
            if(hit.isEmpty() || hit.get().point.distanceToSqr(aimed)>=.001)return Optional.empty();
        }
        var selected=hit.get();
        boolean eligible=current && !selected.blocked && entity.hardwareEditable() && !entity.swing().moving(level.getGameTime())
                && player.isAlive() && !player.isSpectator() && player.mayBuild() && door.mayEditAll(level,player)
                && door.positions().stream().allMatch(pos->player.mayUseItemAt(pos,requested.getDirection(),player.getItemInHand(hand))) && player.canReach(requested.getLocation(),0);
        if(eligible) {
            var worldEnd=requested.getLocation().add(requested.getLocation().subtract(player.getEyePosition()).normalize().scale(.01));
            var obstruction=level.clip(new ClipContext(player.getEyePosition(),worldEnd,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player){
                @Override public VoxelShape getBlockShape(BlockState other,BlockGetter world,BlockPos pos){
                    if(other.getBlock() instanceof AssemblyDoorBlock && DoorLayout.of(pos,other).equals(door))return Shapes.empty();
                    return super.getBlockShape(other,world,pos);
                }
            });
            eligible=obstruction.getType()==HitResult.Type.MISS || obstruction.getLocation().distanceToSqr(player.getEyePosition())+1e-7>=requested.getLocation().distanceToSqr(player.getEyePosition());
        }
        var after=donor.get().apply(entity.hardware().get(selected.slot).orElse(Appearance.DEFAULT)).next();
        return Optional.of(new Proposal(door,selected.slot,entity.hardware(),after,open,eligible,
                pieces.stream().filter(p->p.slot()==selected.slot).map(DoorHardwareGeometry.Part::box).toList()));
    }
    public static VoxelShape shape(Proposal proposal){return shape(proposal.door,proposal.boxes);}
    public static VoxelShape shape(DoorLayout.Assembly door,List<Box> boxes){
        return VoxelShapeFactory.shape(boxes.stream().map(b->b.translate(0,0,door.depth().offset()).rotateY(VoxelShapeFactory.quarterTurns(door.facing()))).toList());
    }
}
