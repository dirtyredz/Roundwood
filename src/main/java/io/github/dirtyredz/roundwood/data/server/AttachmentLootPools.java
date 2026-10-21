package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.loot.TrimFaceCondition;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The loot pools of face-trim attachments (0.8.0), shared by windows and doors: one pool per face,
 * rolled once only while that face carries a trim ({@link TrimFaceCondition}, read from the
 * captured block entity), dropping the host's trim item with that face's look copied from the saved
 * payload ({@code <payload>.<Front|Back>.RoundwoodAppearance}; an oak face has none, so the item
 * stays plain oak). The host's ordinary explosion rule is applied by the caller.
 */
final class AttachmentLootPools {
    private AttachmentLootPools() {
    }

    /**
     * @param payload   the host's trim payload key ({@code WindowTrim}, {@code DoorTrim})
     * @param explosion the host's explosion rule, applied to each pool
     */
    static List<LootPool.Builder> faces(Item trimItem, String payload, UnaryOperator<LootPool.Builder> explosion) {
        List<LootPool.Builder> pools = new ArrayList<>();
        for (AttachmentFace face : AttachmentFace.values()) {
            pools.add(explosion.apply(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(TrimFaceCondition.hasTrim(face))
                    .add(LootItem.lootTableItem(trimItem)
                            .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                    .copy(payload + "." + face.key() + "." + AppearanceNbt.TAG, AppearanceNbt.TAG)))));
        }
        return pools;
    }
}
