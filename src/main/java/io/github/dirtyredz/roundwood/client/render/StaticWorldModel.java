package io.github.dirtyredz.roundwood.client.render;

import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceWorldModel;
import io.github.dirtyredz.roundwood.client.door.DoorStaticModel;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.render.assembly.AssemblyModelData;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import java.util.*;

/** Forge model API only. Chunk builders get immutable, cached pre-lighting quads. */
public final class StaticWorldModel extends BakedModelWrapper<BakedModel> implements StaticGeometryModel {
    private record Key(BlockState state, Direction side, RenderType layer, AssemblyRenderSnapshot snapshot) {}
    private static final AppearanceModelCache<Key, List<BakedQuad>> QUADS = new AppearanceModelCache<>(4096);
    public StaticWorldModel(BakedModel original) { super(original); }
    public static void wrapBlocks(Map<ResourceLocation, BakedModel> models) {
        var blocks = new ArrayList<>(ModBlocks.canonical());
        blocks.addAll(LegacyContent.blocks());
        blocks.addAll(ModBlocks.archways());
        for (var block : blocks) if (block instanceof AssemblyDoorBlock || block instanceof ArchPieceBlock
                || block instanceof io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock
                || block instanceof io.github.dirtyredz.roundwood.block.archway.ArchwayBlock) {
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var location = BlockModelShaper.stateToModelLocation(state);
                var model = models.get(location);
                if (model != null) models.put(location, new StaticWorldModel(model));
            }
        }
    }
    @Override public ModelData getModelData(BlockAndTintGetter region, BlockPos pos, BlockState state, ModelData data) {
        return AssemblyModelData.resolve(region, pos, state, data);
    }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.solid());
    }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType layer) {
        if (StaticRenderingConfig.legacy() || layer != null && layer != RenderType.solid()) return List.of();
        var snapshot = data.get(AssemblyModelData.PROPERTY);
        if (state == null || snapshot == null || !snapshot.available()) return List.of();
        return QUADS.get(new Key(state, side, layer, snapshot), key -> {
            if (state.getBlock() instanceof io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock)
                return List.copyOf(io.github.dirtyredz.roundwood.client.trim.TrimWorldModel.model(state, snapshot)
                        .getQuads(state, side, random, ModelData.EMPTY, layer));
            if (side != null) return List.of();
            return collect(models(state, snapshot), state, random, layer);
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
    private static List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot) {
        if (state.getBlock() instanceof io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock)
            return List.of(io.github.dirtyredz.roundwood.client.trim.TrimWorldModel.model(state, snapshot));
        if (state.getBlock() instanceof io.github.dirtyredz.roundwood.block.archway.ArchwayBlock)
            return io.github.dirtyredz.roundwood.client.archway.ArchwayWorldModel.models(state, snapshot);
        return state.getBlock() instanceof ArchPieceBlock ? ArchPieceWorldModel.models(state, snapshot) : DoorStaticModel.models(state, snapshot);
    }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) {
        var snapshot = data.get(AssemblyModelData.PROPERTY);
        if (snapshot != null) {
            if (snapshot.assembly() instanceof io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly) {
                var appearance = snapshot.frame().map(value -> value.appearance()).orElse(snapshot.appearance());
                return io.github.dirtyredz.roundwood.client.material.DonorSprites.atlas(
                        io.github.dirtyredz.roundwood.client.material.AppearanceLooks.planks(appearance));
            }
            var state = snapshot.members().get(snapshot.assembly().anchor());
            if (state != null) {
                var models = models(state, snapshot);
                if (!models.isEmpty()) return models.get(0).getParticleIcon(ModelData.EMPTY);
            }
        }
        return super.getParticleIcon(data);
    }
}
