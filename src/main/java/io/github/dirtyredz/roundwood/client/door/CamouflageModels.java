package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageCoverage;
import io.github.dirtyredz.roundwood.client.render.GeneralQuadModels;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageGeometry;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Baked models of one cell's camouflage corner in a donor's materials: Roundwood's own corner
 * geometry (only its exposed surfaces, see {@link CamouflageGeometry#exposedFaces}), turned for the
 * door's facing, each face textured with the donor's sprite for that world side and cropped like a
 * full block at that spot. No face is culled against neighbours (the donor is not really there).
 * Built on demand, kept in a small least-recently-used cache, dropped on every model reload.
 */
public final class CamouflageModels {
    private static final int MAX_MODELS = 256;
    private static final FaceBakery BAKERY = new FaceBakery();

    private record Key(BlockState donor, DoorDesign design, Direction facing, DoorLayout.Part part, CamouflageCoverage coverage) {
    }

    private static final AppearanceModelCache<Key, BakedModel> CACHE = new AppearanceModelCache<>(MAX_MODELS);

    private CamouflageModels() {
    }

    /** The corner model for {@code part} of a door facing {@code facing}, in {@code donor}'s materials. */
    public static BakedModel quarter(BlockState donor, DoorDesign design, Direction facing, DoorLayout.Part part, CamouflageCoverage coverage) {
        return CACHE.get(new Key(donor, design, facing, part, coverage), CamouflageModels::build);
    }

    private static BakedModel build(Key key) {
        var quads = bake(key.design(), key.facing(), key.part(), key.coverage(), side -> DonorSprites.face(key.donor(), side));
        return GeneralQuadModels.world(quads, DonorSprites.face(key.donor(), Direction.NORTH).sprite());
    }

    /** Production pre-lighting baker with an explicit donor-material resolver for headless geometry tests. */
    public static List<BakedQuad> bake(DoorDesign design, Direction facing, DoorLayout.Part part, CamouflageCoverage coverage,
                                      java.util.function.Function<Direction, DonorSprites.FaceMaterial> materials) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        List<BakedQuad> quads = new ArrayList<>();
        for (CamouflageGeometry.Face face : CamouflageGeometry.exposedFaces(design, part.columnIndex(), part.row(), coverage)) {
            Box box = face.box().rotateY(turns);
            Direction side = VoxelShapeFactory.turn(direction(face.side()), turns);
            DonorSprites.FaceMaterial material = materials.apply(side);
            BlockElementFace element = new BlockElementFace(null, material.tintIndex(), "", new BlockFaceUV(FaceUv.of(box, side), 0));
            quads.add(BAKERY.bakeQuad(
                    new Vector3f((float) box.minX(), (float) box.minY(), (float) box.minZ()),
                    new Vector3f((float) box.maxX(), (float) box.maxY(), (float) box.maxZ()),
                    element, material.sprite(), side, BlockModelRotation.X0_Y0, null, true));
        }
        return List.copyOf(quads);
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

}
