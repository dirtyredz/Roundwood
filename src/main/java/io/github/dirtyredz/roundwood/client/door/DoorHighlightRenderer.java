package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorSelectionShapes;
import io.github.dirtyredz.roundwood.client.render.OutlineLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
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
        OutlineLines.edges(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()), shape,
                pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z, 0F, 0F, 0F, ALPHA);
    }
}
