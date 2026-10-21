package io.github.dirtyredz.roundwood.appearance;

import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The stable appearance key, the default look and the kind ids. */
class AppearanceKeyTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    @Test
    void keysHaveTheDocumentedShape() {
        assertEquals("wood/minecraft:oak_planks", Appearance.wood(mc("oak_planks")).key());
        assertEquals("wood/minecraft:oak_planks/red", Appearance.wood(mc("oak_planks")).painted(DyeColor.RED).key());
        assertEquals("wood/minecraft:light_blue_planks", Appearance.wood(mc("light_blue_planks")).key());
        assertEquals("wood/minecraft:oak_planks/light_blue",
                Appearance.wood(mc("oak_planks")).painted(DyeColor.LIGHT_BLUE).key());
        assertEquals("masonry/minecraft:bricks", Appearance.masonry(mc("bricks")).key());
        assertEquals("wood/othermod:cedar_planks",
                Appearance.wood(ResourceLocation.fromNamespaceAndPath("othermod", "cedar_planks")).key());
    }

    @Test
    void masonryKeysNeverCarryPaint() {
        assertEquals("masonry/minecraft:bricks", Appearance.masonry(mc("bricks")).painted(DyeColor.RED).key());
    }

    @Test
    void equalAppearancesShareAKeyAndUnequalOnesNever() {
        List<Appearance> all = new ArrayList<>();
        for (String wood : List.of("oak_planks", "spruce_planks")) {
            all.add(Appearance.wood(mc(wood)));
            for (DyeColor dye : DyeColor.values()) {
                all.add(Appearance.wood(mc(wood)).painted(dye));
            }
        }
        for (String masonry : List.of("bricks", "stone_bricks", "deepslate_bricks", "mud_bricks")) {
            all.add(Appearance.masonry(mc(masonry)));
        }
        assertEquals(2 * 17 + 4, all.size());
        Map<String, Appearance> byKey = new HashMap<>();
        for (Appearance appearance : all) {
            assertNull(byKey.put(appearance.key(), appearance), "key collision: " + appearance.key());
        }
        assertEquals(all.size(), byKey.size());
        // A rebuilt equal appearance gives the same key.
        for (Appearance appearance : all) {
            Appearance copy = new Appearance(appearance.kind(), appearance.material(), appearance.paint());
            assertEquals(appearance, copy);
            assertEquals(appearance.key(), copy.key());
        }
        assertNotEquals(Appearance.wood(mc("oak_planks")).key(), Appearance.masonry(mc("oak_planks")).key());
    }

    @Test
    void theDefaultIsUnpaintedOak() {
        Appearance oak = Appearance.DEFAULT;
        assertEquals(Appearance.Kind.WOOD, oak.kind());
        assertEquals(mc("oak_planks"), oak.material());
        assertEquals(Optional.empty(), oak.paint());
        assertEquals(Appearance.OAK_PLANKS, oak.material());
        assertEquals("wood/minecraft:oak_planks", oak.key());
        assertTrue(oak.isWood());
        assertFalse(oak.isPainted());
    }

    @Test
    void kindIdsRoundTripAndUnknownIdsAreRejected() {
        assertEquals("wood", Appearance.Kind.WOOD.id());
        assertEquals("masonry", Appearance.Kind.MASONRY.id());
        for (Appearance.Kind kind : Appearance.Kind.values()) {
            assertEquals(Optional.of(kind), Appearance.Kind.byId(kind.id()));
        }
        assertEquals(Optional.empty(), Appearance.Kind.byId("stone"));
        assertEquals(Optional.empty(), Appearance.Kind.byId(""));
        assertEquals(Optional.empty(), Appearance.Kind.byId("WOOD"));
    }
}
