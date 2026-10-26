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

class GeneratedContentResourcesTest {
    @Test void modularArchLootKeepsThreeIndependentLooks() {
        for (String id : ARCH_PIECES) {
            var pools = json(data("loot_tables/blocks/" + id + ".json")).getAsJsonArray("pools");
            assertEquals(3, pools.size());
            assertEquals(JsonParser.parseString(ownItemPool("roundwood:" + id)), pools.get(0));
            assertEquals(JsonParser.parseString(trimFacePool("front", "roundwood:round_trim", "ArchPieceTrim.Front")), pools.get(1));
            assertEquals(JsonParser.parseString(trimFacePool("back", "roundwood:round_trim", "ArchPieceTrim.Back")), pools.get(2));
        }
    }

    @Test
    void windowTrimIsAnItemWithoutABlock() {
        assertTrue(Files.isRegularFile(assets("models/item/" + WINDOW_TRIM + ".json")), "item model");
        assertTrue(json(assets("lang/en_us.json")).has("block.roundwood." + WINDOW_TRIM), "name");
        assertFalse(json(assets("lang/en_us.json")).has("item.roundwood." + WINDOW_TRIM), "no old item key");
        JsonObject model = json(assets("models/item/" + WINDOW_TRIM + ".json"));
        assertEquals(Set.of("parent"), model.keySet(), "item model has only a parent");
        assertEquals("roundwood:item/round_trim", model.get("parent").getAsString());
        assertFalse(Files.exists(data("recipes/" + WINDOW_TRIM + ".json")), "no recipe");
        assertFalse(Files.exists(assets("blockstates/" + WINDOW_TRIM + ".json")), "no blockstate");
        assertFalse(Files.exists(data("loot_tables/blocks/" + WINDOW_TRIM + ".json")), "no loot table");
        assertFalse(Files.exists(assets("models/block/" + WINDOW_TRIM + ".json")), "no block model");
    }

    @Test
    void archSillTrimIsAnItemWithoutABlock() {
        assertTrue(Files.isRegularFile(assets("models/item/" + ARCH_SILL_TRIM + ".json")), "item model");
        JsonObject lang = json(assets("lang/en_us.json"));
        assertEquals("Arch Sill", lang.get("item.roundwood." + ARCH_SILL_TRIM).getAsString());
        assertTrue(lang.has("tooltip.roundwood." + ARCH_SILL_TRIM), "tooltip");
        assertTrue(lang.has("message.roundwood.sill_upper_arch_only"), "upper-arch-only message");
        assertFalse(lang.has("block.roundwood." + ARCH_SILL_TRIM), "no block name");
        assertFalse(Files.exists(assets("blockstates/" + ARCH_SILL_TRIM + ".json")), "no blockstate");
        assertFalse(Files.exists(data("loot_tables/blocks/" + ARCH_SILL_TRIM + ".json")), "no loot table");
        assertFalse(Files.exists(assets("models/block/" + ARCH_SILL_TRIM + ".json")), "no block model");
    }

    @Test
    void doorTagsListTheCanonicalAndAliasDoorsOnly() {
        Path tags = GENERATED.resolve("data/minecraft/tags");
        Set<String> blockDoors = new TreeSet<>();
        blockDoors.add("roundwood:round_door");
        blockDoors.add("roundwood:arched_door");
        blockDoors.add("roundwood:double_arched_door");
        ModelExpectations.aliasDoors().forEach(d -> blockDoors.add("roundwood:" + d));
        assertEquals(18, blockDoors.size());
        for (String tag : List.of("blocks/doors", "blocks/wooden_doors")) {
            assertEquals(blockDoors, tagValues(tags.resolve(tag + ".json")), tag);
        }
        for (String tag : List.of("items/doors", "items/wooden_doors")) {
            assertEquals(Set.of("roundwood:round_door", "roundwood:arched_door", "roundwood:double_arched_door"), tagValues(tags.resolve(tag + ".json")), tag);
        }
    }

