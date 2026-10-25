package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.DoorUseChannel;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Before the client acts on its crosshair target (use, attack, held mining, pick block), a hit on a
 * round door is credited to the part that contains the hit point, so the server's distance check
 * accepts it. Projected leaf use keeps its real point through a dedicated validated server request;
 * mining and picking that unoccupied projection remain refused. Client-only, on the Forge bus.
 */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DoorTargetingEvents {
    private DoorTargetingEvents() {
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        Optional<BlockHitResult> resolved = DoorHitResolver.resolve(minecraft.level, hit);
        if (resolved.isEmpty()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        } else if (DoorHitResolver.projected(minecraft.level, resolved.get())) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.isUseItem()) {
                DoorUseChannel.send(resolved.get());
            }
        } else if (resolved.get() != hit) {
            // The game reads its target after this event, so the corrected hit is the one sent.
            minecraft.hitResult = resolved.get();
        }
    }
}
