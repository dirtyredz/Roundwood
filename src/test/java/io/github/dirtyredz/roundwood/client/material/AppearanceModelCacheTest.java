package io.github.dirtyredz.roundwood.client.material;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The bounded least-recently-used cache of appearance models, with a counting builder. */
class AppearanceModelCacheTest {
    /** Builds length-of-key values and remembers every key it was asked to build. */
    private static final class Counting {
        final List<String> built = new ArrayList<>();

        Integer build(String key) {
            built.add(key);
            return key.length() * 100 + built.size();
        }

        long count(String key) {
            return built.stream().filter(key::equals).count();
        }
    }

    @Test
    void getBuildsEachKeyOnceAndReturnsTheSameValue() {
        AppearanceModelCache<String, Integer> cache = new AppearanceModelCache<>(8);
        Counting builder = new Counting();
        Integer first = cache.get("oak", builder::build);
        Integer again = cache.get("oak", builder::build);
        assertEquals(first, again);
        assertEquals(1, builder.count("oak"));
        assertEquals(1, cache.size());
    }

    @Test
    void distinctKeysGetDistinctValuesEachBuiltOnce() {
        AppearanceModelCache<String, Integer> cache = new AppearanceModelCache<>(8);
        Counting builder = new Counting();
        Integer a = cache.get("a", builder::build);
        Integer bb = cache.get("bb", builder::build);
        Integer ccc = cache.get("ccc", builder::build);
        assertTrue(a.intValue() != bb.intValue() && bb.intValue() != ccc.intValue() && a.intValue() != ccc.intValue());
        assertEquals(a, cache.get("a", builder::build));
        assertEquals(bb, cache.get("bb", builder::build));
        assertEquals(ccc, cache.get("ccc", builder::build));
        assertEquals(List.of("a", "bb", "ccc"), builder.built);
        assertEquals(3, cache.size());
    }

    @Test
    void theLeastRecentlyUsedKeyIsEvictedBeyondCapacity() {
        AppearanceModelCache<String, Integer> cache = new AppearanceModelCache<>(3);
        Counting builder = new Counting();
        cache.get("a", builder::build);
        cache.get("b", builder::build);
        cache.get("c", builder::build);
        cache.get("a", builder::build); // a is now the most recently used; b is the oldest
        cache.get("d", builder::build); // evicts b
        assertEquals(3, cache.size());
        cache.get("a", builder::build);
        cache.get("c", builder::build);
        cache.get("d", builder::build);
        assertEquals(1, builder.count("a"), "a was read recently and must have survived");
        assertEquals(1, builder.count("c"));
        assertEquals(1, builder.count("d"));
        cache.get("b", builder::build);
        assertEquals(2, builder.count("b"), "b was the least recently used and must have been evicted");
    }

    @Test
    void sizeNeverExceedsCapacityWhateverIsAsked() {
        int capacity = 5;
        AppearanceModelCache<String, Integer> cache = new AppearanceModelCache<>(capacity);
        Counting builder = new Counting();
        for (int i = 0; i < 100; i++) {
            cache.get("key" + i, builder::build);
            assertTrue(cache.size() <= capacity, "size " + cache.size() + " after " + (i + 1) + " keys");
        }
        assertEquals(capacity, cache.size());
        assertEquals(100, builder.built.size());
        // The most recent keys are the ones still cached.
        for (int i = 95; i < 100; i++) {
            cache.get("key" + i, builder::build);
        }
        assertEquals(100, builder.built.size(), "the five newest keys must still be cached");
    }

    @Test
    void clearEmptiesTheCacheAndForcesRebuilds() {
        AppearanceModelCache<String, Integer> cache = new AppearanceModelCache<>(4);
        Counting builder = new Counting();
        cache.get("a", builder::build);
        cache.get("b", builder::build);
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
        cache.get("a", builder::build);
        assertEquals(2, builder.count("a"));
        assertEquals(1, cache.size());
    }

    @Test
    void clearAllEmptiesEveryInstance() {
        AppearanceModelCache<String, Integer> first = new AppearanceModelCache<>(4);
        AppearanceModelCache<Integer, String> second = new AppearanceModelCache<>(4);
        AppearanceModelCache<String, Integer> third = new AppearanceModelCache<>(AppearanceModelCache.MODELS);
        Counting builder = new Counting();
        first.get("a", builder::build);
        first.get("b", builder::build);
        second.get(1, n -> "one");
        third.get("c", builder::build);
        assertEquals(2, first.size());
        assertEquals(1, second.size());
        assertEquals(1, third.size());
        AppearanceModelCache.clearAll();
        assertEquals(0, first.size());
        assertEquals(0, second.size());
        assertEquals(0, third.size());
        first.get("a", builder::build);
        assertEquals(2, builder.count("a"), "a cleared entry is rebuilt");
    }
}
