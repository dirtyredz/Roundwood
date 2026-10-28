package io.github.dirtyredz.roundwood.client.door.hardware;

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
    private static final DoorHardwarePreviewCache CACHE=new DoorHardwarePreviewCache();
    private DoorHardwarePreview() {}
    @SubscribeEvent
    public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){if(event.getLevel().isClientSide())CACHE.clear();}
    @SubscribeEvent(receiveCanceled=true)
    public static void highlight(RenderHighlightEvent.Block event){
        PlacementHighlights.draw(event,CACHE::of,(client,preview)->{
            var camera=event.getCamera().getPosition();var pos=preview.proposal().door().anchor();
            OutlineLines.edges(event.getPoseStack(),event.getMultiBufferSource().getBuffer(RenderType.lines()),
                    preview.shape(),pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z,.3F,1F,.6F,.95F);
        });
    }
}
