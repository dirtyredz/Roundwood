package io.github.dirtyredz.roundwood.data.models;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Independent saved-build expectations: historical strings and known payloads, not registrations. */
class GeneratedArchwayFixtureTest {
    @Test
    void savedArchwaysKeepHistoricalPartsAnchorsAndIndependentLooks() throws Exception {
        CompoundTag fixture;
        try (var input = Files.newInputStream(Path.of("src/gameTest/resources/data/roundwood/structure/archways_v0133.nbt"))) {
            fixture = NbtIo.readCompressed(input);
        }
        var palette = fixture.getList("palette", Tag.TAG_COMPOUND);
        var cells = fixture.getList("blocks", Tag.TAG_COMPOUND);
        Set<String> parts = new HashSet<>();
        int anchors = 0;
        for (Tag tag : cells) {
            CompoundTag cell = (CompoundTag) tag;
            var state = palette.getCompound(cell.getInt("state"));
            String id = state.getString("Name");
            if (id.equals("minecraft:stone")) continue;
            assertTrue(Set.of("roundwood:archway", "roundwood:keyhole_archway").contains(id));
            var pos = cell.getList("pos", Tag.TAG_INT);
            var properties = state.getCompound("Properties");
            String part = properties.getString("part");
            assertTrue(Set.of("lower_left", "upper_left", "lower_right", "upper_right").contains(part));
            assertEquals("north", properties.getString("facing"));
            assertEquals(part.startsWith("lower") ? 1 : 2, pos.getInt(1));
            assertEquals(1, pos.getInt(2));
            int originX = id.equals("roundwood:archway") ? 1 : 5;
            assertEquals(originX + (part.endsWith("right") ? 1 : 0), pos.getInt(0));
            assertTrue(parts.add(id + "/" + part), "duplicate historical part");
            assertEquals(part.equals("lower_left"), cell.contains("nbt"));
            if (!cell.contains("nbt")) continue;
            anchors++;
            var entity = cell.getCompound("nbt");
            assertEquals("roundwood:archway", entity.getString("id"));
            var body = entity.getCompound("RoundwoodAppearance");
            assertEquals(1, body.getInt("Version"));
            if (id.equals("roundwood:archway")) {
                assertEquals("wood", body.getString("Kind"));
                assertEquals("minecraft:spruce_planks", body.getString("Material"));
                var trims = entity.getCompound("ArchwayTrim");
                assertEquals(1, trims.getInt("Version"));
                assertTrue(trims.contains("Front", Tag.TAG_COMPOUND));
                assertTrue(trims.getCompound("Front").isEmpty());
                assertEquals("minecraft:bricks", trims.getCompound("Back").getCompound("RoundwoodAppearance").getString("Material"));
            } else {
                assertEquals("masonry", body.getString("Kind"));
                assertEquals("minecraft:stone_bricks", body.getString("Material"));
            }
        }
        assertEquals(8, parts.size());
        assertEquals(2, anchors);
    }
}
