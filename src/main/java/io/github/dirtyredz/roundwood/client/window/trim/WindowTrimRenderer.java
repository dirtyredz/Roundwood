package io.github.dirtyredz.roundwood.client.window.trim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowFaceIdentity;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Optional;

/**
 * Draws a window's face attachments (0.8.0; arch sills since 0.10.0): each fitted face's band, opaque, from cached models
 * ({@link WindowTrimWorldModels}), tessellated like a block at the window's own cell (world light,
 * ambient occlusion) in the solid layer, without cull sides. Each face's slot is mapped to its world
 * side by the window's face identity, so a trim stays where it was fitted. A flush, floor or ceiling
 * window's wall-side band stands one pixel past the cell: the entity's render bounds include it and
 * it is drawn whenever those bounds are in view.
 */
public class WindowTrimRenderer implements BlockEntityRenderer<RoundWindowBlockEntity> {
    private final ModelBlockRenderer renderer;
    public WindowTrimRenderer(ModelBlockRenderer renderer) { this.renderer = renderer; }
    private final RandomSource random = RandomSource.create();

    public WindowTrimRenderer(BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    @Override
    public void render(RoundWindowBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!io.github.dirtyredz.roundwood.client.render.StaticRenderingConfig.legacy()) return;
        Level level = entity.getLevel();
        BlockState state = entity.getBlockState();
        if (level == null || !entity.anyAttachment() || !(state.getBlock() instanceof WindowPaneBlock block)) {
            return;
        }
        WindowPose windowPose = block.pose(state);
        boolean reversed = WindowPaneBlock.faceReversed(state);
        BlockPos pos = entity.getBlockPos();
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        for (TrimSlot slot : java.util.List.of(TrimSlot.PERIMETER, TrimSlot.SILL)) {
            FaceTrims trims = entity.trims(slot);
            for (AttachmentFace face : AttachmentFace.values()) {
                Optional<Appearance> look = trims.get(face);
                if (look.isEmpty()) {
                    continue;
                }
                Direction side = WindowFaceIdentity.side(windowPose, reversed, face);
                renderer.tesselateBlock(level, WindowTrimWorldModels.model(slot, block.profile(), windowPose, side,
                                WindowTrimModels.look(look.get(), slot, block.profile())), state, pos, pose, consumer, false, random,
                        state.getSeed(pos), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
            }
        }
    }

    /**
     * Like the door: a band may reach past the window's section, so a window with any attachment is
     * drawn whenever its bounds are in view. Bare windows stay with their section (they draw nothing here).
     */
    @Override
    public boolean shouldRenderOffScreen(RoundWindowBlockEntity entity) {
        return entity.anyAttachment();
    }
}
