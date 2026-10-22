package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.interaction.WindowBeamFitPlan;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.client.render.OutlineLines;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draws exactly the clipped cross accepted by the shared fit proposal, at the pane's real depth. */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WindowBeamPreview {
    private WindowBeamPreview() { }

    @SubscribeEvent(receiveCanceled = true)
    public static void onHighlight(RenderHighlightEvent.Block event) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        InteractionHand hand = client.player.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        WindowBeamFitPlan.of(client.level, event.getTarget().getBlockPos(), client.player, hand, event.getTarget())
                .ifPresent(plan -> draw(event, plan.pos(), plan.design(), plan.pose()));
    }

    static void draw(RenderHighlightEvent.Block event, BlockPos pos, WindowDesign design, WindowPose pose) {
        var shape = VoxelShapeFactory.shape(WindowPaneGeometry.panel(design, pose).stream()
                .filter(piece -> piece.texture().equals(WindowPaneGeometry.BEAM)).map(Piece::box).toList());
        var camera = event.getCamera().getPosition();
        OutlineLines.edges(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()), shape,
                pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z, .35F, .8F, 1F, .9F);
    }
}
