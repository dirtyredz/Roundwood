package io.github.dirtyredz.roundwood.client.material.tint;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The colour that turns Roundwood's neutral door boards into an unfamiliar wood: the average of the
 * donor plank sprite's opaque texels (first frame), divided by the neutral artwork's own average so
 * that board times tint lands on the donor's average. Sampled once per sprite and cached (bounded,
 * cleared on reload); the arithmetic is plain so it is unit-tested without Minecraft.
 */
public final class DonorColors {
    /** Mean grey level of {@code door_panel_neutral.png} (0..255), measured from the generated texture. */
    public static final double NEUTRAL_MEAN = 179.5;
    /** Oak's average, the fallback when a sprite cannot be read. */
    public static final int FALLBACK = 0xA2824E;
    private static final int MAX_SPRITES = 512;
    private static final int OPAQUE = 128;
    private static final Map<ResourceLocation, Integer> CACHE = new ConcurrentHashMap<>();

    private DonorColors() {
    }

    /** The RGB tint for neutral boards standing in for {@code sprite}'s wood. */
    public static int tint(TextureAtlasSprite sprite) {
        ResourceLocation name = sprite.contents().name();
        if (CACHE.size() >= MAX_SPRITES && !CACHE.containsKey(name)) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(name, n -> tintFor(sample(sprite.contents()), NEUTRAL_MEAN));
    }

    public static void clear() {
        CACHE.clear();
    }

    /**
     * The average RGB of the opaque texels of {@code abgr} (NativeImage order: alpha, blue, green,
     * red from the high byte), or {@link #FALLBACK} when none is opaque.
     */
    public static int average(int[] abgr) {
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;
        for (int pixel : abgr) {
            if ((pixel >>> 24) < OPAQUE) {
                continue;
            }
            r += pixel & 0xFF;
            g += (pixel >> 8) & 0xFF;
            b += (pixel >> 16) & 0xFF;
            count++;
        }
        if (count == 0) {
            return FALLBACK;
        }
        return (int) Math.round((double) r / count) << 16 | (int) Math.round((double) g / count) << 8
                | (int) Math.round((double) b / count);
    }

    /** The tint that makes neutral boards of mean grey {@code neutralMean} average to {@code averageRgb}. */
    public static int tintFor(int averageRgb, double neutralMean) {
        int r = scale((averageRgb >> 16) & 0xFF, neutralMean);
        int g = scale((averageRgb >> 8) & 0xFF, neutralMean);
        int b = scale(averageRgb & 0xFF, neutralMean);
        return r << 16 | g << 8 | b;
    }

    private static int scale(int channel, double neutralMean) {
        return (int) Math.min(255, Math.round(channel * 255.0 / neutralMean));
    }

    private static int sample(SpriteContents contents) {
        NativeImage image = contents.getOriginalImage();
        if (image == null) {
            return FALLBACK;
        }
        int width = contents.width();
        int height = contents.height();
        int[] pixels = new int[width * height];
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = image.getPixelRGBA(x, y);
                }
            }
        } catch (RuntimeException closedOrOutOfBounds) {
            return FALLBACK;
        }
        return average(pixels);
    }
}
