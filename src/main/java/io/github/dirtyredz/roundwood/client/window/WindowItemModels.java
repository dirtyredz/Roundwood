package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import net.minecraft.resources.ResourceLocation;

import java.util.function.UnaryOperator;

/**
 * The bounded cache of restyled window item models. Keyed by the source item model it was made from
 * <em>and</em> the wood sprite: the round window and both arch pieces restyle different geometry, so
 * the same wood must never hand one item another's model (0.5.0 keyed by wood alone, with one window
 * item). Sources are compared by identity (baked models are), never modified, and the cache is
 * cleared with every other appearance cache on a model bake.
 */
final class WindowItemModels<M> {
    private record Key(Object source, ResourceLocation wood) {
    }

    private final AppearanceModelCache<Key, M> cache;

    WindowItemModels(int capacity) {
        this.cache = new AppearanceModelCache<>(capacity);
    }

    /** The model {@code build} makes from {@code source} for {@code wood}, built once per pair. */
    M get(M source, ResourceLocation wood, UnaryOperator<M> build) {
        return cache.get(new Key(source, wood), key -> build.apply(source));
    }

    int size() {
        return cache.size();
    }

    void clear() {
        cache.clear();
    }
}
