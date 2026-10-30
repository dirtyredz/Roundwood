package io.github.dirtyredz.roundwood.client.material;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The texture and tint a donor block shows on each side, taken from its own baked model: the first
 * quad culled to that side, else the first unculled quad facing that way, else the model's particle
 * sprite untinted. One representative quad per side: ordinary textured cubes look right; connected
 * textures, layered overlays and dynamic models are not reproduced. Shared by camouflage (any full
 * cube) and wooden appearances (planks); results are immutable, resolution uses its own fixed-seed
 * randomness, and the cache is safe for chunk-render workers (windows). Cleared on every model reload.
 */
public final class DonorSprites {
    /** One face's look: the sprite and the model's tint index ({@code -1} for none). */
    public record FaceMaterial(TextureAtlasSprite sprite, int tintIndex) {
    }

    /** Far more than any world uses; reaching it just starts the cache over. */
    private static final int MAX_DONORS = 512;
    private static final long SEED = 42L;
    private static final Map<BlockState, Map<Direction, FaceMaterial>> CACHE = new ConcurrentHashMap<>();

    private DonorSprites() {
    }

    public static FaceMaterial face(BlockState donor, Direction side) {
        if (CACHE.size() >= MAX_DONORS && !CACHE.containsKey(donor)) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(donor, DonorSprites::resolve).get(side);
    }

    /** A material block's representative sprite (its north face), or empty when the block is not registered. */
    public static Optional<TextureAtlasSprite> sprite(ResourceLocation block) {
        if (!BuiltInRegistries.BLOCK.containsKey(block)) {
            return Optional.empty();
        }
        return Optional.of(face(BuiltInRegistries.BLOCK.get(block).defaultBlockState(), Direction.NORTH).sprite());
    }

    /** A texture on the block atlas (missing-texture sprite if absent). */
    public static TextureAtlasSprite atlas(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static Map<Direction, FaceMaterial> resolve(BlockState donor) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(donor);
        Map<Direction, FaceMaterial> faces = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            faces.put(side, pick(model, donor, side));
        }
        return Map.copyOf(faces);
    }

    private static FaceMaterial pick(BakedModel model, BlockState donor, Direction side) {
        List<BakedQuad> culled = model.getQuads(donor, side, RandomSource.create(SEED), ModelData.EMPTY, null);
        if (!culled.isEmpty()) {
            return of(culled.get(0));
        }
        for (BakedQuad quad : model.getQuads(donor, null, RandomSource.create(SEED), ModelData.EMPTY, null)) {
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
