package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.migration.LegacyAppearances.Entry;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 49 ids Roundwood 0.4.0 registered, checked against a hand-written table that does not share
 * code with the mod: a mistake in the catalogue (a wrong plank, a missing id) shows up as a diff.
 */
class LegacyAppearancesTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped");
    private static final Map<String, DyeColor> PAINTS = new LinkedHashMap<>();
    private static final Map<String, String> MASONRY = new LinkedHashMap<>();

    static {
        PAINTS.put("green", DyeColor.GREEN);
        PAINTS.put("yellow", DyeColor.YELLOW);
        PAINTS.put("red", DyeColor.RED);
        PAINTS.put("blue", DyeColor.BLUE);
        MASONRY.put("brick", "bricks");
        MASONRY.put("stone_brick", "stone_bricks");
        MASONRY.put("deepslate_brick", "deepslate_bricks");
        MASONRY.put("mud_brick", "mud_bricks");
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    /** The finish table for one family: id suffix -> expectation, in 0.4.0's registration order. */
    private static Map<String, Entry> finishes(AppearanceFamily family, String suffix) {
        Map<String, Entry> out = new LinkedHashMap<>();
        for (String wood : WOODS) {
            out.put(wood + suffix, new Entry(wood + suffix, family, Appearance.wood(mc(wood + "_planks"))));
        }
        PAINTS.forEach((name, dye) -> out.put(name + suffix,
                new Entry(name + suffix, family, Appearance.wood(mc("oak_planks")).painted(dye))));
        return out;
    }

    private static Map<String, Entry> expectedDoors() {
        return finishes(AppearanceFamily.DOOR, "_round_door");
    }

    private static Map<String, Entry> expectedWindows() {
        return finishes(AppearanceFamily.WINDOW, "_round_window");
    }

    private static Map<String, Entry> expectedTrims() {
        Map<String, Entry> out = finishes(AppearanceFamily.TRIM, "_round_trim");
        MASONRY.forEach((name, block) -> out.put(name + "_round_trim",
                new Entry(name + "_round_trim", AppearanceFamily.TRIM, Appearance.masonry(mc(block)))));
        return out;
    }

    private static Map<String, Entry> expectedAll() {
        Map<String, Entry> all = new LinkedHashMap<>();
        all.putAll(expectedDoors());
        all.putAll(expectedWindows());
        all.putAll(expectedTrims());
        return all;
    }

    @Test
    void theExpectedTableHasFortyNineDistinctIds() {
        assertEquals(15, expectedDoors().size());
        assertEquals(15, expectedWindows().size());
        assertEquals(19, expectedTrims().size());
        assertEquals(49, expectedAll().size());
    }

    @Test
    void familyListsMatchExactlyAndInOrder() {
        assertEquals(new ArrayList<>(expectedDoors().values()), LegacyAppearances.DOORS);
        assertEquals(new ArrayList<>(expectedWindows().values()), LegacyAppearances.WINDOWS);
        assertEquals(new ArrayList<>(expectedTrims().values()), LegacyAppearances.TRIMS);
    }

    @Test
    void allIsTheFortyNineWithNoOmissionsExtrasOrDuplicates() {
        List<Entry> all = LegacyAppearances.ALL;
        assertEquals(49, all.size());
        Set<String> ids = new HashSet<>();
        all.forEach(entry -> assertTrue(ids.add(entry.id()), "duplicate id " + entry.id()));
        assertEquals(expectedAll().keySet(), ids);
        assertEquals(new ArrayList<>(expectedAll().values()), all);
        assertEquals(15 + 15 + 19, LegacyAppearances.DOORS.size() + LegacyAppearances.WINDOWS.size()
                + LegacyAppearances.TRIMS.size());
    }

    @Test
    void everyIdMapsToItsExpectedFamilyAndAppearance() {
        expectedAll().forEach((id, expected) -> {
            Optional<Entry> found = LegacyAppearances.byId(id);
            assertTrue(found.isPresent(), "missing " + id);
            assertEquals(expected.family(), found.get().family(), id);
            assertEquals(expected.appearance(), found.get().appearance(), id);
        });
    }

    @Test
    void woodsMapToTheirPlanksUnpaintedAndPaintsToOakWithThatDye() {
        for (String wood : WOODS) {
            for (String suffix : List.of("_round_door", "_round_window", "_round_trim")) {
                Appearance appearance = LegacyAppearances.byId(wood + suffix).orElseThrow().appearance();
                assertEquals(Appearance.wood(mc(wood + "_planks")), appearance, wood + suffix);
                assertEquals(Optional.empty(), appearance.paint(), wood + suffix);
            }
        }
        PAINTS.forEach((name, dye) -> {
            for (String suffix : List.of("_round_door", "_round_window", "_round_trim")) {
                Appearance appearance = LegacyAppearances.byId(name + suffix).orElseThrow().appearance();
                assertEquals(mc("oak_planks"), appearance.material(), name + suffix);
                assertEquals(Optional.of(dye), appearance.paint(), name + suffix);
            }
        });
    }

    @Test
    void masonryTrimsMapToTheirBlocksOnlyAsTrims() {
        MASONRY.forEach((name, block) -> {
            Entry entry = LegacyAppearances.byId(name + "_round_trim").orElseThrow();
            assertEquals(AppearanceFamily.TRIM, entry.family(), name);
            assertEquals(Appearance.Kind.MASONRY, entry.appearance().kind(), name);
            assertEquals(mc(block), entry.appearance().material(), name);
            assertTrue(LegacyAppearances.byId(name + "_round_door").isEmpty(), name + " door never existed");
            assertTrue(LegacyAppearances.byId(name + "_round_window").isEmpty(), name + " window never existed");
        });
    }

    @Test
    void unknownIdsAreRejected() {
        assertTrue(LegacyAppearances.byId("oak_round_chair").isEmpty());
        assertTrue(LegacyAppearances.byId("round_door").isEmpty(), "the canonical id is not a legacy alias");
        assertTrue(LegacyAppearances.byId("round_window").isEmpty());
        assertTrue(LegacyAppearances.byId("round_trim").isEmpty());
        assertTrue(LegacyAppearances.byId("white_round_door").isEmpty(), "dyes beyond 0.4.0's four never existed");
        assertTrue(LegacyAppearances.byId("roundwood:oak_round_door").isEmpty(), "ids are bare paths");
        assertTrue(LegacyAppearances.byId("").isEmpty());
    }
}
