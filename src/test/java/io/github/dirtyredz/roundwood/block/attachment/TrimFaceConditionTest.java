package io.github.dirtyredz.roundwood.block.attachment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The {@code roundwood:trim_face} JSON (0.9.0 slot selection): a 0.8.0 table without a slot reads as
 * the perimeter, the perimeter is written without one (so 0.8.0 tables are unchanged), other slots
 * round-trip, and unknown names are refused.
 */
class TrimFaceConditionTest {
    private static final TrimFaceCondition.Serializer SERIALIZER = new TrimFaceCondition.Serializer();

    private static JsonObject json(String face, String slot) {
        JsonObject json = new JsonObject();
        json.addProperty("face", face);
        if (slot != null) {
            json.addProperty("slot", slot);
        }
        return json;
    }

    @Test
    void anOmittedSlotIsThePerimeter() {
        assertEquals(new TrimFaceCondition(AttachmentFace.BACK, TrimSlot.PERIMETER), SERIALIZER.deserialize(json("back", null), null));
    }

    @Test
    void everySlotAndFaceRoundTripsAndThePerimeterIsWrittenAsBefore() {
        for (TrimSlot slot : TrimSlot.values()) {
            for (AttachmentFace face : AttachmentFace.values()) {
                JsonObject json = new JsonObject();
                SERIALIZER.serialize(json, new TrimFaceCondition(face, slot), null);
                assertEquals(slot != TrimSlot.PERIMETER, json.has("slot"), slot + " " + face);
                assertEquals(new TrimFaceCondition(face, slot), SERIALIZER.deserialize(json, null));
            }
        }
        JsonObject perimeter = new JsonObject();
        SERIALIZER.serialize(perimeter, new TrimFaceCondition(AttachmentFace.FRONT, TrimSlot.PERIMETER), null);
        assertEquals(json("front", null), perimeter);
        assertFalse(perimeter.has("slot"));
    }

    @Test
    void unknownNamesAreRefused() {
        assertThrows(JsonParseException.class, () -> SERIALIZER.deserialize(json("side", null), null));
        assertThrows(JsonParseException.class, () -> SERIALIZER.deserialize(json("front", "lintel"), null));
    }
}
