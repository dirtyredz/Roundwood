package io.github.dirtyredz.roundwood.client.door;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Draws the whole round leaf of an assembly from its anchor: the finish's baked leaf model for the
 * hinge side, turned for the facing, then swung about the hinge axis by the animated angle. Uses the
 * normal baked-model path (entity-solid block sheet, packed light, overlay), no custom shader.
 * Incomplete assemblies draw nothing, so a broken door never shows a phantom leaf.
 */
public class RoundDoorRenderer implements BlockEntityRenderer<RoundDoorBlockEntity> {
    private final BlockRenderDispatcher blocks;
    /** Model names per door block and hinge; the baked models themselves are looked up each frame (reload-safe). */
    private final Map<Block, Map<DoorHingeSide, ResourceLocation>> names = new IdentityHashMap<>();

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
        DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(leafModel(door, hinge));
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
        pose.mulPose(Axis.YP.rotationDegrees((float) (DoorGeometry.openDegrees(hinge) * progress)));
        pose.translate(-pivotX, 0, -pivotZ);
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.solidBlockSheet()), state, model,
                1F, 1F, 1F, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }

    /** The leaf projects past the anchor's chunk section; render it whenever its bounds are in view. */
    @Override
    public boolean shouldRenderOffScreen(RoundDoorBlockEntity entity) {
        return true;
    }

    private ResourceLocation leafModel(Block door, DoorHingeSide hinge) {
        return names.computeIfAbsent(door, block -> {
            String id = ForgeRegistries.BLOCKS.getKey(block).getPath();
            Map<DoorHingeSide, ResourceLocation> byHinge = new EnumMap<>(DoorHingeSide.class);
            for (DoorHingeSide side : DoorHingeSide.values()) {
                byHinge.put(side, DoorLeafModels.location(id, side));
            }
            return byHinge;
        }).get(hinge);
    }
}
