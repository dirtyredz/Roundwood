package io.github.dirtyredz.roundwood.client.material;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;
import java.util.function.Function;

/** Shared paint, built-in artwork, donor sprite, fallback precedence. Artwork stays with the caller. */
public final class AppearanceLooks {
    private AppearanceLooks() {}

    /** Windows and thin beams preserve the donor's actual plank artwork, including built-in woods. */
    public static ResourceLocation planks(Appearance appearance) {
        return resolve(appearance, PaintStyles::windowPanel, style -> Optional.empty(),
                sprite -> sprite.contents().name(), BuiltinWoodStyles.OAK_PLANKS);
    }

    /** An empty built-in mapping deliberately falls through to the donor's real resource-pack sprite. */
    public static <T> T resolve(Appearance appearance, Function<DyeColor, T> paint,
                               Function<BuiltinWoodStyles.Style, Optional<T>> builtin,
                               Function<TextureAtlasSprite, T> donor, T fallback) {
        if (appearance.paint().isPresent()) return paint.apply(appearance.paint().get());
        Optional<T> artwork = BuiltinWoodStyles.of(appearance.material()).flatMap(builtin);
        return artwork.orElseGet(() -> DonorSprites.sprite(appearance.material()).map(donor).orElse(fallback));
    }
}
