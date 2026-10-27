package io.github.dirtyredz.roundwood.client.render;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/** Marks models whose breaking data must be resolved from the supplied region, including satellites. */
public interface StaticGeometryModel {
    static ModelData breakingData(BakedModel model, BlockAndTintGetter region, BlockPos pos, BlockState state, ModelData supplied) {
        return model instanceof StaticGeometryModel ? model.getModelData(region, pos, state, supplied) : supplied;
    }
}
