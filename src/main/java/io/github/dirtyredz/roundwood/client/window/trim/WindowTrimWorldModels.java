package io.github.dirtyredz.roundwood.client.window.trim;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimSurfaces;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimUv;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import io.github.dirtyredz.roundwood.client.render.SurfaceQuadBaker;

/**
 * Baked models of one face's window trim in one look (0.8.0), for {@link WindowTrimRenderer}: the
 * band's outward face and perimeter reveals ({@link WindowTrimSurfaces}) as general quads (no cull
 * side: a projecting trim must never be dropped because the neighbouring cell is solid; ordinary
 * depth testing decides what shows). Wood tiles like a block, the edge UVs folded back into the
 * sprite even at {@code -1} and {@code 17} ({@link FaceUv#tiled}); paint and masonry spread one image
 * over the piece's artwork extent ({@link WindowTrimUv}). Since 0.10.0 also the arch sill. Cached per slot, profile,
 * pose, side and look; cleared with every appearance cache on reload.
 */
public final class WindowTrimWorldModels {

    private record Key(TrimSlot slot, WindowProfile profile, WindowPose pose, Direction side, TrimLooks.Look look) {
    }

    private static final AppearanceModelCache<Key, BakedModel> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private WindowTrimWorldModels() {
    }

    /** The attachment in {@code slot} on {@code side} (along the normal) of a window posed {@code pose}, in {@code look}. */
    public static BakedModel model(TrimSlot slot, WindowProfile profile, WindowPose pose, Direction side, TrimLooks.Look look) {
        return MODELS.get(new Key(slot, profile, pose, side, look), WindowTrimWorldModels::build);
    }

    private static BakedModel build(Key key) {
        TextureAtlasSprite face = DonorSprites.atlas(key.look().face());
        TextureAtlasSprite edge = DonorSprites.atlas(key.look().edge());
        List<BakedQuad> quads = new ArrayList<>();
        for (Surface surface : WindowTrimSurfaces.surfaces(key.slot(), key.profile(), key.pose(), key.side())) {
            boolean outward = surface.texture().equals(WindowTrimGeometry.FACE);
            TextureAtlasSprite sprite = outward ? face : edge;
            BakedQuad quad = SurfaceQuadBaker.bake(surface.backing(), surface.face(), sprite);
            if (outward && key.look().whole()) {
                boolean front = surface.face() == key.pose().front();
                quad = ModelRetexturing.retexture(quad, sprite,
                        (x, y, z) -> WindowTrimUv.whole(key.profile(), key.pose(), x * 16.0, y * 16.0, z * 16.0, front));
            }
            quads.add(quad);
        }
        Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            culled.put(direction, List.of());
        }
        return new SimpleBakedModel(List.copyOf(quads), culled, true, true, false, face, ItemTransforms.NO_TRANSFORMS,
                ItemOverrides.EMPTY);
    }


}
