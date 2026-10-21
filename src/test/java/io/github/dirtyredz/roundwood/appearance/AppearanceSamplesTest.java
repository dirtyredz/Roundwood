package io.github.dirtyredz.roundwood.appearance;

import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The bounded, deterministic example list used for previews and the showcase. */
class AppearanceSamplesTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    private static final List<String> VANILLA = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped");
    private static final List<ResourceLocation> MASONRY = List.of(mc("bricks"), mc("stone_bricks"),
            mc("deepslate_bricks"), mc("mud_bricks"));

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    private static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath("modded", path);
    }

    private static List<ResourceLocation> vanillaIds() {
        return VANILLA.stream().map(w -> mc(w + "_planks")).toList();
    }

    @Test
    void vanillaWoodsComeFirstInFixedOrderEvenWhenNothingIsPresent() {
        List<Appearance> samples = AppearanceSamples.forFamily(AppearanceFamily.DOOR, List.of(), MASONRY);
        assertEquals(11, AppearanceSamples.VANILLA_WOODS.size());
        assertEquals(vanillaIds(), AppearanceSamples.VANILLA_WOODS);
        for (int i = 0; i < 11; i++) {
            assertEquals(Appearance.wood(vanillaIds().get(i)), samples.get(i), "index " + i);
        }
        // 11 woods, then 16 paints, no modded woods and (door) no masonry.
        assertEquals(11 + 16, samples.size());
    }

    @Test
    void moddedWoodsFollowSortedDeduplicatedAndWithoutVanillaDuplicates() {
        List<ResourceLocation> present = new ArrayList<>(vanillaIds());
        present.add(mod("zebra_planks"));
        present.add(mod("apple_planks"));
        present.add(mod("apple_planks"));
        present.add(mc("oak_planks"));
        List<Appearance> samples = AppearanceSamples.forFamily(AppearanceFamily.WINDOW, present, MASONRY);
        assertEquals(Appearance.wood(mod("apple_planks")), samples.get(11));
        assertEquals(Appearance.wood(mod("zebra_planks")), samples.get(12));
        assertEquals(11 + 2 + 16, samples.size());
        long oaks = samples.stream().filter(a -> a.equals(Appearance.wood(mc("oak_planks")))).count();
        assertEquals(1, oaks, "vanilla oak must not be repeated");
        assertEquals(List.of(mod("apple_planks"), mod("zebra_planks")), AppearanceSamples.modded(present));
    }

    @Test
    void moddedWoodsAreCappedAtEightEvenWhenTwentyAreOffered() {
        assertEquals(8, AppearanceSamples.MAX_MODDED);
        List<ResourceLocation> present = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            present.add(mod(String.format("wood_%02d_planks", i)));
        }
        List<ResourceLocation> modded = AppearanceSamples.modded(present);
        assertEquals(8, modded.size());
        for (int i = 0; i < 8; i++) {
            assertEquals(mod(String.format("wood_%02d_planks", i)), modded.get(i), "the first eight by id");
        }
        List<Appearance> samples = AppearanceSamples.forFamily(AppearanceFamily.DOOR, present, MASONRY);
        assertEquals(11 + 8 + 16, samples.size());
    }

    @Test
    void sixteenPaintsOnOakFollowDyeColorOrder() {
        List<Appearance> samples = AppearanceSamples.forFamily(AppearanceFamily.DOOR, List.of(), MASONRY);
        DyeColor[] dyes = DyeColor.values();
        assertEquals(16, dyes.length);
        for (int i = 0; i < dyes.length; i++) {
            assertEquals(Appearance.DEFAULT.painted(dyes[i]), samples.get(11 + i), dyes[i].getName());
        }
    }

    @Test
    void masonryIsAppendedForTrimsOnlyInGivenOrder() {
        List<Appearance> door = AppearanceSamples.forFamily(AppearanceFamily.DOOR, List.of(), MASONRY);
        List<Appearance> window = AppearanceSamples.forFamily(AppearanceFamily.WINDOW, List.of(), MASONRY);
        List<Appearance> trim = AppearanceSamples.forFamily(AppearanceFamily.TRIM, List.of(), MASONRY);
        assertTrue(door.stream().allMatch(Appearance::isWood));
        assertTrue(window.stream().allMatch(Appearance::isWood));
        assertEquals(27 + 4, trim.size());
        assertEquals(door, trim.subList(0, 27));
        for (int i = 0; i < 4; i++) {
            assertEquals(Appearance.masonry(MASONRY.get(i)), trim.get(27 + i), "masonry " + i);
        }
        List<ResourceLocation> reversed = new ArrayList<>(MASONRY);
        Collections.reverse(reversed);
        List<Appearance> reordered = AppearanceSamples.forFamily(AppearanceFamily.TRIM, List.of(), reversed);
        assertEquals(Appearance.masonry(MASONRY.get(3)), reordered.get(27), "given order is kept, not sorted");
    }

    @Test
    void theResultIsDeterministicWhateverTheInputOrder() {
        List<ResourceLocation> present = new ArrayList<>(vanillaIds());
        for (int i = 0; i < 12; i++) {
            present.add(mod("wood_" + i + "_planks"));
        }
        List<Appearance> first = AppearanceSamples.forFamily(AppearanceFamily.TRIM, present, MASONRY);
        for (long seed = 1; seed <= 5; seed++) {
            List<ResourceLocation> shuffled = new ArrayList<>(present);
            Collections.shuffle(shuffled, new Random(seed));
            assertEquals(first, AppearanceSamples.forFamily(AppearanceFamily.TRIM, shuffled, MASONRY), "seed " + seed);
        }
        Set<ResourceLocation> asSet = new HashSet<>(present);
        assertEquals(first, AppearanceSamples.forFamily(AppearanceFamily.TRIM, asSet, MASONRY));
    }

    @Test
    void theSizeIsBounded() {
        List<ResourceLocation> many = new ArrayList<>(vanillaIds());
        for (int i = 0; i < 200; i++) {
            many.add(mod("wood_" + i + "_planks"));
        }
        for (AppearanceFamily family : AppearanceFamily.values()) {
            List<Appearance> samples = AppearanceSamples.forFamily(family, many, MASONRY);
            assertTrue(samples.size() <= 11 + 8 + 16 + 4, family + ": " + samples.size());
            assertEquals(samples.size(), new HashSet<>(samples).size(), family + ": duplicates");
        }
        assertEquals(11 + 8 + 16 + 4, AppearanceSamples.forFamily(AppearanceFamily.TRIM, many, MASONRY).size());
        assertEquals(11 + 8 + 16, AppearanceSamples.forFamily(AppearanceFamily.DOOR, many, MASONRY).size());
    }
}
