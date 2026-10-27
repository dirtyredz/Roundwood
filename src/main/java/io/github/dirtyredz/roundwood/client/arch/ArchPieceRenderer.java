package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.client.render.AssemblyCellRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Draws a whole piece from its anchor (0.14.0), like the frame and the doors: each cell's piece of
 * the frame in the piece's look ({@link ArchPieceModels#body}), then each installed face casing's
 * piece, tessellated like a block at that cell's own position (its light and world ambient
 * occlusion) in the solid layer. Nothing is drawn for an incomplete piece. The world model is empty,
 * so (like the door and frame) the vanilla mining crack does not show on pieces.
 */
public class ArchPieceRenderer implements BlockEntityRenderer<ArchPieceBlockEntity> {
    private final AssemblyCellRenderer renderer;

    public ArchPieceRenderer(AssemblyCellRenderer renderer) { this.renderer = renderer; }

    public ArchPieceRenderer(BlockEntityRendererProvider.Context context) {
        this.renderer = new AssemblyCellRenderer(context.getBlockRenderDispatcher().getModelRenderer(), RandomSource.create());
    }

    @Override
    public void render(ArchPieceBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!io.github.dirtyredz.roundwood.client.render.StaticRenderingConfig.legacy()) return;
        Level level = entity.getLevel();
        BlockState anchorState = entity.getBlockState();
        if (level == null || !(anchorState.getBlock() instanceof ArchPieceBlock block)
                || anchorState.getValue(ArchPieceBlock.CELL) != ArchPieceCell.LOWER_LEFT) {
            return;
        }
        ArchPieceLayout.Assembly assembly = ArchPieceLayout.of(entity.getBlockPos(), anchorState);
        if (!assembly.complete(level, block)) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        ArchPieceModels.Look body = ArchPieceModels.look(entity.appearance());
        for (ArchPieceCell part : assembly.parts()) {
            draw(level, assembly, part, ArchPieceModels.body(assembly.design(), assembly.side(), part, assembly.facing(), assembly.back(), assembly.front(), body), pose, consumer);
        }
        for (AttachmentFace face : AttachmentFace.values()) {
            Optional<Appearance> trim = entity.trims().get(face);
            if (trim.isEmpty()) {
                continue;
            }
            ArchPieceModels.Look look = ArchPieceModels.look(trim.get());
            for (ArchPieceCell part : assembly.parts()) {
                draw(level, assembly, part, ArchPieceModels.casing(assembly.design(), assembly.side(), face, part, assembly.facing(), assembly.back(), assembly.front(), look), pose,
                        consumer);
            }
        }
    }

    private void draw(Level level, ArchPieceLayout.Assembly assembly, ArchPieceCell part, BakedModel model, PoseStack pose,
                      VertexConsumer consumer) {
        BlockPos cell = assembly.pos(part);
        BlockState state = level.getBlockState(cell);
        BlockPos anchor = assembly.anchor();
        renderer.draw(level, anchor, cell, model, state, pose, consumer, false);
    }

    /** The piece spans up to four chunk sections; draw it whenever its bounds are in view. */
    @Override
    public boolean shouldRenderOffScreen(ArchPieceBlockEntity entity) {
        return true;
    }
}
