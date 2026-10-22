package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.attachment.TrimFaceCondition;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Roundwood's loot condition types (0.8.0): {@code roundwood:trim_face}, which face trims drop. */
public final class ModLootConditions {
    public static final DeferredRegister<LootItemConditionType> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Roundwood.MOD_ID);

    public static final RegistryObject<LootItemConditionType> TRIM_FACE = CONDITIONS.register("trim_face",
            () -> new LootItemConditionType(new TrimFaceCondition.Serializer()));

    private ModLootConditions() {
    }
}
