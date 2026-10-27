package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareDonors;
import io.github.dirtyredz.roundwood.block.door.interaction.DoorHardwareTarget;
import io.github.dirtyredz.roundwood.client.render.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shows the complete assembly-wide slot that the shared server proposal would change. */
@Mod.EventBusSubscriber(modid=Roundwood.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class DoorHardwarePreview {
    private DoorHardwarePreview() {}
    @SubscribeEvent(receiveCanceled=true)
    public static void highlight(RenderHighlightEvent.Block event){
        PlacementHighlights.draw(event,(player,level,hit)->{
            var hand=DoorHardwareDonors.of(player,InteractionHand.MAIN_HAND).isPresent()?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
            return DoorHardwareTarget.of(level,player,hand,hit).filter(DoorHardwareTarget.Proposal::eligible);
        },(client,proposal)->{
            var camera=event.getCamera().getPosition();var pos=proposal.door().anchor();
            OutlineLines.edges(event.getPoseStack(),event.getMultiBufferSource().getBuffer(RenderType.lines()),
                    DoorHardwareTarget.shape(proposal),pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z,.3F,1F,.6F,.95F);
        });
    }
}
