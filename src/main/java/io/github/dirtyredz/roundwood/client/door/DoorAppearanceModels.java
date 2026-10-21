package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.BuiltinWoodStyles;
import io.github.dirtyredz.roundwood.client.material.DonorColors;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.PaintStyles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.Optional;

/**
 * Door leaves (and door items) in an appearance. The oak leaf model is the template; only its panel
 * quads change sprite, the brass hardware never does (it is never tinted either). Looks:
 * <ul>
 *     <li>vanilla woods: their own 0.4.0 full-height board panels, untinted (the plain oak door is the
 *     baked leaf itself, so its texture and lighting are exactly 0.4.0's);</li>
 *     <li>paint: the painted board panel, opaque and untinted;</li>
 *     <li>other woods: the neutral board panel, tinted with the donor plank's average colour (tint
 *     index 0, so only the panel takes the colour);</li>
 *     <li>an unavailable material: oak.</li>
 * </ul>
 */
public final class DoorAppearanceModels {
    /** Tint index of a tinted panel quad; brass quads stay untinted. */
    public static final int PANEL_TINT = 0;

    /** The panel artwork and its tint ({@code -1}: none). */
    public record Look(ResourceLocation panel, int tint) {
        public boolean isOakTemplate() {
            return panel.equals(BuiltinWoodStyles.OAK_DOOR_PANEL) && tint < 0;
        }
    }

    private record Key(Look look, String variant) {
    }

    private static final AppearanceModelCache<Key, BakedModel> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private DoorAppearanceModels() {
    }

    public static Look look(Appearance appearance) {
        if (appearance.paint().isPresent()) {
            return new Look(PaintStyles.doorPanel(appearance.paint().get()), -1);
        }
        Optional<BuiltinWoodStyles.Style> builtin = BuiltinWoodStyles.of(appearance.material());
        if (builtin.isPresent()) {
            return new Look(builtin.get().doorPanel(), -1);
        }
        Optional<TextureAtlasSprite> donor = DonorSprites.sprite(appearance.material());
        return donor.map(sprite -> new Look(BuiltinWoodStyles.NEUTRAL_DOOR_PANEL, DonorColors.tint(sprite)))
                .orElse(new Look(BuiltinWoodStyles.OAK_DOOR_PANEL, -1));
    }

    /** The baked leaf for {@code hinge} in {@code look}. */
    public static BakedModel leaf(Look look, DoorHingeSide hinge) {
        BakedModel oak = Minecraft.getInstance().getModelManager().getModel(DoorLeafModels.location(hinge));
        return look.isOakTemplate() ? oak : MODELS.get(new Key(look, hinge.getSerializedName()), key -> retexture(oak, look));
    }

    /** {@code oakItem} (the door's item model) in {@code appearance}. */
    public static BakedModel item(BakedModel oakItem, Appearance appearance) {
        Look look = look(appearance);
        return look.isOakTemplate() ? oakItem : MODELS.get(new Key(look, "item"), key -> retexture(oakItem, look));
    }

    private static BakedModel retexture(BakedModel oak, Look look) {
        TextureAtlasSprite panel = DonorSprites.atlas(look.panel());
        int tint = look.tint() >= 0 ? PANEL_TINT : -1;
        return ModelRetexturing.map(oak, quad -> isPanel(quad) ? ModelRetexturing.retexture(quad, panel, tint) : quad, side -> side);
    }

    private static boolean isPanel(BakedQuad quad) {
        return quad.getSprite().contents().name().equals(BuiltinWoodStyles.OAK_DOOR_PANEL);
    }
}
