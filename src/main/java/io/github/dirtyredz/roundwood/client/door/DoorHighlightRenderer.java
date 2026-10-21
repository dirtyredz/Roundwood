package io.github.dirtyredz.roundwood.client.door;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorSelectionShapes;
import io.github.dirtyredz.roundwood.block.door.selection.DoorSwingOutline;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import io.github.dirtyredz.roundwood.client.render.OutlineLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces the vanilla block highlight for round doors. Vanilla outlines the block's
 * {@code getShape}, which is the target shape: with a block item in hand it includes the empty
 * corners, so an open door was outlined as the closed frame. This draws the visible shape instead
 * ({@link DoorSelectionShapes#visible}: the leaf at rest plus filled corners only), as one shape's
 * edges (no seams between boxes), with vanilla's line style. While the leaf is swinging the
 * blockstate already holds the final pose, so the leaf's outline is instead the closed silhouette
 * turned by the anchor's current swing ({@link DoorSwingOutline}, the renderer's own angle) and the
 * installed trim rings (0.8.0, static casing) and
 * filled corners stay where they are: the outline never runs ahead of the leaf. An incomplete
 * assembly draws nothing, like the leaf renderer. Client-only, on the Forge bus.
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
        Vec3 camera = event.getCamera().getPosition();
        double x = pos.getX() - camera.x;
        double y = pos.getY() - camera.y;
        double z = pos.getZ() - camera.z;
        PoseStack pose = event.getPoseStack();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        double now = level.getGameTime() + event.getPartialTick();
        if (level.getBlockEntity(DoorLayout.of(pos, state).anchor()) instanceof RoundDoorBlockEntity anchor
                && anchor.swing().moving(now)) {
            OutlineLines.edges(pose, lines, DoorSelectionShapes.visibleStatic(state, level, pos), x, y, z, 0F, 0F, 0F, ALPHA);
            DoorLayout.Part part = DoorLayout.partOf(state);
            for (DoorSwingOutline.Edge e : DoorSwingOutline.forCell(state.getValue(DoorBlock.HINGE), anchor.swing().progress(now),
                    part.column.index(), part.row(), VoxelShapeFactory.quarterTurns(state.getValue(DoorBlock.FACING)))) {
                OutlineLines.line(pose, lines, x + e.x1() / 16, y + e.y1() / 16, z + e.z1() / 16,
                        x + e.x2() / 16, y + e.y2() / 16, z + e.z2() / 16, 0F, 0F, 0F, ALPHA);
            }
            return;
        }
        OutlineLines.edges(pose, lines, DoorSelectionShapes.visible(state, level, pos), x, y, z, 0F, 0F, 0F, ALPHA);
    }
}
