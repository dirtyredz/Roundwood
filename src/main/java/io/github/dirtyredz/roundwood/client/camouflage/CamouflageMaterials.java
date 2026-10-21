package io.github.dirtyredz.roundwood.client.camouflage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The texture and tint a donor block shows on each side, taken from its own baked model: the first
 * quad culled to that side, else the first unculled quad facing that way, else the model's particle
 * sprite untinted. One representative quad per side: ordinary textured cubes look right; connected
 * textures, layered overlays and dynamic models are not reproduced. Cleared on every model reload.
 */
public final class CamouflageMaterials {
    /** One face's look: the sprite and the model's tint index ({@code -1} for none). */
    public record FaceMaterial(TextureAtlasSprite sprite, int tintIndex) {
    }

    /** Far more than any world uses; reaching it just starts the cache over. */
    private static final int MAX_DONORS = 512;
    private static final Map<BlockState, Map<Direction, FaceMaterial>> CACHE = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private CamouflageMaterials() {
    }

    public static FaceMaterial face(BlockState donor, Direction side) {
        if (CACHE.size() >= MAX_DONORS && !CACHE.containsKey(donor)) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(donor, CamouflageMaterials::resolve).get(side);
    }

    static void clear() {
        CACHE.clear();
    }

    private static Map<Direction, FaceMaterial> resolve(BlockState donor) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(donor);
        Map<Direction, FaceMaterial> faces = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            faces.put(side, pick(model, donor, side));
        }
        return faces;
    }

    private static FaceMaterial pick(BakedModel model, BlockState donor, Direction side) {
        RANDOM.setSeed(42L);
        List<BakedQuad> culled = model.getQuads(donor, side, RANDOM, ModelData.EMPTY, null);
        if (!culled.isEmpty()) {
            return of(culled.get(0));
        }
        RANDOM.setSeed(42L);
        for (BakedQuad quad : model.getQuads(donor, null, RANDOM, ModelData.EMPTY, null)) {
            if (quad.getDirection() == side) {
                return of(quad);
            }
        }
        return new FaceMaterial(model.getParticleIcon(ModelData.EMPTY), -1);
    }

    private static FaceMaterial of(BakedQuad quad) {
        return new FaceMaterial(quad.getSprite(), quad.isTinted() ? quad.getTintIndex() : -1);
    }
}
