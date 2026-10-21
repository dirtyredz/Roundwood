package io.github.dirtyredz.roundwood.client.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.BuiltinWoodStyles;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.PaintStyles;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Frame quarters (and frame items) in an appearance, from the oak source models: front and back
 * faces take the face artwork, the ring's sides the edge artwork, and a quarter is turned for its
 * facing here (positions, normals, winding and cull sides) so the anchor's renderer can tessellate
 * it with world light at the quarter's own cell. Looks:
 * <ul>
 *     <li>vanilla woods: their stripped log, tiled like a block (as since 0.4.0);</li>
 *     <li>other woods: the donor plank sprite, tiled;</li>
 *     <li>paint: the painted board panel spread across the whole ring;</li>
 *     <li>masonry: its radial brick face across the ring and granular edge;</li>
 *     <li>an unavailable material: oak.</li>
 * </ul>
 */
public final class TrimAppearanceModels {
    /** Which source family, and the face and edge artwork. */
    public record Look(boolean whole, ResourceLocation face, ResourceLocation edge) {
    }

    private record Key(Look look, Direction facing, TrimPart part) {
    }

    private static final Look OAK = new Look(false, BuiltinWoodStyles.OAK_TRIM_LOG, BuiltinWoodStyles.OAK_TRIM_LOG);
    private static final AppearanceModelCache<Key, BakedModel> QUARTERS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    private static final AppearanceModelCache<Look, BakedModel> ITEMS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private TrimAppearanceModels() {
    }

    public static Look look(Appearance appearance) {
        if (appearance.paint().isPresent()) {
            ResourceLocation panel = PaintStyles.doorPanel(appearance.paint().get());
            return new Look(true, panel, panel);
        }
        if (!appearance.isWood()) {
            return MasonryMaterials.byBlock(appearance.material())
                    .map(m -> new Look(true, PaintStyles.masonryFace(m), PaintStyles.masonryEdge(m)))
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

    /** The quarter {@code part} of a frame facing {@code facing}, in {@code look}, turned into place. */
    public static BakedModel quarter(Look look, Direction facing, TrimPart part) {
        return QUARTERS.get(new Key(look, facing, part), key -> {
            BakedModel source = Minecraft.getInstance().getModelManager()
                    .getModel(TrimModelNames.quarterLocation(part, look.whole()));
            int turns = VoxelShapeFactory.quarterTurns(facing);
            UnaryOperator<BakedQuad> paint = painter(look);
            return ModelRetexturing.map(source, quad -> ModelRetexturing.rotateY(paint.apply(quad), turns),
                    side -> ModelRetexturing.turn(side, turns));
        });
    }

    /** The frame's item model in {@code appearance}; {@code oakItem} is the tiled oak item model. */
    public static BakedModel item(BakedModel oakItem, Appearance appearance) {
        Look look = look(appearance);
        if (look.equals(OAK)) {
            return oakItem;
        }
        return ITEMS.get(look, key -> {
            BakedModel source = look.whole()
                    ? Minecraft.getInstance().getModelManager().getModel(TrimModelNames.wholeItemLocation()) : oakItem;
            return ModelRetexturing.map(source, painter(look), side -> side);
        });
    }

    private static UnaryOperator<BakedQuad> painter(Look look) {
        TextureAtlasSprite face = DonorSprites.atlas(look.face());
        TextureAtlasSprite edge = DonorSprites.atlas(look.edge());
        return quad -> ModelRetexturing.retexture(quad, quad.getDirection().getAxis() == Direction.Axis.Z ? face : edge, -1);
    }
}
