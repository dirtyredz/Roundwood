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

    public static Look of(Appearance appearance, Function<MasonryMaterials.Material, ResourceLocation> masonryFace) {
        if (appearance.paint().isPresent()) {
            ResourceLocation panel = PaintStyles.doorPanel(appearance.paint().get());
            return new Look(true, panel, panel);
        }
        if (!appearance.isWood()) {
            return MasonryMaterials.byBlock(appearance.material())
                    .map(m -> new Look(true, masonryFace.apply(m), MasonryStyles.edge(m)))
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
}
