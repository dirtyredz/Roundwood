package io.github.dirtyredz.roundwood.block.attachment;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * Loot condition {@code roundwood:trim_face} (0.8.0): true when the captured block entity carries a
 * trim on {@link #face} in {@link #slot} (0.9.0). Reads only the loot context's block entity (a
 * window's own, a door's anchor supplied by the door), never the world, so it works the same for a
 * player's break, an explosion or support loss. JSON:
 * {@code {"condition": "roundwood:trim_face", "face": "front"}}, plus {@code "slot": "sill"} for a
 * slot other than the perimeter (an omitted slot is the perimeter, so 0.8.0 tables keep their meaning).
 */
public record TrimFaceCondition(AttachmentFace face, TrimSlot slot) implements LootItemCondition {
    public static final LootItemConditionType TYPE = new LootItemConditionType(new Serializer());

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    public boolean test(LootContext context) {
        BlockEntity entity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        return entity instanceof TrimAttachmentSource source && source.trims(slot).has(face);
    }

    public static LootItemCondition.Builder hasTrim(TrimSlot slot, AttachmentFace face) {
        return () -> new TrimFaceCondition(face, slot);
    }

    public static final class Serializer implements net.minecraft.world.level.storage.loot.Serializer<TrimFaceCondition> {
        @Override
        public void serialize(JsonObject json, TrimFaceCondition condition, JsonSerializationContext context) {
            json.addProperty("face", condition.face().getSerializedName());
            if (condition.slot() != TrimSlot.PERIMETER) {
                json.addProperty("slot", condition.slot().getSerializedName());
            }
        }

        @Override
        public TrimFaceCondition deserialize(JsonObject json, JsonDeserializationContext context) {
            AttachmentFace face = named(AttachmentFace.values(), GsonHelper.getAsString(json, "face"), "trim face");
            TrimSlot slot = json.has("slot")
                    ? named(TrimSlot.values(), GsonHelper.getAsString(json, "slot"), "trim slot") : TrimSlot.PERIMETER;
            return new TrimFaceCondition(face, slot);
        }

        private static <T extends StringRepresentable> T named(T[] values, String name, String what) {
            for (T value : values) {
                if (value.getSerializedName().equals(name)) {
                    return value;
                }
            }
            throw new JsonParseException("unknown " + what + " " + name);
        }
    }
}
