package io.github.dirtyredz.roundwood.client.camouflage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Optional;

/**
 * Draws an assembly's static camouflage corners from its anchor, outside the leaf's swing: one
 * cached corner model per cell, tessellated like a block at that cell (the cell's own light, world
 * ambient occlusion and biome tint) in the solid layer, the same path vanilla uses for moving
 * blocks. Nothing is drawn without camouflage.
 */
public final class CamouflageRenderer {
    private static final RandomSource RANDOM = RandomSource.create();

    private CamouflageRenderer() {
    }

    /** {@code pose} is at the anchor cell's origin, before any facing or swing transform. */
    public static void render(RoundDoorBlockEntity entity, DoorLayout.Assembly assembly, Level level, PoseStack pose,
                              MultiBufferSource buffers) {
        Optional<CamouflageData> camouflage = entity.camouflage();
        if (camouflage.isEmpty()) {
            return;
        }
        BlockState donor = camouflage.get().material();
        ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        BlockPos anchor = assembly.anchor();
        for (DoorLayout.Part part : assembly.parts()) {
            BlockPos cell = assembly.pos(part);
            pose.pushPose();
            pose.translate(cell.getX() - anchor.getX(), cell.getY() - anchor.getY(), cell.getZ() - anchor.getZ());
            renderer.tesselateBlock(level, CamouflageModels.quarter(donor, assembly.design(), assembly.facing(), part), donor, cell, pose, consumer,
                    false, RANDOM, donor.getSeed(cell), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
            pose.popPose();
        }
    }
}
