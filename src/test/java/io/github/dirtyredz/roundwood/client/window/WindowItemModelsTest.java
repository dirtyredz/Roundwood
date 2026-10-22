package io.github.dirtyredz.roundwood.client.window;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The restyled window item cache. 0.5.0 keyed it by wood alone, which with three window items would
 * hand an arch the round window's model in the same wood. Plain objects stand in for baked models
 * (compared by identity, as baked models are).
 */
class WindowItemModelsTest {
    private static final ResourceLocation SPRUCE = ResourceLocation.fromNamespaceAndPath("minecraft", "block/spruce_planks");
    private static final ResourceLocation BIRCH = ResourceLocation.fromNamespaceAndPath("minecraft", "block/birch_planks");
    private static final ResourceLocation CLEAR = ResourceLocation.fromNamespaceAndPath("roundwood", "block/window_glass_clear");
    private static final ResourceLocation RED_GLASS = ResourceLocation.fromNamespaceAndPath("roundwood", "block/window_glass_red");
    private static final ResourceLocation BLUE_GLASS = ResourceLocation.fromNamespaceAndPath("roundwood", "block/window_glass_blue");

    /** A built model: remembers its source, never modifies it. */
    private record Built(Object source, int serial) {
    }

    @Test
    void distinctSourcesInTheSameWoodStayDistinct() {
        WindowItemModels<Object> cache = new WindowItemModels<>(16);
        Object round = new Object();
        Object archLeft = new Object();
        Object archRight = new Object();
        List<Object> built = new ArrayList<>();
        Object a = cache.get(round, SPRUCE, CLEAR, source -> record(built, source));
        Object b = cache.get(archLeft, SPRUCE, CLEAR, source -> record(built, source));
        Object c = cache.get(archRight, SPRUCE, CLEAR, source -> record(built, source));
        assertEquals(round, ((Built) a).source());
        assertEquals(archLeft, ((Built) b).source());
        assertEquals(archRight, ((Built) c).source());
        assertEquals(3, built.size());
        assertSame(a, cache.get(round, SPRUCE, CLEAR, source -> record(built, source)), "the same pair is built once");
        assertNotSame(a, cache.get(round, BIRCH, CLEAR, source -> record(built, source)), "another wood is another model");
        assertEquals(4, cache.size());
    }

    /** 0.8.0: the same window in the same wood with differently tinted glass must never share a model. */
    @Test
    void differentGlassInTheSameWoodNeverCollides() {
        WindowItemModels<Object> cache = new WindowItemModels<>(16);
        Object round = new Object();
        List<Object> built = new ArrayList<>();
        Object clear = cache.get(round, SPRUCE, CLEAR, source -> record(built, source));
        Object red = cache.get(round, SPRUCE, RED_GLASS, source -> record(built, source));
        Object blue = cache.get(round, SPRUCE, BLUE_GLASS, source -> record(built, source));
        assertNotSame(clear, red);
        assertNotSame(red, blue);
        assertSame(red, cache.get(round, SPRUCE, RED_GLASS, source -> record(built, source)));
        assertEquals(3, built.size());
    }

    @Test
    void independentBeamsCacheSeparatelyFromThePlainItemAndEachOther() {
        WindowItemModels<Object> cache = new WindowItemModels<>(16);
        Object source = new Object();
        List<Object> built = new ArrayList<>();
        Object plain = cache.get(source, SPRUCE, CLEAR, s -> record(built, s));
        Object spruce = cache.get(source, SPRUCE, CLEAR, SPRUCE, s -> record(built, s));
        Object birch = cache.get(source, SPRUCE, CLEAR, BIRCH, s -> record(built, s));
        assertNotSame(plain, spruce);
        assertNotSame(spruce, birch);
        assertSame(spruce, cache.get(source, SPRUCE, CLEAR, SPRUCE, s -> record(built, s)));
        assertEquals(3, built.size());
    }

    @Test
    void sourcesAreNeverReplacedAndClearingRebuilds() {
        WindowItemModels<Object> cache = new WindowItemModels<>(16);
        Object source = new Object();
        List<Object> built = new ArrayList<>();
        Object first = cache.get(source, SPRUCE, CLEAR, s -> record(built, s));
        assertNotSame(source, first);
        cache.clear();
        assertEquals(0, cache.size());
        Object second = cache.get(source, SPRUCE, CLEAR, s -> record(built, s));
        assertNotSame(first, second, "after a reload the model is rebuilt from the current source");
        assertEquals(2, built.size());
    }

    @Test
    void theCacheStaysBounded() {
        WindowItemModels<Object> cache = new WindowItemModels<>(4);
        for (int i = 0; i < 20; i++) {
            cache.get(new Object(), SPRUCE, CLEAR, s -> s);
        }
        assertEquals(4, cache.size());
    }

    private static Object record(List<Object> built, Object source) {
        Built model = new Built(source, built.size());
        built.add(model);
        return model;
    }
}
