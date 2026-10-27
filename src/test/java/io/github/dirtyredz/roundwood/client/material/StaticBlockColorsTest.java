package io.github.dirtyredz.roundwood.client.material;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaticBlockColorsTest {
    @Test void donorAndArbitraryOriginalIndexRemainStableForRetainedMeshes() {
        var bindings = new TintBindings<String>();
        int grass = bindings.bind("grass", 0);
        int foliage = bindings.bind("synthetic_foliage", 7);
        int water = bindings.bind("synthetic_water", 42);
        assertNotEquals(grass, foliage);
        assertNotEquals(foliage, water);
        assertEquals(new TintBindings.Binding<>("synthetic_foliage", 7), bindings.get(foliage));
        assertEquals(-1, bindings.bind("paint", -1));
        assertNull(bindings.get(-1));
        assertEquals(grass, bindings.bind("grass", 0));
        assertEquals(water, bindings.bind("synthetic_water", 42));
        assertEquals(0x123456, StaticBlockColors.resolve(bindings.get(foliage), "sample-pos", (donor, pos, index) -> {
            assertEquals("synthetic_foliage", donor); assertEquals("sample-pos", pos); assertEquals(7, index);
            return 0x123456;
        }));
    }
}
