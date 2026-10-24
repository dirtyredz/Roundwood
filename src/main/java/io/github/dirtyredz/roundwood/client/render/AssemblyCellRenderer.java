package io.github.dirtyredz.roundwood.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

/** Draws a solid model relative to an assembly anchor using the destination cell's world light. */
public record AssemblyCellRenderer(ModelBlockRenderer renderer, RandomSource random) {
    public void draw(Level level, BlockPos anchor, BlockPos cell, BakedModel model, BlockState state,
                     PoseStack pose, VertexConsumer consumer, boolean cull) {
        draw(level, anchor, cell, model, state, pose, consumer, cull, state.getSeed(cell), Vec3.ZERO);
    }

    /** A projected ring retains the source door's seed and its own depth shift. */
    public void draw(Level level, BlockPos anchor, BlockPos cell, BakedModel model, BlockState state,
                     PoseStack pose, VertexConsumer consumer, boolean cull, long seed, Vec3 shift) {
        pose.pushPose();
        try {
            pose.translate(cell.getX() - anchor.getX() + shift.x, cell.getY() - anchor.getY() + shift.y,
                    cell.getZ() - anchor.getZ() + shift.z);
            renderer.tesselateBlock(level, model, state, cell, pose, consumer, cull, random, seed,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
        } finally {
            pose.popPose();
        }
    }
}
