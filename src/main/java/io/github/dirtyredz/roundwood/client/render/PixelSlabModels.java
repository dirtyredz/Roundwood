package io.github.dirtyredz.roundwood.client.render;

import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.tint.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import io.github.dirtyredz.roundwood.geometry.PixelSlab;
import io.github.dirtyredz.roundwood.geometry.Surface;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Baked models of a flat pixel mask extruded between two depths, in one trim look (extracted in 0.12.0
 * from the arched door's casing models, which the archways' frames and casings now share): one
 * cell's visible surfaces ({@link PixelSlab}), its front and back faces with the look's face artwork
 * and every side with its edge artwork, turned for the facing, with no cull side (a piece standing
 * proud of its cell must never be dropped because the neighbouring cell is solid). Wood tiles like a
 * block; whole looks (paint, masonry) spread one image over the assembly's face
 * ({@link AssemblyFaceUv}), the back mirrored. The caller caches the result.
 */
public final class PixelSlabModels {
    private static final FaceBakery BAKERY = new FaceBakery();

    /**
     * Where one assembly sits in its whole-face image: its width and height in pixels, and how many
     * pixels the (square) image spans ({@link AssemblyFaceUv#point}).
     */
    public record Extent(double width, double height, double image) {
    }

    private PixelSlabModels() {
    }

    /**
     * The surfaces of {@code mask} (assembly pixels) inside the cell at ({@code column}, {@code row}),
     * between depths {@code back} and {@code front}, turned for {@code facing}, in {@code look}.
     */
    public static BakedModel cell(PixelCircle.PixelTest mask, int column, int row, double back, double front, Direction facing,
                                  TrimLooks.Look look, Extent extent) {
        return model(quads(mask, column, row, back, front, VoxelShapeFactory.quarterTurns(facing), false, look, extent), look,
                ItemTransforms.NO_TRANSFORMS, true, false);
    }

    /**
     * The whole mask as one unturned model in assembly coordinates (every cell's piece moved to its place),
     * for an inventory model drawn like {@code oakItem} (its transforms and GUI lighting).
     */
    public static BakedModel whole(PixelCircle.PixelTest mask, int columns, int rows, double back, double front,
                                   TrimLooks.Look look, Extent extent, BakedModel oakItem) {
        List<BakedQuad> quads = new ArrayList<>();
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                quads.addAll(quads(mask, column, row, back, front, 0, true, look, extent));
            }
        }
        return model(quads, look, oakItem.getTransforms(), oakItem.usesBlockLight(), true);
    }

    /**
     * One cell's quads: cell-local and turned by {@code turns}, or ({@code inPlace}, unturned) moved to
     * the cell's place in the assembly.
     */
    private static List<BakedQuad> quads(PixelCircle.PixelTest mask, int column, int row, double back, double front, int turns,
                                         boolean inPlace, TrimLooks.Look look, Extent extent) {
        return quads(mask, column, row, back, front, turns, inPlace, look, extent, DonorSprites::atlas);
    }

    /** The same baking path with an explicit atlas lookup, usable without a running client. */
    static List<BakedQuad> quads(PixelCircle.PixelTest mask, int column, int row, double back, double front, int turns,
                                boolean inPlace, TrimLooks.Look look, Extent extent,
                                Function<ResourceLocation, TextureAtlasSprite> atlas) {
        return quads(PixelSlab.cellFaces(mask, 16 * column, 16 * row, back, front, ""), column, row, turns, inPlace, look, extent, atlas);
    }

    /** Bake caller-owned visible surfaces, retaining the slab's material and UV policy. */
    public static BakedModel cell(List<Surface> surfaces, int column, int row, Direction facing, TrimLooks.Look look, Extent extent) {
        return model(quads(surfaces, column, row, VoxelShapeFactory.quarterTurns(facing), false, look, extent, DonorSprites::atlas),
                look, ItemTransforms.NO_TRANSFORMS, true, false);
    }

    public static List<BakedQuad> quads(List<Surface> surfaces, int column, int row, int turns, boolean inPlace,
                                       TrimLooks.Look look, Extent extent, Function<ResourceLocation, TextureAtlasSprite> atlas) {
        TextureAtlasSprite faceSprite = atlas.apply(look.face());
        TextureAtlasSprite edgeSprite = atlas.apply(look.edge());
        int originX = 16 * column;
        int originY = 16 * row;
        List<BakedQuad> quads = new ArrayList<>();
        for (Surface surface : surfaces) {
            boolean planar = surface.face().getAxis() == Direction.Axis.Z;
            TextureAtlasSprite sprite = planar ? faceSprite : edgeSprite;
            Box box = inPlace ? surface.backing().translate(originX, originY, 0) : surface.backing().rotateY(turns);
            Direction side = VoxelShapeFactory.turn(surface.face(), turns);
            BakedQuad quad = bake(box, side, sprite);
            if (planar && look.whole()) {
                boolean seenFromFront = surface.face() == Direction.SOUTH;
                double cellX = inPlace ? 0 : originX;
                double cellY = inPlace ? 0 : originY;
                quad = ModelRetexturing.retexture(quad, sprite, (x, y, z) -> wholeUv(cellX, cellY, turns, seenFromFront,
                        x * 16.0, y * 16.0, z * 16.0, extent));
            }
            quads.add(quad);
        }
        return quads;
    }

    private static BakedModel model(List<BakedQuad> quads, TrimLooks.Look look, ItemTransforms transforms, boolean blockLight, boolean item) {
        Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            culled.put(direction, List.of());
        }
        return new SimpleBakedModel(List.copyOf(quads), culled, true, blockLight, item, DonorSprites.atlas(look.face()), transforms,
                ItemOverrides.EMPTY);
    }

    /**
     * The whole-face UV of a facing-turned point whose cell starts at ({@code cellX}, {@code cellY}) of the
     * assembly: turned back into the assembly's frame first.
     */
    private static float[] wholeUv(double cellX, double cellY, int turns, boolean front, double x, double y, double z, Extent extent) {
        for (int i = 0; i < turns; i++) {
            double turnedX = z; // undo one clockwise turn: (x, z) <- (z, 16 - x)
            z = Box.BLOCK - x;
            x = turnedX;
        }
        double[] uv = AssemblyFaceUv.point(x + cellX, y + cellY, front, extent.width(), extent.height(), extent.image());
        return new float[] {(float) uv[0], (float) uv[1]};
    }

    private static BakedQuad bake(Box box, Direction side, TextureAtlasSprite sprite) {
        BlockElementFace element = new BlockElementFace(null, -1, "", new BlockFaceUV(FaceUv.tiled(box, side), 0));
        return BAKERY.bakeQuad(
                new Vector3f((float) box.minX(), (float) box.minY(), (float) box.minZ()),
                new Vector3f((float) box.maxX(), (float) box.maxY(), (float) box.maxZ()),
                element, sprite, side, BlockModelRotation.X0_Y0, null, true);
    }
}
