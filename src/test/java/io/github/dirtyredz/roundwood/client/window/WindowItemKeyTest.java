package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowItemKeyTest {
    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("roundwood", name);
    }

    private static BakedModel source() {
        return new BakedModel() {
            @Override public java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(
                    net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.Direction side,
                    net.minecraft.util.RandomSource random) { return java.util.List.of(); }
            @Override public boolean useAmbientOcclusion() { return true; }
            @Override public boolean usesBlockLight() { return true; }
            @Override public boolean isGui3d() { return false; }
            @Override public boolean isCustomRenderer() { return false; }
            @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
            @Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return null; }
        };
    }

    @Test
    void sourceFrameGlassAndIndependentBeamsAllDistinguishCachedItems() {
        BakedModel source = source();
        var wood = texture("spruce");
        var glass = texture("clear");
        var key = new WindowAppearanceModel.ItemKey(source, wood, glass, null);
        var cache = new AppearanceModelCache<WindowAppearanceModel.ItemKey, Object>(16);
        Object first = cache.get(key, ignored -> new Object());
        assertSame(first, cache.get(new WindowAppearanceModel.ItemKey(source, wood, glass, null), ignored -> new Object()));
        for (var distinct : java.util.List.of(
                new WindowAppearanceModel.ItemKey(source(), wood, glass, null),
                new WindowAppearanceModel.ItemKey(source, texture("birch"), glass, null),
                new WindowAppearanceModel.ItemKey(source, wood, texture("red"), null),
                new WindowAppearanceModel.ItemKey(source, wood, glass, wood),
                new WindowAppearanceModel.ItemKey(source, wood, glass, texture("birch")))) {
            assertNotSame(first, cache.get(distinct, ignored -> new Object()));
        }
        assertEquals(6, cache.size());
        cache.clear();
        assertNotSame(first, cache.get(key, ignored -> new Object()));
    }
}
