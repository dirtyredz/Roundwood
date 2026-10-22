package io.github.dirtyredz.roundwood.client.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

/**
 * The attachment items' icons in a stack's look: the Universal Trim's flat, face-on square frame
 * (0.9.0, {@code item/round_trim}) and the Arch Sill's flat bar (0.10.0, {@code item/arch_sill_trim}),
 * each in oak stripped log with its face mapped once over 16 pixels (which for a tiled wood equals the
 * default UVs). The front and back are retextured to the look's face artwork and the sides to its edge
 * ({@link TrimLooks}); masonry shows the slot's own straight-course face (the square frame, the sill's
 * course). Every id of an attachment (the 0.4.0 aliases in their historical looks, the 0.8.0 window
 * trim) resolves here, so no stack ever falls back to the old circular source models. Was
 * {@code UniversalTrimModels} (0.9.0).
 */
public final class TrimItemModels {
    private record Key(TrimSlot slot, TrimLooks.Look look) {
    }

    private static final AppearanceModelCache<Key, BakedModel> ITEMS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private TrimItemModels() {
    }

    public static TrimLooks.Look look(TrimSlot slot, Appearance appearance) {
        return TrimLooks.of(appearance, slot == TrimSlot.SILL ? MasonryStyles::sillFace : MasonryStyles::squareFace);
    }

    /** {@code oakItem} (the slot's oak icon) in {@code appearance}. */
    public static BakedModel item(TrimSlot slot, BakedModel oakItem, Appearance appearance) {
        TrimLooks.Look look = look(slot, appearance);
        if (look.equals(TrimLooks.OAK)) {
            return oakItem;
        }
        return ITEMS.get(new Key(slot, look), key -> {
            TextureAtlasSprite face = DonorSprites.atlas(look.face());
            TextureAtlasSprite edge = DonorSprites.atlas(look.edge());
            return ModelRetexturing.map(oakItem,
                    quad -> ModelRetexturing.retexture(quad, quad.getDirection().getAxis() == Direction.Axis.Z ? face : edge, -1),
                    side -> side);
        });
    }
}
