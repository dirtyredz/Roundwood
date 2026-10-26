package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.client.material.AppearanceLooks;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.render.GeneralQuadModels;
import io.github.dirtyredz.roundwood.client.render.SurfaceQuadBaker;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.function.Function;

/** Cached cell models: the same donor on every surface, projected after facing rotation. */
public final class ArchPieceModels {
    public record Look(ResourceLocation texture, boolean masonry) {}
    private record Key(ArchPieceDesign design, Side side, ArchPieceCell cell, Direction facing,
                       double back, double front, Look look) {}
    private static final AppearanceModelCache<Key, BakedModel> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    private ArchPieceModels() {}

    public static Look look(Appearance appearance) { return look(appearance, AppearanceLooks::planks); }
    static Look look(Appearance appearance, Function<Appearance, ResourceLocation> textures) {
        return new Look(textures.apply(appearance), !appearance.isWood());
    }

    public static BakedModel body(ArchPieceDesign design, Side side, ArchPieceCell cell, Direction facing, Look look) {
        return model(new Key(design, side, cell, facing, 0, 16, look));
    }

    public static BakedModel casing(ArchPieceDesign design, Side side, AttachmentFace face, ArchPieceCell cell, Direction facing, Look look) {
        double[] depth = ArchPieceTrimGeometry.depth(face);
        return model(new Key(design, side, cell, facing, depth[0], depth[1], look));
    }

    private static BakedModel model(Key key) {
        return MODELS.get(key, ignored -> {
            var sprite = DonorSprites.atlas(key.look.texture());
            return GeneralQuadModels.world(quads(key.design, key.side, key.cell, key.back, key.front, key.facing, sprite), sprite);
        });
    }

    /** Production baker with an explicit sprite, also exercised without a client/GL context. */
    static List<BakedQuad> quads(ArchPieceDesign design, Side side, ArchPieceCell cell, double back, double front,
                                  Direction facing, TextureAtlasSprite sprite) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        return ArchPieceGeometry.surfaces(design, side, cell.column, cell.row, back, front, "").stream()
                .map(surface -> SurfaceQuadBaker.bake(surface.backing().rotateY(turns),
                        VoxelShapeFactory.turn(surface.face(), turns), sprite)).toList();
    }
}
