package io.github.dirtyredz.roundwood.client.render.world;

import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import java.util.*;

/**
 * Forge model API only. Chunk builders get immutable, cached pre-lighting quads. What a cell draws
 * comes from its family's {@link StaticModelSource}; this class never names a family.
 */
public final class StaticWorldModel extends BakedModelWrapper<BakedModel> implements StaticGeometryModel {
    private record Key(BlockState state, Direction side, RenderType layer, AssemblyRenderSnapshot snapshot) {}
    private static final AppearanceModelCache<Key, List<BakedQuad>> QUADS = new AppearanceModelCache<>(4096);
    private final StaticModelSource source;

    public StaticWorldModel(BakedModel original, StaticModelSource source) {
        super(original);
        this.source = source;
    }

    /** Wraps every blockstate model of each Roundwood block that has a bound static source. */
    public static void wrapBlocks(Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> models, StaticModelSources sources) {
        sources.forEach((block, source) -> {
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var location = BlockModelShaper.stateToModelLocation(state);
                var model = models.get(location);
                if (model != null) models.put(location, new StaticWorldModel(model, source));
            }
        });
    }
    @Override public ModelData getModelData(BlockAndTintGetter region, BlockPos pos, BlockState state, ModelData data) {
        return AssemblyModelData.resolve(region, pos, state, data);
    }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.solid());
    }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType layer) {
        if (layer != null && layer != RenderType.solid()) return List.of();
        var snapshot = data.get(AssemblyModelData.PROPERTY);
        if (state == null || snapshot == null || !snapshot.available()) return List.of();
        return QUADS.get(new Key(state, side, layer, snapshot), key -> {
            var models = source.models(state, snapshot);
            if (source.culledBySide()) return collectSide(models, state, side, random, layer);
            if (side != null) return List.of();
            return collect(models, state, random, layer);
        });
    }
    public static List<BakedQuad> collect(List<BakedModel> models, BlockState state, RandomSource random, RenderType layer) {
            var out = new ArrayList<BakedQuad>();
            for (var model : models) {
                out.addAll(model.getQuads(state, null, random, ModelData.EMPTY, layer));
                for (var direction : Direction.values()) out.addAll(model.getQuads(state, direction, random, ModelData.EMPTY, layer));
            }
            return List.copyOf(out);
    }
    private static List<BakedQuad> collectSide(List<BakedModel> models, BlockState state, Direction side, RandomSource random, RenderType layer) {
        var out = new ArrayList<BakedQuad>();
        for (var model : models) out.addAll(model.getQuads(state, side, random, ModelData.EMPTY, layer));
        return List.copyOf(out);
    }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) {
        var snapshot = data.get(AssemblyModelData.PROPERTY);
        if (snapshot != null) {
            var particle = source.particle(snapshot);
            if (particle != null) return particle;
        }
        return super.getParticleIcon(data);
    }
}
