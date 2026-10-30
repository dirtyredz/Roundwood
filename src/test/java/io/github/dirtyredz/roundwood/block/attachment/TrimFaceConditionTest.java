package io.github.dirtyredz.roundwood.block.attachment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code roundwood:trim_face} JSON (0.9.0 slot selection) through its 1.21 MapCodec: a 0.8.0 table without a
 * slot reads as the perimeter, the perimeter is written without one (so 0.8.0 tables are unchanged), other slots
 * round-trip, and unknown names are refused.
 */
class TrimFaceConditionTest {
    private static JsonObject json(String face, String slot) {
        JsonObject json = new JsonObject();
        json.addProperty("face", face);
        if (slot != null) {
            json.addProperty("slot", slot);
        }
        return json;
    }

    private static TrimFaceCondition decode(JsonElement json) {
        return TrimFaceCondition.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private static JsonElement encode(TrimFaceCondition condition) {
        return TrimFaceCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, condition).getOrThrow();
    }

    @Test
    void anOmittedSlotIsThePerimeter() {
        assertEquals(new TrimFaceCondition(AttachmentFace.BACK, TrimSlot.PERIMETER), decode(json("back", null)));
    }

    @Test
    void everySlotAndFaceRoundTripsAndThePerimeterIsWrittenAsBefore() {
        for (TrimSlot slot : TrimSlot.values()) {
            for (AttachmentFace face : AttachmentFace.values()) {
                JsonObject json = encode(new TrimFaceCondition(face, slot)).getAsJsonObject();
                assertEquals(slot != TrimSlot.PERIMETER, json.has("slot"), slot + " " + face);
                assertEquals(new TrimFaceCondition(face, slot), decode(json));
            }
        }
        JsonObject perimeter = encode(new TrimFaceCondition(AttachmentFace.FRONT, TrimSlot.PERIMETER)).getAsJsonObject();
        assertEquals(json("front", null), perimeter);
        assertFalse(perimeter.has("slot"));
    }

    @Test
    void unknownNamesAreRefused() {
        assertTrue(TrimFaceCondition.CODEC.codec().parse(JsonOps.INSTANCE, json("side", null)).isError());
        assertTrue(TrimFaceCondition.CODEC.codec().parse(JsonOps.INSTANCE, json("front", "lintel")).isError());
    }
}
