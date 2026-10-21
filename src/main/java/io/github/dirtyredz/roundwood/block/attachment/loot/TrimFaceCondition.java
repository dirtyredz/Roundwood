package io.github.dirtyredz.roundwood.block.attachment.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimAttachmentSource;
import io.github.dirtyredz.roundwood.registry.ModLootConditions;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * Loot condition {@code roundwood:trim_face} (0.8.0): true when the captured block entity carries a
 * trim on {@link #face}. Reads only the loot context's block entity (a window's own, a door's anchor
 * supplied by the door), never the world, so it works the same for a player's break, an explosion
 * or support loss. JSON: {@code {"condition": "roundwood:trim_face", "face": "front"}}.
 */
public record TrimFaceCondition(AttachmentFace face) implements LootItemCondition {
    @Override
    public LootItemConditionType getType() {
        return ModLootConditions.TRIM_FACE.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    public boolean test(LootContext context) {
        BlockEntity entity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        return entity instanceof TrimAttachmentSource source && source.trims().has(face);
    }

    public static LootItemCondition.Builder hasTrim(AttachmentFace face) {
        return () -> new TrimFaceCondition(face);
    }

    public static final class Serializer implements net.minecraft.world.level.storage.loot.Serializer<TrimFaceCondition> {
        @Override
        public void serialize(JsonObject json, TrimFaceCondition condition, JsonSerializationContext context) {
            json.addProperty("face", condition.face().getSerializedName());
        }

        @Override
        public TrimFaceCondition deserialize(JsonObject json, JsonDeserializationContext context) {
            String name = GsonHelper.getAsString(json, "face");
            for (AttachmentFace face : AttachmentFace.values()) {
                if (face.getSerializedName().equals(name)) {
                    return new TrimFaceCondition(face);
                }
            }
            throw new JsonParseException("unknown trim face " + name);
        }
    }
}
