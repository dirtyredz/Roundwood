package io.github.dirtyredz.roundwood.client.render.world;

import io.github.dirtyredz.roundwood.testsupport.QuadModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StaticBreakingModelTest {
    @Test void onlyMarkedModelsResolveDataAndReceiveTheOriginalContext() {
        var property = new ModelProperty<String>();
        var supplied = ModelData.builder().with(property, "old").build();
        var current = ModelData.builder().with(property, "new").build();
        var position = new BlockPos(16, 16, 16);
        class Marked extends BakedModelWrapper<BakedModel> implements StaticGeometryModel {
            Marked() { super(null); }
            @Override public ModelData getModelData(BlockAndTintGetter region, BlockPos pos, BlockState state, ModelData data) {
                assertSame(position, pos); assertSame(supplied, data); assertNull(region); assertNull(state);
                return current;
            }
        }
        assertSame(current, StaticGeometryModel.breakingData(new Marked(), null, position, null, supplied));
        assertSame(supplied, StaticGeometryModel.breakingData(new QuadModel(List.of(), null), null, position, null, supplied));
    }
}
