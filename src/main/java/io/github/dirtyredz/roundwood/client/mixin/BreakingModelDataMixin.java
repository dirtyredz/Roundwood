package io.github.dirtyredz.roundwood.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.client.render.StaticGeometryModel;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Forge-added overload has a stable, unmapped name. No tessellator internals are intercepted. */
@Mixin(BlockRenderDispatcher.class)
public abstract class BreakingModelDataMixin {
    @ModifyVariable(method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraftforge/client/model/data/ModelData;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1, expect = 1, allow = 1, remap = false)
    private ModelData roundwood$breakingData(ModelData data, BlockState state, BlockPos pos, BlockAndTintGetter region,
                                             PoseStack pose, VertexConsumer consumer, ModelData supplied) {
        var model = ((BlockRenderDispatcher) (Object) this).getBlockModel(state);
        return StaticGeometryModel.breakingData(model, region, pos, state, data);
    }
}
