package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import net.minecraft.resources.ResourceLocation;

import java.util.function.UnaryOperator;

/**
 * The bounded cache of restyled window item models. Keyed by the source item model it was made from,
 * the frame's wood sprite <em>and</em> the glass sprite (0.8.0): the window pieces restyle different
 * geometry, and two windows of the same wood with differently tinted glass must never share a model.
 * Sources are compared by identity (baked models are), never modified, and the cache is cleared with
 * every other appearance cache on a model bake.
 */
final class WindowItemModels<M> {
    private record Key(Object source, ResourceLocation wood, ResourceLocation glass) {
    }

    private final AppearanceModelCache<Key, M> cache;

    WindowItemModels(int capacity) {
        this.cache = new AppearanceModelCache<>(capacity);
    }

    /** The model {@code build} makes from {@code source} for {@code wood} and {@code glass}, built once per triple. */
    M get(M source, ResourceLocation wood, ResourceLocation glass, UnaryOperator<M> build) {
        return cache.get(new Key(source, wood, glass), key -> build.apply(source));
    }

    int size() {
        return cache.size();
    }

    void clear() {
        cache.clear();
    }
}
