package io.github.dirtyredz.roundwood.client.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable world models whose surfaces must not be culled against neighbouring blocks. */
public final class GeneralQuadModels {
    private GeneralQuadModels() {}

    public static BakedModel world(List<BakedQuad> quads, TextureAtlasSprite particle) {
        Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) culled.put(side, List.of());
        return new SimpleBakedModel(List.copyOf(quads), culled, true, true, false,
                particle, ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY);
    }
}
