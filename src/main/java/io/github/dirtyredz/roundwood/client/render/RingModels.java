package io.github.dirtyredz.roundwood.client.render;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

import java.util.function.UnaryOperator;

/**
 * Frame and door-ring quarters in an appearance, from the oak source models: front and back faces
 * take the face artwork, the ring's sides the edge artwork ({@link TrimLooks}; masonry shows its
 * thin-mortar radial brick face across the whole ring, 0.7.0), and a quarter is turned for its
 * facing here (positions, normals, winding and cull sides) so the anchor's renderer can tessellate
 * it with world light at the quarter's own cell. (The item icon is the Universal Trim's,
 * {@code client/item/TrimItemModels}, since 0.9.0.)
 */
public final class RingModels {
    private record Key(TrimLooks.Look look, Direction facing, Quarter part) {
    }

    private static final AppearanceModelCache<Key, BakedModel> QUARTERS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private RingModels() {
    }

    public static TrimLooks.Look look(Appearance appearance) {
        return TrimLooks.of(appearance, MasonryStyles::doorFace);
    }

    /** The quarter {@code part} of a frame facing {@code facing}, in {@code look}, turned into place. */
    public static BakedModel quarter(TrimLooks.Look look, Direction facing, Quarter part) {
        return QUARTERS.get(new Key(look, facing, part), key -> {
            BakedModel source = Minecraft.getInstance().getModelManager()
                    .getModel(RingModelNames.quarterLocation(part, look.whole()));
            int turns = VoxelShapeFactory.quarterTurns(facing);
            UnaryOperator<BakedQuad> paint = painter(look);
            return ModelRetexturing.map(source, quad -> ModelRetexturing.rotateY(paint.apply(quad), turns),
                    side -> VoxelShapeFactory.turn(side, turns));
        });
    }

    private static UnaryOperator<BakedQuad> painter(TrimLooks.Look look) {
        TextureAtlasSprite face = DonorSprites.atlas(look.face());
        TextureAtlasSprite edge = DonorSprites.atlas(look.edge());
        return quad -> ModelRetexturing.retexture(quad, quad.getDirection().getAxis() == Direction.Axis.Z ? face : edge, -1);
    }
}