    @Test
    void everyBlockIsMineableWithTheAxeAndNothingWithThePickaxe() {
        Path tags = GENERATED.resolve("data/minecraft/tags/blocks/mineable");
        Set<String> axe = new TreeSet<>();
        BLOCKS.forEach(b -> axe.add("roundwood:" + b));
        assertEquals(axe, tagValues(tags.resolve("axe.json")));
        Path pickaxe = tags.resolve("pickaxe.json");
        if (Files.exists(pickaxe)) {
            assertTrue(tagValues(pickaxe).stream().noneMatch(v -> v.startsWith("roundwood:")), "no roundwood block is a pickaxe block");
        }
    }

    @Test
    void familyTagsListOnlyTheCanonicalBlocks() {
        Path tags = GENERATED.resolve("data/roundwood/tags");
        for (String kind : List.of("blocks", "items")) {
            assertEquals(Set.of("roundwood:round_door", "roundwood:arched_door", "roundwood:double_arched_door"), tagValues(tags.resolve(kind + "/round_doors.json")), kind);
            Set<String> windows = new TreeSet<>();
            WINDOWS.forEach(w -> windows.add("roundwood:" + w));
            assertEquals(26, windows.size());
            assertEquals(windows, tagValues(tags.resolve(kind + "/round_windows.json")), kind);
        }
        for (String kind : List.of("blocks", "items")) {
            assertEquals(Set.of("roundwood:archway", "roundwood:keyhole_archway"),
                    tagValues(tags.resolve(kind + "/archways.json")), kind + " archways");
        }
        for (String kind : List.of("blocks", "items")) {
            assertEquals(ARCH_PIECES.stream().map(id -> "roundwood:" + id).collect(java.util.stream.Collectors.toSet()),
                    tagValues(tags.resolve(kind + "/arch_pieces.json")), kind + " modular pieces");
        }
        assertEquals(Set.of("roundwood:round_trim"), tagValues(tags.resolve("blocks/round_trims.json")), "block trims");
        assertEquals(Set.of("roundwood:round_trim", "roundwood:window_trim"),
                tagValues(tags.resolve("items/round_trims.json")), "item trims");
    }

    @Test
    void lootDropsTheCanonicalItemOfTheBlocksFamily() {
        for (String block : BLOCKS) {
            JsonObject loot = json(data("loot_tables/blocks/" + block + ".json"));
            assertEquals("minecraft:block", loot.get("type").getAsString(), block);
            JsonArray pools = loot.getAsJsonArray("pools");
            if (RETIRED.contains(block)) {
                assertEquals(1, pools.size(), block);
                assertEquals(JsonParser.parseString(ownItemPool("roundwood:" + block)), pools.get(0), block);
                continue;
            }
            boolean window = WINDOWS.contains(block) || block.endsWith("_round_window");
            boolean door = block.equals("round_door") || block.equals("arched_door") || block.equals("double_arched_door") || block.endsWith("_round_door");
            boolean piece = ARCH_PIECES.contains(block);
            boolean archway = ARCHWAYS.contains(block) || piece;
            boolean upperArch = UPPER_ARCHES.contains(block);
            assertEquals(upperArch ? 6 : window ? 4 : door || archway ? 3 : 1, pools.size(), block);
            assertEquals(JsonParser.parseString(ownItemPool("roundwood:" + family(block))), pools.get(0), block);
            if (window || door || archway) {
                String item = "roundwood:round_trim";
                String tag = piece ? "ArchPieceTrim" : archway ? "ArchwayTrim" : window ? "WindowTrim" : "DoorTrim";
                assertEquals(JsonParser.parseString(trimFacePool("front", item, tag + ".Front")), pools.get(1), block + ": front trim pool");
                assertEquals(JsonParser.parseString(trimFacePool("back", item, tag + ".Back")), pools.get(2), block + ": back trim pool");
            }
            if (window) {
                assertEquals(JsonParser.parseString(beamPool()), pools.get(pools.size() - 1), block + ": singleton beam pool");
            }
            if (upperArch) {
                assertEquals(JsonParser.parseString(sillFacePool("front", "WindowSillTrim.Front")), pools.get(3), block + ": front sill pool");
                assertEquals(JsonParser.parseString(sillFacePool("back", "WindowSillTrim.Back")), pools.get(4), block + ": back sill pool");
            }
        }
        for (String canonical : CANONICAL) { // canonical blocks drop themselves
            assertEquals("roundwood:" + canonical, json(data("loot_tables/blocks/" + canonical + ".json"))
                    .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                    .getAsJsonObject().get("name").getAsString());
        }
        assertEquals("roundwood:round_window", json(data("loot_tables/blocks/oak_round_window.json"))
                .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject().get("name").getAsString());
    }

