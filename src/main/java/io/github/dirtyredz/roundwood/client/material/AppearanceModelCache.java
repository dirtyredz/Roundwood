package io.github.dirtyredz.roundwood.client.material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A bounded least-recently-used cache of appearance models and quad lists. Material-model keys
 * describe looks/geometry; chunk-quad keys may include immutable positioned snapshots. Thread-safe:
 * models are built on chunk-render workers. Every instance
 * registers itself so one reload clears them all together with the donor caches
 * ({@link #clearAll}); stale models are therefore never drawn after F3+T or a resource change.
 */
public final class AppearanceModelCache<K, V> {
    /** Appearance-model entries per cache. */
    public static final int MODELS = 2048;
    private static final List<AppearanceModelCache<?, ?>> ALL = new ArrayList<>();
    private static volatile long generation;
    public static long generation() { return generation; }

    private final Map<K, V> entries;

    public AppearanceModelCache(int capacity) {
        this.entries = new LinkedHashMap<>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
        synchronized (ALL) {
            ALL.add(this);
        }
    }

    public synchronized V get(K key, Function<K, V> build) {
        V cached = entries.get(key);
        if (cached == null) {
            cached = build.apply(key);
            entries.put(key, cached);
        }
        return cached;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
    }

    /** Model reload: every appearance model, sampled colour and donor sprite goes. */
    public static void clearAll() {
        synchronized (ALL) {
            ALL.forEach(AppearanceModelCache::clear);
        }
        DonorSprites.clear();
        DonorColors.clear();
        generation++;
    }
}
