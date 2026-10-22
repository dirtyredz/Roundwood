package io.github.dirtyredz.roundwood.client.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

/**
 * The Universal Trim's icon in a stack's look (0.9.0): the flat, face-on square frame
 * ({@code item/round_trim}, oak stripped log, its face mapped once over 16 pixels, which for a tiled
 * wood equals the default UVs) with the front and back retextured to the look's face artwork and the
 * sides to its edge ({@link TrimLooks}; masonry shows the square frame's straight-course face). Every
 * id of the attachment (the 0.4.0 aliases in their historical looks, the 0.8.0 window trim) resolves
 * here, so no stack ever falls back to the old circular source models.
 */
public final class UniversalTrimModels {
    private static final AppearanceModelCache<TrimLooks.Look, BakedModel> ITEMS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private UniversalTrimModels() {
    }

    public static TrimLooks.Look look(Appearance appearance) {
        return TrimLooks.of(appearance, MasonryStyles::squareFace);
    }

    /** {@code oakItem} (the square oak icon) in {@code appearance}. */
    public static BakedModel item(BakedModel oakItem, Appearance appearance) {
        TrimLooks.Look look = look(appearance);
        if (look.equals(TrimLooks.OAK)) {
            return oakItem;
        }
        return ITEMS.get(look, key -> {
            TextureAtlasSprite face = DonorSprites.atlas(look.face());
            TextureAtlasSprite edge = DonorSprites.atlas(look.edge());
            return ModelRetexturing.map(oakItem,
                    quad -> ModelRetexturing.retexture(quad, quad.getDirection().getAxis() == Direction.Axis.Z ? face : edge, -1),
                    side -> side);
        });
    }
}
