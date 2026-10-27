package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.client.material.AppearanceLooks;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.world.level.block.Block;
import java.util.Map;
import java.util.ArrayList;
import io.github.dirtyredz.roundwood.client.window.trim.WindowTrimRenderer;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.migration.CrossWindowCompatibility;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.pane.WindowAppearanceData;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.BuiltinWoodStyles;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.GlassStyles;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
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
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState;
import io.github.dirtyredz.roundwood.block.window.trim.WindowBeamNbt;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.migration.item.RetiredCrossWindowItem;

/**
 * Wraps canonical and compatibility window models. Plain panes retain generated geometry and
 * frame/glass retexturing. Fitted panes compose independent beam geometry and use immutable
 * neighbour coverage; face trims and sills join the solid chunk layer unless legacy rendering is enabled. All models are cached
 * by geometry and materials and invalidated by the existing appearance-cache reload lifecycle.
 */
public final class WindowAppearanceModel extends BakedModelWrapper<BakedModel> {
    /** Wrap each canonical, alias and retired window blockstate model once at bake time. */
    public static void wrapBlocks(Map<ResourceLocation, BakedModel> models) {
        List<Block> windows = new ArrayList<>(LegacyContent.windows());
        windows.addAll(ModBlocks.windows());
        windows.addAll(CrossWindowCompatibility.blocks());
        for (Block window : windows) {
            for (BlockState state : window.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(location);
                if (model != null) {
                    models.put(location, new WindowAppearanceModel(model));
                }
            }
        }
    }

    private record Key(BlockState state, Direction side, RenderType renderType, ResourceLocation wood, ResourceLocation glass) {
    }

    private static final AppearanceModelCache<Key, List<BakedQuad>> QUADS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    record ItemKey(BakedModel source, ResourceLocation wood, ResourceLocation glass, ResourceLocation beams) {}

    private static final AppearanceModelCache<ItemKey, BakedModel> ITEMS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    public WindowAppearanceModel(BakedModel original) {
        super(original);
    }

    private record WorldKey(BlockState state, Direction side, RenderType layer, WindowAppearanceData snapshot, boolean legacy) {}
    private static final AppearanceModelCache<WorldKey, List<BakedQuad>> WORLD = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    @Override public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        var base = super.getRenderTypes(state, random, data);
        var snapshot = data.get(WindowAppearanceData.PROPERTY);
        if (io.github.dirtyredz.roundwood.client.render.StaticRenderingConfig.legacy() || snapshot == null
                || !snapshot.trims().any() && !snapshot.sills().any()) return base;
        return net.minecraftforge.client.ChunkRenderTypeSet.union(base, net.minecraftforge.client.ChunkRenderTypeSet.of(RenderType.solid()));
    }

    /** The frame's artwork for {@code appearance}: its plank sprite, its paint, or oak when unavailable. */
    public static ResourceLocation wood(Appearance appearance) {
        return AppearanceLooks.planks(appearance);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                    @Nullable RenderType renderType) {
        boolean legacy = io.github.dirtyredz.roundwood.client.render.StaticRenderingConfig.legacy();
        var snapshot = data.get(WindowAppearanceData.PROPERTY);
        if (state == null) return paneQuads(state, side, random, data, renderType);
        return WORLD.get(new WorldKey(state, side, renderType, snapshot, legacy), key -> {
            var out = new ArrayList<BakedQuad>();
            if (renderType == null || super.getRenderTypes(state, random, data).contains(renderType))
                out.addAll(paneQuads(state, side, random, data, renderType));
            if (!legacy && side == null && (renderType == null || renderType == RenderType.solid()))
                out.addAll(io.github.dirtyredz.roundwood.client.render.StaticWorldModel.collect(
                        io.github.dirtyredz.roundwood.client.window.trim.WindowStaticModels.models(state, snapshot), state, random, renderType));
            return List.copyOf(out);
        });
    }

    private List<BakedQuad> paneQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                    @Nullable RenderType renderType) {
        WindowAppearanceData snapshot = data.get(WindowAppearanceData.PROPERTY);
        if (state != null && state.getBlock() instanceof WindowPaneBlock pane
                && (snapshot != null && snapshot.beams().isPresent() || pane.design().pattern() == WindowPattern.CROSS)) {
            if (side != null) return List.of();
            Appearance frame = snapshot == null ? AppearanceDefaults.of(pane) : snapshot.appearance();
            Appearance beams = snapshot == null ? frame : snapshot.beams().orElse(frame);
            var design = new WindowDesign(pane.profile(), WindowPattern.CROSS);
            var geometry = new WindowPaneState.Snapshot(design, pane.pose(state));
            return WindowBeamModels.quads(geometry, wood(frame), snapshot == null ? GlassStyles.CLEAR : GlassStyles.glass(snapshot.glass()),
                    beams, snapshot == null ? java.util.Map.of() : snapshot.neighbours());
        }
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
        boolean legacy = stack.getItem() instanceof RetiredCrossWindowItem;
        Appearance appearance = legacy ? CrossWindowCompatibility.appearance(stack) : AppearanceStacks.read(stack);
        ResourceLocation wood = wood(appearance);
        ResourceLocation glass = GlassStyles.glass(legacy ? CrossWindowCompatibility.glass(stack) : WindowGlassStacks.read(stack));
        if (stack.getItem() instanceof RetiredCrossWindowItem retired) {
            var pane = (WindowPaneBlock) retired.getBlock();
            var state = pane.defaultBlockState();
            var geometry = new WindowPaneState.Snapshot(retired.design(), pane.pose(state));
            var fitted = WindowBeamNbt.read(stack.getTagElement("BlockEntityTag")).trims()
                    .get(AttachmentFace.FRONT).orElse(appearance);
            return ITEMS.get(new ItemKey(oakItem, wood, glass, TrimLooks.beams(fitted).face()), key ->
                    WindowBeamModels.item(key.source(), WindowBeamModels.quads(geometry, wood, glass, fitted, java.util.Map.of()), DonorSprites.atlas(wood)));
        }
        if (wood.equals(BuiltinWoodStyles.OAK_PLANKS) && glass.equals(GlassStyles.CLEAR)) {
            return oakItem;
        }
        return ITEMS.get(new ItemKey(oakItem, wood, glass, null), key -> ModelRetexturing.map(key.source(),
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
