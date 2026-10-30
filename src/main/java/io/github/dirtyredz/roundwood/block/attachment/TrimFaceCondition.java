package io.github.dirtyredz.roundwood.block.attachment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
    /** The perimeter slot is the default: omitted on write and when absent, so 0.8.0 tables keep their meaning. */
    public static final MapCodec<TrimFaceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(AttachmentFace::values).fieldOf("face").forGetter(TrimFaceCondition::face),
            StringRepresentable.fromEnum(TrimSlot::values).optionalFieldOf("slot", TrimSlot.PERIMETER).forGetter(TrimFaceCondition::slot)
    ).apply(instance, TrimFaceCondition::new));
    public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

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
}
