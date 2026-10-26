package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceItemGeometry;
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
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.function.Function;

/** Cached cell models: the same donor on every surface, projected after facing rotation. */
public final class ArchPieceModels {
    public record Look(ResourceLocation texture, boolean masonry) {}
    private record Key(ArchPieceDesign design, Side side, ArchPieceCell cell, Direction facing,
                       double back, double front, Look look) {}
    private static final AppearanceModelCache<Key, BakedModel> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    private record ItemKey(ArchPieceDesign design, Look look) {}
    private static final AppearanceModelCache<ItemKey, BakedModel> ITEMS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
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
            return GeneralQuadModels.world(quads(key.design, key.side, key.cell, key.back, key.front, key.facing, sprite, key.look.masonry()), sprite);
        });
    }

    /** Production baker with an explicit sprite, also exercised without a client/GL context. */
    static List<BakedQuad> quads(ArchPieceDesign design, Side side, ArchPieceCell cell, double back, double front,
                                  Direction facing, TextureAtlasSprite sprite) {
        return quads(design, side, cell, back, front, facing, sprite, false);
    }

    static List<BakedQuad> quads(ArchPieceDesign design, Side side, ArchPieceCell cell, double back, double front,
                                  Direction facing, TextureAtlasSprite sprite, boolean masonry) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        List<BakedQuad> quads = new ArrayList<>();
        var joints = masonry ? ArchMasonryModels.joints(design) : List.<List<ArchMasonryModels.Point>>of();
        for (var surface : ArchPieceGeometry.surfaces(design, side, cell.column, cell.row, back, front, "")) {
            var quad = SurfaceQuadBaker.bake(surface.backing().rotateY(turns), VoxelShapeFactory.turn(surface.face(), turns), sprite);
            if (masonry && surface.face().getAxis() == Direction.Axis.Z)
                quads.addAll(ArchMasonryModels.bake(surface, quad, cell.column * 16, cell.row * 16, turns, joints));
            else quads.add(quad);
        }
        return List.copyOf(quads);
    }

    public static BakedModel item(ArchPieceDesign design, BakedModel oak, Appearance appearance) {
        var look = look(appearance);
        return ITEMS.get(new ItemKey(design, look), ignored -> {
            var sprite = DonorSprites.atlas(look.texture());
            var culled = new EnumMap<Direction, List<BakedQuad>>(Direction.class);
            for (var direction : Direction.values()) culled.put(direction, List.of());
            return new SimpleBakedModel(itemQuads(design, sprite, look.masonry()), culled, true, oak.usesBlockLight(), true,
                    sprite, oak.getTransforms(), ItemOverrides.EMPTY);
        });
    }

    /** Same cell bake as the body, translated afterwards so authored donor UVs are preserved. */
    static List<BakedQuad> itemQuads(ArchPieceDesign design, TextureAtlasSprite sprite, boolean masonry) {
        List<BakedQuad> out = new ArrayList<>();
        for (var cell : ArchPieceCell.of(design)) {
            for (var quad : quads(design, Side.LEFT, cell, 0, 16, Direction.NORTH, sprite, masonry)) {
                int[] vertices = quad.getVertices().clone();
                int stride = vertices.length / 4;
                for (int i = 0; i < vertices.length; i += stride) {
                    vertices[i] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[i]) + cell.column + ArchPieceItemGeometry.offsetX(design) / 16F);
                    vertices[i + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[i + 1]) + cell.row);
                }
                out.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
            }
        }
        return List.copyOf(out);
    }
}
