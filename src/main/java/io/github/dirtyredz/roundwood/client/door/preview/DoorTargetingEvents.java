package io.github.dirtyredz.roundwood.client.door.preview;

import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.DoorUseChannel;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Optional;

/**
 * Before the client acts on its crosshair target (use, attack, held mining, pick block), a hit on a
 * round door is credited to the part that contains the hit point, so the server's distance check
 * accepts it. Projected leaf use keeps its real point through a dedicated validated server request;
 * mining and picking that unoccupied projection remain refused. Client-only, on the Forge bus.
 */
@EventBusSubscriber(modid = Roundwood.MOD_ID, value = Dist.CLIENT)
public final class DoorTargetingEvents {
    private record RayKey(Object level,Object player,long tick,Object eye,Object end,Object main,Object mainTag,
                          Object off,Object offTag,boolean secondary,Object targetState,Object targetRevision) {}
    private static final DoorPreviewMemo<RayKey,BlockHitResult> RAYS=new DoorPreviewMemo<>();
    private DoorTargetingEvents() {
    }

    /** After vanilla picking, before block highlights: acquire the same whole leaf the server validates. */
    @SubscribeEvent
    public static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        acquire(Minecraft.getInstance(), event.getPartialTick().getGameTimeDeltaPartialTick(false),false);
    }

    @SubscribeEvent
    public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event){if(event.getLevel().isClientSide())RAYS.clear();}

    private static void acquire(Minecraft minecraft, float partialTick,boolean fresh) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || minecraft.getCameraEntity() != minecraft.player) return;
        var eye = minecraft.player.getEyePosition(partialTick);
        var end = eye.add(minecraft.player.getViewVector(partialTick).scale(minecraft.player.blockInteractionRange()));
        var main=minecraft.player.getMainHandItem();var off=minecraft.player.getOffhandItem();
        Object targetState=null,targetRevision=null;
        if(minecraft.hitResult instanceof BlockHitResult previous) {
            var state=minecraft.level.getBlockState(previous.getBlockPos());targetState=state;
            if(state.getBlock() instanceof io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock) {
                var door=io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.of(previous.getBlockPos(),state);
                if(minecraft.level.getBlockEntity(door.anchor()) instanceof io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity entity)
                    targetRevision=entity.staticRenderPublication().current();
            }
        }
        var key=new RayKey(minecraft.level,minecraft.player,minecraft.level.getGameTime(),eye,end,
                main.getItem(),main.getComponentsPatch(),off.getItem(),off.getComponentsPatch(),
                minecraft.player.isSecondaryUseActive(),targetState,targetRevision);
        if(fresh)RAYS.clear(); // input always validates the live world, even inside a rendered tick
        var hit = RAYS.get(key,()->io.github.dirtyredz.roundwood.block.door.selection.DoorRaycast.clip(minecraft.level, minecraft.player, eye, end));
        if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.ENTITY
                && minecraft.hitResult.getLocation().distanceToSqr(eye) <= hit.getLocation().distanceToSqr(eye)) return;
        boolean doorHit = hit.getType() == HitResult.Type.BLOCK && minecraft.level.getBlockState(hit.getBlockPos()).getBlock()
                instanceof io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
        boolean previousDoor = minecraft.hitResult instanceof BlockHitResult previous && previous.getType() == HitResult.Type.BLOCK
                && minecraft.level.getBlockState(previous.getBlockPos()).getBlock()
                instanceof io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
        if (!doorHit && !previousDoor) return;
        minecraft.hitResult = hit;
        minecraft.crosshairPickEntity = null;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        // Minecraft picks again at the start of each tick, overwriting the render-time target.
        acquire(minecraft, 1.0F,true);
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
