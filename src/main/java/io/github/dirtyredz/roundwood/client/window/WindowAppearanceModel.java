package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.WindowAppearanceData;
import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.BuiltinWoodStyles;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.GlassStyles;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.PaintStyles;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Wraps every window blockstate model (the ten windows and the 0.4.0 aliases) so the chunk-baked
 * window shows its block entity's looks ({@link WindowAppearanceData}): the frame and beams (the oak
 * planks of the generated model) take the frame's wood or paint, and the glazing (the clear glass)
 * takes the glass colour (0.8.0). Junction glass is glass too, so one colour covers the whole
 * window. Face trims are not part of this model: {@link WindowTrimRenderer} draws them. The
 * translucent render type, connections and waterlogging are the model's own. Results are cached per
 * state, side, render type, wood sprite and glass sprite (bounded, cleared on reload; safe for chunk
 * workers). Oak with clear glass is the baked model itself.
 */
public final class WindowAppearanceModel extends BakedModelWrapper<BakedModel> {
    private record Key(BlockState state, Direction side, RenderType renderType, ResourceLocation wood, ResourceLocation glass) {
    }

    private static final AppearanceModelCache<Key, List<BakedQuad>> QUADS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    private static final WindowItemModels<BakedModel> ITEMS = new WindowItemModels<>(AppearanceModelCache.MODELS);

    public WindowAppearanceModel(BakedModel original) {
        super(original);
    }

    /** The frame's artwork for {@code appearance}: its plank sprite, its paint, or oak when unavailable. */
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
        if (state == null || !(state.getBlock() instanceof WindowPaneBlock)) {
            return quads;
        }
        WindowAppearanceData stored = data.get(WindowAppearanceData.PROPERTY);
        ResourceLocation wood = wood(stored != null ? stored.appearance() : AppearanceDefaults.of(state.getBlock()));
        ResourceLocation glass = stored != null ? GlassStyles.glass(stored.glass()) : GlassStyles.CLEAR;
        if (wood.equals(BuiltinWoodStyles.OAK_PLANKS) && glass.equals(GlassStyles.CLEAR)) {
            return quads;
        }
        return QUADS.get(new Key(state, side, renderType, wood, glass), key -> restyle(quads, wood, glass));
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        WindowAppearanceData appearance = data.get(WindowAppearanceData.PROPERTY);
        return appearance == null ? super.getParticleIcon(data) : DonorSprites.atlas(wood(appearance.appearance()));
    }

    /** A window item model ({@code oakItem}: that window piece's) for {@code stack}'s frame look and glass. */
    public static BakedModel item(BakedModel oakItem, ItemStack stack) {
        ResourceLocation wood = wood(AppearanceStacks.read(stack));
        ResourceLocation glass = GlassStyles.glass(WindowGlassStacks.read(stack));
        if (wood.equals(BuiltinWoodStyles.OAK_PLANKS) && glass.equals(GlassStyles.CLEAR)) {
            return oakItem;
        }
        return ITEMS.get(oakItem, wood, glass, source -> ModelRetexturing.map(source,
                restyler(wood, glass), s -> s));
    }

    private static List<BakedQuad> restyle(List<BakedQuad> quads, ResourceLocation wood, ResourceLocation glass) {
        UnaryOperator<BakedQuad> restyler = restyler(wood, glass);
        return quads.stream().map(restyler).toList();
    }

    /** Swaps the oak frame quads to {@code wood} and the clear glass quads to {@code glass}; nothing else changes. */
    private static UnaryOperator<BakedQuad> restyler(ResourceLocation wood, ResourceLocation glass) {
        TextureAtlasSprite woodSprite = wood.equals(BuiltinWoodStyles.OAK_PLANKS) ? null : DonorSprites.atlas(wood);
        TextureAtlasSprite glassSprite = glass.equals(GlassStyles.CLEAR) ? null : DonorSprites.atlas(glass);
        return quad -> {
            ResourceLocation name = quad.getSprite().contents().name();
            if (woodSprite != null && name.equals(BuiltinWoodStyles.OAK_PLANKS)) {
                return ModelRetexturing.retexture(quad, woodSprite, -1);
            }
            if (glassSprite != null && name.equals(GlassStyles.CLEAR)) {
                return ModelRetexturing.retexture(quad, glassSprite, -1);
            }
            return quad;
        };
    }
}
