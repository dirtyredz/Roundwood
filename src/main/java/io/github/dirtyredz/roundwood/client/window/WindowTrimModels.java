package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimUv;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.BuiltinWoodStyles;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.PaintStyles;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * The window trim's artwork (0.7.0). The generated window models draw the trim with two marker
 * sprites ({@link #FACE_MARKER} for its front and back, {@link #EDGE_MARKER} for its sides), so after
 * baking its quads are never confused with the pane's oak wood; they are always replaced here:
 * <ul>
 *     <li>vanilla woods: their stripped log, tiled like a block (the round trim's wood);</li>
 *     <li>other woods: the donor plank sprite, tiled;</li>
 *     <li>paint: the painted door boards spread once over the face ({@link WindowTrimUv});</li>
 *     <li>masonry: the thin-mortar face for the profile spread once over the round window or the whole
 *     2 x 2 arched window, and the granular edge;</li>
 *     <li>an unavailable material: oak.</li>
 * </ul>
 * Glass and pane wood are never touched. The window trim item resolves its stack the same way.
 */
public final class WindowTrimModels {
    public static final ResourceLocation FACE_MARKER = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "block/window_trim_face");
    public static final ResourceLocation EDGE_MARKER = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "block/window_trim_edge");

    /** What a trim look draws: whole-face mapping or tiled, and the face and edge artwork. */
    public record Look(boolean whole, ResourceLocation face, ResourceLocation edge) {
    }

    private static final Look OAK = new Look(false, BuiltinWoodStyles.OAK_TRIM_LOG, BuiltinWoodStyles.OAK_TRIM_LOG);
    private static final AppearanceModelCache<Look, BakedModel> ITEMS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private WindowTrimModels() {
    }

    public static Look look(Appearance appearance, WindowProfile profile) {
        if (appearance.paint().isPresent()) {
            ResourceLocation panel = PaintStyles.doorPanel(appearance.paint().get());
            return new Look(true, panel, panel);
        }
        if (!appearance.isWood()) {
            return MasonryMaterials.byBlock(appearance.material())
                    .map(m -> new Look(true, MasonryStyles.windowFace(m, profile), MasonryStyles.edge(m)))
                    .orElse(OAK);
        }
        Optional<BuiltinWoodStyles.Style> builtin = BuiltinWoodStyles.of(appearance.material());
        if (builtin.isPresent()) {
            return new Look(false, builtin.get().trimLog(), builtin.get().trimLog());
        }
        return DonorSprites.sprite(appearance.material())
                .map(sprite -> new Look(false, sprite.contents().name(), sprite.contents().name()))
                .orElse(OAK);
    }

    /** True for a quad drawn with one of the trim's marker sprites. */
    public static boolean isTrim(BakedQuad quad) {
        ResourceLocation name = quad.getSprite().contents().name();
        return name.equals(FACE_MARKER) || name.equals(EDGE_MARKER);
    }

    /** A trim quad of a window posed {@code pose} with {@code profile}, in {@code look}. */
    public static BakedQuad paint(BakedQuad quad, Look look, WindowProfile profile, WindowPose pose) {
        boolean face = quad.getSprite().contents().name().equals(FACE_MARKER);
        TextureAtlasSprite sprite = DonorSprites.atlas(face ? look.face() : look.edge());
        if (!face || !look.whole()) {
            return ModelRetexturing.retexture(quad, sprite, -1);
        }
        boolean front = quad.getDirection() == pose.front();
        return ModelRetexturing.retexture(quad, sprite,
                (x, y, z) -> WindowTrimUv.whole(profile, pose, x * 16.0, y * 16.0, z * 16.0, front));
    }

    /**
     * The window trim item in {@code appearance}: its ring model (oak stripped log, whole-face UVs over
     * 16 pixels, which for wood equal the tiled ones) with the front and back retextured to the face
     * artwork and the sides to the edge artwork.
     */
    public static BakedModel item(BakedModel oakItem, Appearance appearance) {
        Look look = look(appearance, WindowProfile.ROUND);
        if (look.equals(OAK)) {
            return oakItem;
        }
        return ITEMS.get(look, key -> {
            TextureAtlasSprite face = DonorSprites.atlas(look.face());
            TextureAtlasSprite edge = DonorSprites.atlas(look.edge());
            return ModelRetexturing.map(oakItem,
                    quad -> ModelRetexturing.retexture(quad, quad.getDirection().getAxis() == Direction.Axis.Z ? face : edge, -1),
                    side -> side);
        });
    }
}
