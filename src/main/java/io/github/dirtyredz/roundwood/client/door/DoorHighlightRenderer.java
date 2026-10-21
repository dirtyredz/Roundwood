package io.github.dirtyredz.roundwood.client.door;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorSelectionShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces the vanilla block highlight for round doors. Vanilla outlines the block's
 * {@code getShape}, which is the target shape: with a block item in hand it includes the empty
 * corners, so an open door was outlined as the closed frame. This draws the visible shape instead
 * ({@link DoorSelectionShapes#visible}: the leaf at rest plus filled corners only), as one shape's
 * edges (no seams between boxes), with vanilla's line style. An incomplete assembly draws nothing,
 * like the leaf renderer. Client-only, on the Forge bus.
 */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DoorHighlightRenderer {
    private static final float ALPHA = 0.4F;

    private DoorHighlightRenderer() {
    }

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockPos pos = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RoundDoorBlock door)) {
            return;
        }
        event.setCanceled(true);
        if (!DoorLayout.of(pos, state).complete(level, door)) {
            return; // nothing is drawn for a broken assembly, so nothing is outlined either
        }
        VoxelShape shape = DoorSelectionShapes.visible(state, level, pos);
        Vec3 camera = event.getCamera().getPosition();
        draw(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()), shape,
                pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
    }

    /** Vanilla's {@code LevelRenderer.renderShape}: every edge of the shape as a black line. */
    private static void draw(PoseStack pose, VertexConsumer lines, VoxelShape shape, double x, double y, double z) {
        PoseStack.Pose last = pose.last();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= length;
            dy /= length;
            dz /= length;
            lines.vertex(last.pose(), (float) (x1 + x), (float) (y1 + y), (float) (z1 + z))
                    .color(0F, 0F, 0F, ALPHA).normal(last.normal(), dx, dy, dz).endVertex();
            lines.vertex(last.pose(), (float) (x2 + x), (float) (y2 + y), (float) (z2 + z))
                    .color(0F, 0F, 0F, ALPHA).normal(last.normal(), dx, dy, dz).endVertex();
        });
    }
}
