package io.github.dirtyredz.roundwood.testsupport;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** No SimpleBakedModel static RenderType initialization in plain JUnit. */
public record QuadModel(List<BakedQuad> quads, TextureAtlasSprite sprite) implements BakedModel {
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return side == null ? quads : List.of(); }
    public boolean useAmbientOcclusion() { return true; }
    public boolean usesBlockLight() { return true; }
    public boolean isGui3d() { return false; }
    public boolean isCustomRenderer() { return false; }
    public TextureAtlasSprite getParticleIcon() { return sprite; }
    public ItemOverrides getOverrides() { return null; }
}
