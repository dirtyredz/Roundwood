package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowSurfaceGeometry;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.client.render.SurfaceQuadBaker;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Composes frame, glazing, connector reach and independently styled beams before surface extraction. */
public final class WindowBeamModels {
    private record Key(WindowPaneState.Snapshot pane, ResourceLocation frame, ResourceLocation glass, ResourceLocation beams) { }
    private record Drawn(Surface surface, BakedQuad quad) { }
    private static final AppearanceModelCache<Key, List<Drawn>> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    private WindowBeamModels() { }

    /** Inventory geometry keeps the source's lighting, transforms and translucent render types. */
    public static net.minecraft.client.resources.model.BakedModel item(net.minecraft.client.resources.model.BakedModel source,
            List<BakedQuad> quads, net.minecraft.client.renderer.texture.TextureAtlasSprite particle) {
        List<BakedQuad> frozen = List.copyOf(quads);
        return new net.neoforged.neoforge.client.model.BakedModelWrapper<>(source) {
            @Override
            public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState state, Direction side,
                                            net.minecraft.util.RandomSource random) {
                return side == null ? frozen : List.of();
            }

            @Override
            public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState state, Direction side,
                    net.minecraft.util.RandomSource random, net.neoforged.neoforge.client.model.data.ModelData data,
                    net.minecraft.client.renderer.RenderType renderType) {
                return getQuads(state, side, random);
            }

            @Override
            public List<net.minecraft.client.resources.model.BakedModel> getRenderPasses(net.minecraft.world.item.ItemStack stack, boolean fabulous) {
                return List.of(this);
            }

            @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return particle; }
            @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData data) { return particle; }
        };
    }

    public static List<Surface> surfaces(WindowPaneState.Snapshot pane) {
        return WindowSurfaceGeometry.surfaces(WindowPaneGeometry.panel(pane.design(), pane.pose()), Set.of(WindowPaneGeometry.GLASS));
    }

    public static List<BakedQuad> quads(WindowPaneState.Snapshot pane, ResourceLocation frame, ResourceLocation glass,
                                      Appearance beams, Map<Direction, WindowPaneState.Snapshot> neighbours) {
        return MODELS.get(new Key(pane, frame, glass, TrimLooks.beams(beams).face()), key -> surfaces(key.pane()).stream().map(surface -> {
            ResourceLocation texture = switch (surface.texture()) {
                case WindowPaneGeometry.GLASS -> key.glass();
                case WindowPaneGeometry.BEAM -> key.beams();
                default -> key.frame();
            };
            return new Drawn(surface, SurfaceQuadBaker.bake(surface.backing(), surface.face(), DonorSprites.atlas(texture)));
        }).toList()).stream().filter(drawn -> WindowBeamCulling.visible(drawn.surface(), neighbours)).map(Drawn::quad).toList();
    }
}
