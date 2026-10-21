package io.github.dirtyredz.roundwood.client.camouflage;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.door.camouflage.CamouflageGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Baked models of one cell's camouflage corner in a donor's materials: Roundwood's own corner
 * geometry (only its exposed surfaces, see {@link CamouflageGeometry#exposedFaces}), turned for the
 * door's facing, each face textured with the donor's sprite for that world side and cropped like a
 * full block at that spot. No face is culled against neighbours (the donor is not really there).
 * Built on demand, kept in a small least-recently-used cache, dropped on every model reload.
 */
public final class CamouflageModels {
    private static final int MAX_MODELS = 256;
    private static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "camouflage");
    private static final FaceBakery BAKERY = new FaceBakery();

    private record Key(BlockState donor, Direction facing, DoorLayout.Part part) {
    }

    private static final Map<Key, BakedModel> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, BakedModel> eldest) {
            return size() > MAX_MODELS;
        }
    };

    private CamouflageModels() {
    }

    /** The corner model for {@code part} of a door facing {@code facing}, in {@code donor}'s materials. */
    public static BakedModel quarter(BlockState donor, Direction facing, DoorLayout.Part part) {
        return CACHE.computeIfAbsent(new Key(donor, facing, part), CamouflageModels::build);
    }

    /** Resource reload: sprites and donor models may all have changed. */
    public static void clear() {
        CACHE.clear();
        CamouflageMaterials.clear();
    }

    private static BakedModel build(Key key) {
        int turns = VoxelShapeFactory.quarterTurns(key.facing());
        List<BakedQuad> quads = new ArrayList<>();
        for (CamouflageGeometry.Face face : CamouflageGeometry.exposedFaces(key.part().column.index(), key.part().row())) {
            Box box = face.box().rotateY(turns);
            Direction side = turn(direction(face.side()), turns);
            CamouflageMaterials.FaceMaterial material = CamouflageMaterials.face(key.donor(), side);
            BlockElementFace element = new BlockElementFace(null, material.tintIndex(), "", new BlockFaceUV(FaceUv.of(box, side), 0));
            quads.add(BAKERY.bakeQuad(
                    new Vector3f((float) box.minX(), (float) box.minY(), (float) box.minZ()),
                    new Vector3f((float) box.maxX(), (float) box.maxY(), (float) box.maxZ()),
                    element, material.sprite(), side, BlockModelRotation.X0_Y0, null, true, NAME));
        }
        Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            culled.put(side, List.of());
        }
        return new SimpleBakedModel(List.copyOf(quads), culled, true, true, false,
                CamouflageMaterials.face(key.donor(), Direction.NORTH).sprite(), ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY);
    }

    private static Direction direction(CamouflageGeometry.Side side) {
        return switch (side) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
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
