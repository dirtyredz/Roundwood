package io.github.dirtyredz.roundwood.client.door;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.client.camouflage.CamouflageRenderer;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Draws the whole round leaf of an assembly from its anchor: the leaf model for the hinge side in the
 * door's appearance ({@link DoorAppearanceModels}, cached, never rebuilt per frame), turned for the
 * facing, then swung about the hinge axis by the animated angle. Uses the normal baked-model path
 * (entity-solid block sheet, packed light, overlay), no custom shader; a donor tint reaches only the
 * panel's tinted quads, never the brass.
 * The static camouflage corners are drawn first, without the leaf's transforms ({@link CamouflageRenderer}).
 * Incomplete assemblies draw nothing, so a broken door never shows a phantom leaf.
 */
public class RoundDoorRenderer implements BlockEntityRenderer<RoundDoorBlockEntity> {
    private final BlockRenderDispatcher blocks;

    public RoundDoorRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(RoundDoorBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = entity.getLevel();
        BlockState state = entity.getBlockState();
        if (level == null || !(state.getBlock() instanceof RoundDoorBlock door)
                || DoorLayout.partOf(state) != DoorLayout.Part.LOWER_LEFT) {
            return;
        }
        DoorLayout.Assembly assembly = DoorLayout.of(entity.getBlockPos(), state);
        if (!assembly.complete(level, door)) {
            return;
        }
        // The corners stay in the closed door plane: drawn before any facing or swing transform.
        CamouflageRenderer.render(entity, assembly, level, pose, buffers);

        DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
        DoorAppearanceModels.Look look = DoorAppearanceModels.look(entity.appearance());
        BakedModel model = DoorAppearanceModels.leaf(look, hinge);
        float red = look.tint() < 0 ? 1F : ((look.tint() >> 16) & 0xFF) / 255F;
        float green = look.tint() < 0 ? 1F : ((look.tint() >> 8) & 0xFF) / 255F;
        float blue = look.tint() < 0 ? 1F : (look.tint() & 0xFF) / 255F;
        double progress = entity.swing().progress(level.getGameTime() + partialTick);

        pose.pushPose();
        // Authored facing north; turn about the anchor cell's centre like a blockstate y rotation.
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-90F * VoxelShapeFactory.quarterTurns(state.getValue(DoorBlock.FACING))));
        pose.translate(-0.5, 0, -0.5);
        // Swing the whole leaf about its hinge axis.
        double pivotX = DoorGeometry.pivotX(hinge) / 16.0;
        double pivotZ = DoorGeometry.PIVOT_Z / 16.0;
        pose.translate(pivotX, 0, pivotZ);
        pose.mulPose(Axis.YP.rotationDegrees((float) DoorGeometry.swingDegrees(hinge, progress)));
        pose.translate(-pivotX, 0, -pivotZ);
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.solidBlockSheet()), state, model,
                red, green, blue, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }

    /** The leaf projects past the anchor's chunk section; render it whenever its bounds are in view. */
    @Override
    public boolean shouldRenderOffScreen(RoundDoorBlockEntity entity) {
        return true;
    }
}
