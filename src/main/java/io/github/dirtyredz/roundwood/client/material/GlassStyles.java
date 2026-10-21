package io.github.dirtyredz.roundwood.client.material;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.Optional;

/**
 * Artwork bindings for window glass (0.8.0): the clear glazing every window model is generated with,
 * and one generated stained texture per dye ({@code window_glass_<dye>}, same diagonal marks as the
 * clear glass, darker), independent of the frame's paint colours.
 */
public final class GlassStyles {
    public static final ResourceLocation CLEAR = texture("window_glass_clear");

    private GlassStyles() {
    }

    /** The glazing for {@code color}; clear when empty. */
    public static ResourceLocation glass(Optional<DyeColor> color) {
        return color.map(dye -> texture("window_glass_" + dye.getName())).orElse(CLEAR);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "block/" + name);
    }
}
