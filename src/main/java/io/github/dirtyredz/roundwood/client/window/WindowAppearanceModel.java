package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.block.window.WindowAppearanceData;
import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
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
 * Wraps every window blockstate model (the ten windows and the 0.4.0 aliases) so the chunk-baked
 * window shows its block entity's looks ({@link WindowAppearanceData}): the wooden face and beams
 * (the oak planks of the generated model) take the pane's wood or paint, and a fitted trim's marker
 * quads take the trim's look ({@link WindowTrimModels}, always, since its markers are never final
 * art). Glass and junctions never change, and the translucent render type, connections and
 * waterlogging are the model's own. Results are cached per state, side, render type, wood sprite and
 * trim look (bounded, cleared on reload; safe for chunk workers). Oak with no trim is the baked model
 * itself.
 */
public final class WindowAppearanceModel extends BakedModelWrapper<BakedModel> {
    private record Key(BlockState state, Direction side, RenderType renderType, ResourceLocation wood,
                       @Nullable WindowTrimModels.Look trim) {
    }

    private static final AppearanceModelCache<Key, List<BakedQuad>> QUADS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    private static final WindowItemModels<BakedModel> ITEMS = new WindowItemModels<>(AppearanceModelCache.MODELS);

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
        if (state == null || !(state.getBlock() instanceof WindowPaneBlock block)) {
            return quads;
        }
        ResourceLocation wood = wood(appearance(state, data));
        WindowTrimModels.Look trim = WindowPaneBlock.trimmed(state)
                ? WindowTrimModels.look(trimAppearance(data), block.profile()) : null;
        if (trim == null && wood.equals(BuiltinWoodStyles.OAK_PLANKS)) {
            return quads;
        }
        return QUADS.get(new Key(state, side, renderType, wood, trim), key -> {
            TextureAtlasSprite woodSprite = DonorSprites.atlas(wood);
            return quads.stream().map(quad -> {
                if (isWood(quad)) {
                    return wood.equals(BuiltinWoodStyles.OAK_PLANKS) ? quad : ModelRetexturing.retexture(quad, woodSprite, -1);
                }
                if (trim != null && WindowTrimModels.isTrim(quad)) {
                    return WindowTrimModels.paint(quad, trim, block.profile(), block.pose(state));
                }
                return quad;
            }).toList();
        });
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        WindowAppearanceData appearance = data.get(WindowAppearanceData.PROPERTY);
        return appearance == null ? super.getParticleIcon(data) : DonorSprites.atlas(wood(appearance.appearance()));
    }

    /** A window item model ({@code oakItem}: any window piece's) in {@code appearance}; window items never carry a trim. */
    public static BakedModel item(BakedModel oakItem, Appearance appearance) {
        ResourceLocation wood = wood(appearance);
        if (wood.equals(BuiltinWoodStyles.OAK_PLANKS)) {
            return oakItem;
        }
        TextureAtlasSprite sprite = DonorSprites.atlas(wood);
        return ITEMS.get(oakItem, wood, source -> ModelRetexturing.map(source,
                quad -> isWood(quad) ? ModelRetexturing.retexture(quad, sprite, -1) : quad, s -> s));
    }

    private static Appearance appearance(BlockState state, ModelData data) {
        WindowAppearanceData stored = data.get(WindowAppearanceData.PROPERTY);
        return stored != null ? stored.appearance() : AppearanceDefaults.of(state.getBlock());
    }

    /** The trim's look, oak until the entity's data arrives. */
    private static Appearance trimAppearance(ModelData data) {
        WindowAppearanceData stored = data.get(WindowAppearanceData.PROPERTY);
        return stored == null ? Appearance.DEFAULT : stored.trim().orElse(Appearance.DEFAULT);
    }

    private static boolean isWood(BakedQuad quad) {
        return quad.getSprite().contents().name().equals(BuiltinWoodStyles.OAK_PLANKS);
    }
}
