package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimGeometry;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
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

/**
 * Baked models of one cell's piece of a door casing in one look (0.11.0), built from the design's
 * casing outline ({@link DoorTrimGeometry#inCasing}) rather than from the legacy frame's quarter
 * models, for {@link DoorTrimRenderer}: the arched door's jambs and crown. Each piece is the
 * casing's visible surfaces inside the cell ({@link PixelSlab}), its front and back included (the
 * opening leaf exposes the back), turned for the facing, with no cull side (a casing standing one
 * pixel proud must never be dropped because the neighbouring cell is solid). Wood tiles like a block
 * (its stripped log); paint and masonry spread one image over the whole door's face
 * ({@link AssemblyFaceUv}, the board panel's left half for paint, the arched casing's own masonry
 * course for masonry), the edges granular. Cached per design, face, cell, facing and look; cleared
 * with every appearance cache on reload.
 */
public final class DoorTrimModels {
    private static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "door_casing");
    private static final FaceBakery BAKERY = new FaceBakery();

    private record Key(DoorDesign design, AttachmentFace face, DoorLayout.Part part, Direction facing, TrimLooks.Look look) {
    }

    private static final AppearanceModelCache<Key, BakedModel> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private DoorTrimModels() {
    }

    /** What {@code appearance} draws on {@code design}'s casing (masonry follows the casing's contour). */
    public static TrimLooks.Look look(DoorDesign design, Appearance appearance) {
        return TrimLooks.of(appearance, material -> design == DoorDesign.ROUND
                ? MasonryStyles.doorFace(material) : MasonryStyles.archedDoorCasingFace(material));
    }

    /** The piece of {@code face}'s casing in {@code part}'s cell of a door facing {@code facing}, in {@code look}. */
    public static BakedModel casing(DoorDesign design, AttachmentFace face, DoorLayout.Part part, Direction facing,
                                    TrimLooks.Look look) {
        return MODELS.get(new Key(design, face, part, facing, look), DoorTrimModels::build);
    }

    private static BakedModel build(Key key) {
        TextureAtlasSprite faceSprite = DonorSprites.atlas(key.look().face());
        TextureAtlasSprite edgeSprite = DonorSprites.atlas(key.look().edge());
        int column = key.part().column.index();
        int row = key.part().row();
        int turns = VoxelShapeFactory.quarterTurns(key.facing());
        boolean front = key.face() == AttachmentFace.FRONT;
        double back = front ? DoorGeometry.FRONT : DoorGeometry.BACK - DoorTrimGeometry.PROUD;
        double out = front ? DoorGeometry.FRONT + DoorTrimGeometry.PROUD : DoorGeometry.BACK;
        List<BakedQuad> quads = new ArrayList<>();
        for (Surface surface : PixelSlab.cellFaces((u, v) -> DoorTrimGeometry.inCasing(key.design(), u, v),
                16 * column, 16 * row, back, out, "")) {
            boolean planar = surface.face().getAxis() == Direction.Axis.Z;
            TextureAtlasSprite sprite = planar ? faceSprite : edgeSprite;
            Box box = surface.backing().rotateY(turns);
            Direction side = turn(surface.face(), turns);
            BakedQuad quad = bake(box, side, sprite);
            if (planar && key.look().whole()) {
                boolean seenFromFront = surface.face() == Direction.SOUTH;
                quad = ModelRetexturing.retexture(quad, sprite, (x, y, z) -> wholeUv(key.design(), column, row, turns,
                        seenFromFront, x * 16.0, y * 16.0, z * 16.0));
            }
            quads.add(quad);
        }
        Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            culled.put(direction, List.of());
        }
        return new SimpleBakedModel(List.copyOf(quads), culled, true, true, false, faceSprite, ItemTransforms.NO_TRANSFORMS,
                ItemOverrides.EMPTY);
    }

    /** The whole-face UV of a cell-local, facing-turned point: turned back into the door's frame first. */
    private static float[] wholeUv(DoorDesign design, int column, int row, int turns, boolean front, double x, double y, double z) {
        for (int i = 0; i < turns; i++) {
            double turnedX = z; // undo one clockwise turn: (x, z) <- (z, 16 - x)
            z = Box.BLOCK - x;
            x = turnedX;
        }
        double[] uv = AssemblyFaceUv.point(x + 16 * column, y + 16 * row, front, design.width(), DoorDesign.HEIGHT,
                DoorGeometry.DIAMETER);
        return new float[] {(float) uv[0], (float) uv[1]};
    }

    private static BakedQuad bake(Box box, Direction side, TextureAtlasSprite sprite) {
        BlockElementFace element = new BlockElementFace(null, -1, "", new BlockFaceUV(FaceUv.tiled(box, side), 0));
        return BAKERY.bakeQuad(
                new Vector3f((float) box.minX(), (float) box.minY(), (float) box.minZ()),
                new Vector3f((float) box.maxX(), (float) box.maxY(), (float) box.maxZ()),
                element, sprite, side, BlockModelRotation.X0_Y0, null, true, NAME);
    }

    /** The same clockwise quarter turns {@link Box#rotateY} applies to geometry. */
    private static Direction turn(Direction side, int turns) {
        Direction out = side;
        if (out.getAxis().isHorizontal()) {
            for (int i = 0; i < turns; i++) {
                out = out.getClockWise();
            }
        }
        return out;
    }
}
