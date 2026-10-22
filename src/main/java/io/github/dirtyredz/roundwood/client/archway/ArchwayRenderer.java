package io.github.dirtyredz.roundwood.client.archway;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Optional;

/**
 * Draws a whole archway from its anchor (0.12.0), like the frame and the doors: each cell's piece of
 * the frame in the archway's look ({@link ArchwayModels#body}), then each installed face casing's
 * piece, tessellated like a block at that cell's own position (its light and world ambient
 * occlusion) in the solid layer. Nothing is drawn for an incomplete archway. The world model is empty,
 * so (like the door and frame) the vanilla mining crack does not show on archways.
 */
public class ArchwayRenderer implements BlockEntityRenderer<ArchwayBlockEntity> {
    private final ModelBlockRenderer renderer;
    private final RandomSource random = RandomSource.create();

    public ArchwayRenderer(BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    @Override
    public void render(ArchwayBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = entity.getLevel();
        BlockState anchorState = entity.getBlockState();
        if (level == null || !(anchorState.getBlock() instanceof ArchwayBlock block)
                || anchorState.getValue(ArchwayBlock.PART) != ArchwayPart.LOWER_LEFT) {
            return;
        }
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(entity.getBlockPos(), anchorState);
        if (!assembly.complete(level, block)) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        TrimLooks.Look body = ArchwayModels.look(assembly.design(), entity.appearance());
        for (ArchwayPart part : ArchwayPart.values()) {
            draw(level, assembly, part, ArchwayModels.body(assembly.design(), part, assembly.facing(), body), pose, consumer);
        }
        for (AttachmentFace face : AttachmentFace.values()) {
            Optional<Appearance> trim = entity.trims().get(face);
            if (trim.isEmpty()) {
                continue;
            }
            TrimLooks.Look look = ArchwayModels.look(assembly.design(), trim.get());
            for (ArchwayPart part : ArchwayPart.values()) {
                draw(level, assembly, part, ArchwayModels.casing(assembly.design(), face, part, assembly.facing(), look), pose,
                        consumer);
            }
        }
    }

    private void draw(Level level, ArchwayLayout.Assembly assembly, ArchwayPart part, BakedModel model, PoseStack pose,
                      VertexConsumer consumer) {
        BlockPos cell = assembly.pos(part);
        BlockState state = level.getBlockState(cell);
        BlockPos anchor = assembly.anchor();
        pose.pushPose();
        pose.translate(cell.getX() - anchor.getX(), cell.getY() - anchor.getY(), cell.getZ() - anchor.getZ());
        renderer.tesselateBlock(level, model, state, cell, pose, consumer, false, random, state.getSeed(cell),
                OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }

    /** The archway spans up to four chunk sections; draw it whenever its bounds are in view. */
    @Override
    public boolean shouldRenderOffScreen(ArchwayBlockEntity entity) {
        return true;
    }
}