    @Test
    void langNamesCanonicalAndAliasBlocksAndKeepsTheTooltips() {
        JsonObject lang = json(assets("lang/en_us.json"));
        assertEquals("Round Door", lang.get("block.roundwood.round_door").getAsString());
        assertEquals("Arched Door", lang.get("block.roundwood.arched_door").getAsString());
        for (String id : ARCH_PIECES) {
            assertEquals(id.equals("keyhole_shoulder") ? "Keyhole Shoulder" : "Arch Top " + id.substring(9),
                    lang.get("block.roundwood." + id).getAsString());
        }
        assertTrue(lang.has("tooltip.roundwood.arch_piece") && lang.has("tooltip.roundwood.retired_archway"));
        assertEquals("Archway", lang.get("block.roundwood.archway").getAsString());
        assertEquals("Keyhole Archway", lang.get("block.roundwood.keyhole_archway").getAsString());
        List<String> windowNames = List.of("Round Window", "Round Window with Cross Beams", "Round Window Arch, Left",
                "Round Window Arch with Cross Beams, Left", "Round Window Arch, Right",
                "Round Window Arch with Cross Beams, Right", "Round Window Corner, Bottom Left",
                "Round Window Corner with Cross Beams, Bottom Left", "Round Window Corner, Bottom Right",
                "Round Window Corner with Cross Beams, Bottom Right",
                "Round Window Arch, Bottom Left", "Round Window Arch with Cross Beams, Bottom Left",
                "Round Window Arch, Bottom Right", "Round Window Arch with Cross Beams, Bottom Right",
                "Round Window Corner, Top Left", "Round Window Corner with Cross Beams, Top Left",
                "Round Window Corner, Top Right", "Round Window Corner with Cross Beams, Top Right",
                "Round Window Edge, Top", "Round Window Edge with Cross Beams, Top",
                "Round Window Edge, Left", "Round Window Edge with Cross Beams, Left",
                "Round Window Edge, Bottom", "Round Window Edge with Cross Beams, Bottom",
                "Round Window Edge, Right", "Round Window Edge with Cross Beams, Right");
        assertEquals(WINDOWS.size(), windowNames.size());
        for (int i = 0; i < WINDOWS.size(); i++) {
            assertEquals(windowNames.get(i), lang.get("block.roundwood." + WINDOWS.get(i)).getAsString(), WINDOWS.get(i));
        }
        assertEquals("Window Trim", lang.get("block.roundwood.window_trim").getAsString());
        assertTrue(lang.has("tooltip.roundwood.universal_trim"));
        assertFalse(lang.has("tooltip.roundwood.window_trim") || lang.has("tooltip.roundwood.door_trim"), "retired trim tooltips");
        assertEquals("Universal Trim", lang.get("block.roundwood.round_trim").getAsString());
        for (String alias : ALIASES) {
            assertEquals(englishName(alias), lang.get("block.roundwood." + alias).getAsString(), alias);
        }
        assertEquals("Dark Oak Round Door", lang.get("block.roundwood.dark_oak_round_door").getAsString());
        assertEquals("Stone Brick Round Trim", lang.get("block.roundwood.stone_brick_round_trim").getAsString());
        assertTrue(lang.has("itemGroup.roundwood"));
        assertTrue(lang.keySet().stream().anyMatch(k -> k.startsWith("tooltip.roundwood.")), "tooltip keys");
        assertTrue(lang.keySet().stream().anyMatch(k -> k.startsWith("message.roundwood.")), "message keys");
    }
}
