package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.frame.*;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem;
import io.github.dirtyredz.roundwood.block.door.selection.DoorPlacementOutline;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import io.github.dirtyredz.roundwood.client.render.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Optional;

/** The same complete proposal as server fitting; rejected combinations draw a red occupied cell. */
@Mod.EventBusSubscriber(modid=Roundwood.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class DoorArchPlacementPreview {
    private record Attempt(BlockHitResult hit, Optional<DoorArchFitting.Proposal> proposal) {}
    private DoorArchPlacementPreview() {}
    private static Optional<Attempt> of(Player player, Level level, BlockHitResult hit) {
        var hand=PlacementHighlights.held(player,s -> s.getItem() instanceof RoundDoorItem || s.getItem() instanceof ArchPieceItem);
        if(hand==null || player.isSpectator() || !level.isLoaded(hit.getBlockPos())) return Optional.empty();
        var resolved=DoorHitResolver.resolve(level,hit); if(resolved.isEmpty()) return Optional.empty();
        var context=new UseOnContext(player,hand,resolved.get());
        return DoorArchFitting.claims(context)?Optional.of(new Attempt(resolved.get(),DoorArchFitting.propose(context))):Optional.empty();
    }
    @SubscribeEvent(receiveCanceled=true)
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        PlacementHighlights.draw(event,DoorArchPlacementPreview::of,(client,attempt)->{
            var camera=event.getCamera().getPosition(); var pose=event.getPoseStack(); var lines=event.getMultiBufferSource().getBuffer(RenderType.lines());
            if(attempt.proposal().isEmpty()) {
                var pos=attempt.hit().getBlockPos();
                OutlineLines.edges(pose,lines,Shapes.block(),pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z,1,.15F,.1F,.95F);
                return;
            }
            var proposal=attempt.proposal().get(); var door=proposal.fit().door(); var pos=door.anchor();
            double x=pos.getX()-camera.x,y=pos.getY()-camera.y,z=pos.getZ()-camera.z;
            OutlineLines.edges(pose,lines,DoorArchShapes.of(door,proposal.attachment(),false),x,y,z,.4F,1,.5F,.9F);
            var edges=DoorPlacementOutline.silhouette(door,proposal.states().get(pos).getValue(DoorBlock.OPEN));
            for(var e:edges) OutlineLines.line(pose,lines,x+e.x1()/16,y+e.y1()/16,z+e.z1()/16,x+e.x2()/16,y+e.y2()/16,z+e.z2()/16,.35F,.8F,1,.9F);
        });
    }
}
