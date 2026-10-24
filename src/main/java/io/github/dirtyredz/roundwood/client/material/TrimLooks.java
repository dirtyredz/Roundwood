package io.github.dirtyredz.roundwood.client.material;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Function;

/**
 * What a trim look draws on its faces and edges (0.9.0: one resolver shared by the frame and door
 * rings, the window trims and the Universal Trim icon, which before kept their own copies). Only the
 * masonry face depends on the shape, so the caller supplies it:
 * <ul>
 *     <li>vanilla woods: their stripped log, tiled like a block;</li>
 *     <li>other woods: the donor plank sprite, tiled;</li>
 *     <li>paint: the painted door boards spread once over the face ({@code whole});</li>
 *     <li>masonry: the caller's thin-mortar face spread once over the face, and the granular edge;</li>
 *     <li>an unavailable material: oak.</li>
 * </ul>
 */
public final class TrimLooks {
    /** Whole-face mapping (else tiled), and the face and edge artwork. Also a model-cache key. */
    public record Look(boolean whole, ResourceLocation face, ResourceLocation edge) {
    }

    public static final Look OAK = new Look(false, BuiltinWoodStyles.OAK_TRIM_LOG, BuiltinWoodStyles.OAK_TRIM_LOG);

    private TrimLooks() {
    }

    /** Thin beams retain window plank/paint artwork; masonry uses the existing granular artwork. */
    public static Look beams(Appearance appearance) {
        ResourceLocation texture;
        if (!appearance.isWood() && appearance.paint().isEmpty()) texture = MasonryMaterials.byBlock(appearance.material()).map(MasonryStyles::edge).orElse(BuiltinWoodStyles.OAK_PLANKS);
        else texture = AppearanceLooks.planks(appearance);
        return new Look(false, texture, texture);
    }

    public static Look of(Appearance appearance, Function<MasonryMaterials.Material, ResourceLocation> masonryFace) {
        if (!appearance.isWood() && appearance.paint().isEmpty()) {
            return MasonryMaterials.byBlock(appearance.material())
                    .map(m -> new Look(true, masonryFace.apply(m), MasonryStyles.edge(m)))
                    .orElse(OAK);
        }
        return AppearanceLooks.resolve(appearance,
                dye -> { var panel = PaintStyles.doorPanel(dye); return new Look(true, panel, panel); },
                style -> Optional.of(new Look(false, style.trimLog(), style.trimLog())),
                sprite -> new Look(false, sprite.contents().name(), sprite.contents().name()), OAK);
    }
}
