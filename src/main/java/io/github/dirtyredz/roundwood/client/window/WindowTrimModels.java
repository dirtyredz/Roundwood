package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.BuiltinWoodStyles;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.PaintStyles;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * The window trim's artwork: what a trim look draws on its face and its edges. Since 0.8.0 the
 * attachment renderer ({@link WindowTrimWorldModels}) builds the trim's quads from it directly; the
 * generated markers {@code window_trim_face} and {@code _edge} are kept as textures but no model uses
 * them any more:
 * <ul>
 *     <li>vanilla woods: their stripped log, tiled like a block (the round trim's wood);</li>
 *     <li>other woods: the donor plank sprite, tiled;</li>
 *     <li>paint: the painted door boards spread once over the face ({@link WindowTrimUv});</li>
 *     <li>masonry: the thin-mortar face for the profile spread once over the round window or the whole
 *     2 x 2 arched window, and the granular edge;</li>
 *     <li>an unavailable material: oak.</li>
 * </ul>
 * Glass and pane wood are never touched. The window trim item resolves its stack the same way.
 */
public final class WindowTrimModels {
    /** What a trim look draws: whole-face mapping or tiled, and the face and edge artwork. */
    public record Look(boolean whole, ResourceLocation face, ResourceLocation edge) {
    }

    private static final Look OAK = new Look(false, BuiltinWoodStyles.OAK_TRIM_LOG, BuiltinWoodStyles.OAK_TRIM_LOG);
    private static final AppearanceModelCache<Look, BakedModel> ITEMS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private WindowTrimModels() {
    }

    public static Look look(Appearance appearance, WindowProfile profile) {
        if (appearance.paint().isPresent()) {
            ResourceLocation panel = PaintStyles.doorPanel(appearance.paint().get());
            return new Look(true, panel, panel);
        }
        if (!appearance.isWood()) {
            return MasonryMaterials.byBlock(appearance.material())
                    .map(m -> new Look(true, MasonryStyles.windowFace(m, profile), MasonryStyles.edge(m)))
                    .orElse(OAK);
        }
        Optional<BuiltinWoodStyles.Style> builtin = BuiltinWoodStyles.of(appearance.material());
        if (builtin.isPresent()) {
            return new Look(false, builtin.get().trimLog(), builtin.get().trimLog());
        }
        return DonorSprites.sprite(appearance.material())
                .map(sprite -> new Look(false, sprite.contents().name(), sprite.contents().name()))
                .orElse(OAK);
    }

    /**
     * The window trim item in {@code appearance}: its ring model (oak stripped log, whole-face UVs over
     * 16 pixels, which for wood equal the tiled ones) with the front and back retextured to the face
     * artwork and the sides to the edge artwork.
     */
    public static BakedModel item(BakedModel oakItem, Appearance appearance) {
        Look look = look(appearance, WindowProfile.ROUND);
        if (look.equals(OAK)) {
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
