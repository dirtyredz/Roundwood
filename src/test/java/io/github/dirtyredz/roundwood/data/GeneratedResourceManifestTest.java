package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static io.github.dirtyredz.roundwood.data.GeneratedFiles.*;

class GeneratedResourceManifestTest {
    @Test
    void expectedIdentifierCountsAreRight() {
        assertEquals(15, ModelExpectations.aliasDoors().size());
        assertEquals(15, ModelExpectations.aliasWindows().size());
        assertEquals(19, ModelExpectations.aliasTrims().size());
        assertEquals(49, ALIASES.size());
        assertEquals(18, CANONICAL.size());
        assertEquals(26, WINDOWS.size());
        assertEquals(13, PLAIN_WINDOWS.size());
        assertEquals(13, RETIRED.size());
        assertEquals(80, BLOCKS.size());
        assertEquals(80, new HashSet<>(BLOCKS).size(), "identifiers must be unique");
    }

    @Test
    void everyBlockHasStateItemModelLootAndName() {
        JsonObject lang = json(assets("lang/en_us.json"));
        for (String block : BLOCKS) {
            assertTrue(Files.isRegularFile(assets("blockstates/" + block + ".json")), "blockstate " + block);
            assertTrue(Files.isRegularFile(assets("models/item/" + block + ".json")), "item model " + block);
            assertTrue(Files.isRegularFile(data("loot_tables/blocks/" + block + ".json")), "loot " + block);
            assertTrue(lang.has("block.roundwood." + block), "name " + block);
        }
        assertTrue(lang.has("itemGroup.roundwood"));
    }

    @Test
    void generatedSetsAreExactlyTheExpectedManifests() throws IOException {
        Set<String> block = new TreeSet<>(List.of("template_round_door_leaf_left", "template_round_door_leaf_right",
                "round_door_leaf_left", "round_door_leaf_right", "round_door", "round_trim",
                "template_arched_door_leaf_left", "template_arched_door_leaf_right", "arched_door_leaf_left",
                "arched_door_leaf_right", "arched_door", "archway", "keyhole_archway"));
        Set<String> item = new TreeSet<>(List.of("template_round_door", "round_door", "template_arched_door", "arched_door",
                "round_trim", WINDOW_TRIM,
                ARCH_SILL_TRIM, "cross_beams", "archway", "keyhole_archway"));
        block.addAll(windowBlockModels());
        block.addAll(ARCH_PIECES);
        item.addAll(WINDOWS);
        for (String part : PARTS) {
            block.add("template_round_trim_" + part);
            block.add("template_round_trim_whole_" + part);
            block.add("round_trim_" + part);
            block.add("round_trim_whole_" + part);
        }
        item.addAll(ALIASES);
        assertEquals(112, windowBlockModels().size());
        assertEquals(33 + 112, block.size()); // 22 before 0.11.0, the arched door adds 5; 0.12.0 the two archways
        assertEquals(85, item.size()); // 63 of 0.9.0, 16 new windows, arch_sill_trim; 0.11.0 the arched door and its template; 0.12.0 the two archways
        // Exact sets: no per-finish models remain; the 49 old ids are only item-model aliases.
        assertEquals(block, names(assets("models/block")));
        assertEquals(item, names(assets("models/item")));
        var stateBlocks = new TreeSet<>(BLOCKS);
        stateBlocks.addAll(ARCH_PIECES);
        assertEquals(stateBlocks, names(assets("blockstates")));
        var lootBlocks = new TreeSet<>(BLOCKS);
        lootBlocks.addAll(ARCH_PIECES);
        assertEquals(lootBlocks, names(data("loot_tables/blocks")));
        Set<String> crafted = new TreeSet<>(CANONICAL);
        crafted.add("cross_beams");
        crafted.add(ARCH_SILL_TRIM); // crafted, but an item only
        assertEquals(crafted, names(data("recipes")));
        assertEquals(crafted, names(data("advancements/recipes")));
        assertEquals(84, allJson(assets("blockstates")).size());
        assertEquals(33 + 112, allJson(assets("models/block")).size());
        assertEquals(85, allJson(assets("models/item")).size());
        assertEquals(20, allJson(data("recipes")).size());
        assertEquals(84, allJson(data("loot_tables/blocks")).size());
        assertEquals(20, allJson(data("advancements/recipes")).size());
    }
}
