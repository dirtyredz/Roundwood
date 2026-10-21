package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.block.window.WindowAppearanceData;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.BuiltinWoodStyles;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.PaintStyles;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wraps every window blockstate model (canonical and 0.4.0 aliases) so the chunk-baked window shows
 * its block entity's appearance ({@link WindowAppearanceData}): only the wooden face's quads (the
 * oak planks of the generated core) change sprite; the glass and the arms never do, and the
 * translucent render type, connections and waterlogging are the model's own. Results are cached per
 * state, side, render type and wood sprite (bounded, cleared on reload; safe for chunk workers).
 * Oak, unpainted, is the baked model itself.
 */
public final class WindowAppearanceModel extends BakedModelWrapper<BakedModel> {
    private record Key(BlockState state, Direction side, RenderType renderType, ResourceLocation wood) {
    }

    private static final AppearanceModelCache<Key, List<BakedQuad>> QUADS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    private static final AppearanceModelCache<ResourceLocation, BakedModel> ITEMS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    public WindowAppearanceModel(BakedModel original) {
        super(original);
    }

    /** The wooden face's artwork for {@code appearance}: its plank sprite, its paint, or oak when unavailable. */
    public static ResourceLocation wood(Appearance appearance) {
        if (appearance.paint().isPresent()) {
            return PaintStyles.windowPanel(appearance.paint().get());
        }
        return DonorSprites.sprite(appearance.material()).map(sprite -> sprite.contents().name())
                .orElse(BuiltinWoodStyles.OAK_PLANKS);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                    @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, random, data, renderType);
        ResourceLocation wood = wood(appearance(state, data));
        if (state == null || wood.equals(BuiltinWoodStyles.OAK_PLANKS)) {
            return quads;
        }
        return QUADS.get(new Key(state, side, renderType, wood), key -> retexture(quads, DonorSprites.atlas(wood)));
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        WindowAppearanceData appearance = data.get(WindowAppearanceData.PROPERTY);
        return appearance == null ? super.getParticleIcon(data) : DonorSprites.atlas(wood(appearance.appearance()));
    }

    /** The window item model ({@code oakItem}) in {@code appearance}. */
    public static BakedModel item(BakedModel oakItem, Appearance appearance) {
        ResourceLocation wood = wood(appearance);
        if (wood.equals(BuiltinWoodStyles.OAK_PLANKS)) {
            return oakItem;
        }
        TextureAtlasSprite sprite = DonorSprites.atlas(wood);
        return ITEMS.get(wood, key -> ModelRetexturing.map(oakItem,
                quad -> isWood(quad) ? ModelRetexturing.retexture(quad, sprite, -1) : quad, s -> s));
    }

    private static Appearance appearance(@Nullable BlockState state, ModelData data) {
        WindowAppearanceData stored = data.get(WindowAppearanceData.PROPERTY);
        if (stored != null) {
            return stored.appearance();
        }
        return state == null ? Appearance.DEFAULT : AppearanceDefaults.of(state.getBlock());
    }

    private static List<BakedQuad> retexture(List<BakedQuad> quads, TextureAtlasSprite wood) {
        return quads.stream().map(quad -> isWood(quad) ? ModelRetexturing.retexture(quad, wood, -1) : quad).toList();
    }

    private static boolean isWood(BakedQuad quad) {
        return quad.getSprite().contents().name().equals(BuiltinWoodStyles.OAK_PLANKS);
    }
}
