package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.client.door.leaf.DoorSurfaceModels;
import io.github.dirtyredz.roundwood.client.door.leaf.DoorLeafModels;
import io.github.dirtyredz.roundwood.client.door.hardware.DoorHardwareModels;
import io.github.dirtyredz.roundwood.client.material.AppearanceLooks;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.world.item.Item;
import java.util.stream.Stream;
import java.util.List;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.BuiltinWoodStyles;
import io.github.dirtyredz.roundwood.client.material.tint.DonorColors;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.tint.ModelRetexturing;
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
 * quads use the panel look; typed hardware groups use independent untinted finishes. Looks:
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
    public record Look(ResourceLocation panel, int tint, boolean tiled) {
        public Look(ResourceLocation panel, int tint) { this(panel,tint,false); }
        public boolean isOakTemplate() {
            return !tiled && panel.equals(BuiltinWoodStyles.OAK_DOOR_PANEL) && tint < 0;
        }
    }

    /** Design-keyed (0.11.0): four door shapes share looks, so a look alone would hand one door the other's model. */
    private record Key(Look look, DoorDesign design, String variant, io.github.dirtyredz.roundwood.block.door.hardware.DoorHardware hardware) {
    }

    private static final AppearanceModelCache<Key, BakedModel> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private DoorAppearanceModels() {
    }

    /** Door panel tint registration; brass remains untinted. */
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        List<Item> doors = Stream.concat(ModItems.all().stream(), LegacyContent.items().stream())
                .filter(item -> item instanceof AppearanceDefaults family && family.appearanceFamily() == AppearanceFamily.DOOR)
                .toList();
        event.register((stack, tintIndex) -> {
            if (tintIndex != PANEL_TINT) {
                return -1;
            }
            int tint = look(AppearanceStacks.read(stack)).tint();
            return tint < 0 ? -1 : tint;
        }, doors.toArray(Item[]::new));
    }

    public static Look look(Appearance appearance) {
        if (appearance.kind() == Appearance.Kind.BLOCK_SURFACE) return new Look(AppearanceLooks.planks(appearance),-1,true);
        return AppearanceLooks.resolve(appearance,
                dye -> new Look(PaintStyles.doorPanel(dye), -1),
                style -> Optional.of(new Look(style.doorPanel(), -1)),
                sprite -> new Look(BuiltinWoodStyles.NEUTRAL_DOOR_PANEL, DonorColors.tint(sprite)),
                new Look(BuiltinWoodStyles.OAK_DOOR_PANEL, -1));
    }

    /** The baked leaf of {@code design} for {@code hinge} in {@code look}. */
    public static BakedModel leaf(Look look, DoorDesign design, DoorHingeSide hinge, io.github.dirtyredz.roundwood.block.door.hardware.DoorHardware hardware) {
        BakedModel oak = Minecraft.getInstance().getModelManager().getModel(DoorLeafModels.location(design, hinge));
        return MODELS.get(new Key(look, design, hinge.getSerializedName(),hardware), key -> retexture(oak, look, design, hinge, false, hardware));
    }

    /** {@code oakItem} ({@code design}'s item model) in {@code appearance}. */
    public static BakedModel item(DoorDesign design, BakedModel oakItem, net.minecraft.world.item.ItemStack stack) {
        Look look = look(AppearanceStacks.read(stack));
        var hardware = io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStacks.read(stack);
        return MODELS.get(new Key(look, design, "item",hardware), key -> retexture(oakItem, look, design, DoorHingeSide.LEFT, true, hardware));
    }

    private static BakedModel retexture(BakedModel oak, Look look, DoorDesign design, DoorHingeSide hinge, boolean item, io.github.dirtyredz.roundwood.block.door.hardware.DoorHardware hardware) {
        TextureAtlasSprite panel = DonorSprites.atlas(look.panel());
        if (look.tiled()) {
            var surface = DoorSurfaceModels.replacePanel(oak,design,hinge,item,panel,DoorAppearanceModels::isPanel);
            return ModelRetexturing.map(surface, quad -> DoorHardwareModels.retexture(quad,hardware), side -> side);
        }
        int tint = look.tint() >= 0 ? PANEL_TINT : -1;
        return ModelRetexturing.map(oak, quad -> isPanel(quad) ? ModelRetexturing.retexture(quad, panel, tint) : DoorHardwareModels.retexture(quad,hardware), side -> side);
    }

    private static boolean isPanel(BakedQuad quad) {
        return quad.getSprite().contents().name().equals(BuiltinWoodStyles.OAK_DOOR_PANEL);
    }
}
