package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimNbt;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Every part drops its family's canonical item (explosion rules apply); the block puts the broken
 * object's appearance on it ({@code appearance/item/AppearanceDrops}). A door or trim still yields one
 * item: only the part whose removal started the teardown is evaluated, partners are cleared with
 * drops suppressed. A hidden 0.4.0 alias that is broken before converting drops the canonical item
 * in its historical look.
 *
 * <p>Windows (canonical and alias) have a second pool, only while {@code trimmed=true}: one window
 * trim carrying the trim's own look, copied from the block entity's nested {@code WindowTrim} tag,
 * under the same explosion rule as the pane. So a trimmed window drops its pane and its trim as two
 * items, and nothing ever refunds a trim outside loot.
 */
public class RoundwoodBlockLoot extends BlockLootSubProvider {
    public RoundwoodBlockLoot() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.roundDoor());
        dropSelf(ModBlocks.roundTrim());
        ModBlocks.windows().forEach(window -> add(window, window(window, window.asItem())));
        LegacyContent.doors().forEach(block -> dropOther(block, ModItems.ROUND_DOOR.get()));
        LegacyContent.windows().forEach(block -> add(block, window(block, ModItems.ROUND_WINDOW.get())));
        LegacyContent.trims().forEach(block -> dropOther(block, ModItems.ROUND_TRIM.get()));
    }

    /** The pane's item, plus a window trim in its look while the window is trimmed. */
    private LootTable.Builder window(Block block, Item pane) {
        return createSingleItemTable(pane).withPool(applyExplosionCondition(ModItems.WINDOW_TRIM.get(), LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(WindowPaneBlock.TRIMMED, true)))
                .add(LootItem.lootTableItem(ModItems.WINDOW_TRIM.get())
                        .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                .copy(WindowTrimNbt.TAG + "." + AppearanceNbt.TAG, AppearanceNbt.TAG)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(ModBlocks.canonical().stream(), LegacyContent.blocks().stream()).toList();
    }
}
