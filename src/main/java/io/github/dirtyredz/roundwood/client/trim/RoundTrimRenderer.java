package io.github.dirtyredz.roundwood.client.trim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Draws a whole frame from its anchor quarter: each quarter's cached model in the frame's
 * appearance ({@link TrimAppearanceModels}, already turned for the facing) is tessellated like a
 * block at that quarter's own cell (its light, world ambient occlusion, faces culled against real
 * neighbours) in the solid layer. Nothing is drawn for an incomplete frame. The world model is empty,
 * so like the door the vanilla mining crack does not show on frames.
 */
public class RoundTrimRenderer implements BlockEntityRenderer<RoundTrimBlockEntity> {
    private final ModelBlockRenderer renderer;
    private final RandomSource random = RandomSource.create();

    public RoundTrimRenderer(BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    @Override
    public void render(RoundTrimBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = entity.getLevel();
        BlockState anchorState = entity.getBlockState();
        if (level == null || !(anchorState.getBlock() instanceof RoundTrimBlock block)
                || anchorState.getValue(RoundTrimBlock.PART) != TrimPart.LOWER_LEFT) {
            return;
        }
        TrimLayout.Assembly assembly = TrimLayout.of(entity.getBlockPos(), anchorState);
        if (!assembly.complete(level, block)) {
            return;
        }
        TrimLooks.Look look = TrimAppearanceModels.look(entity.appearance());
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        BlockPos anchor = assembly.anchor();
        for (TrimPart part : TrimPart.values()) {
            BlockPos cell = assembly.pos(part);
            BlockState state = level.getBlockState(cell);
            pose.pushPose();
            pose.translate(cell.getX() - anchor.getX(), cell.getY() - anchor.getY(), cell.getZ() - anchor.getZ());
            renderer.tesselateBlock(level, TrimAppearanceModels.quarter(look, assembly.facing(), part), state, cell, pose,
                    consumer, true, random, state.getSeed(cell), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
            pose.popPose();
        }
    }

    /** The frame spans up to four chunk sections; draw it whenever its bounds are in view. */
    @Override
    public boolean shouldRenderOffScreen(RoundTrimBlockEntity entity) {
        return true;
    }
}
