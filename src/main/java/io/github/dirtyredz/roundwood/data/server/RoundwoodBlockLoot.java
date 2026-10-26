package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimNbt;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimNbt;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimNbt;
import io.github.dirtyredz.roundwood.block.window.trim.WindowSillNbt;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimNbt;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Set;
import java.util.stream.Stream;
import io.github.dirtyredz.roundwood.block.window.trim.WindowBeamNbt;
import io.github.dirtyredz.roundwood.migration.CrossWindowCompatibility;

/**
 * Every part drops its family's canonical item (explosion rules apply); the block puts the broken
 * object's appearance on it ({@code appearance/item/AppearanceDrops}). A door or frame still yields
 * one item: only the part whose removal started the teardown is evaluated, partners are cleared with
 * drops suppressed. A hidden 0.4.0 alias that is broken before converting drops the canonical item
 * in its historical look.
 *
 * <p>Windows and doors (canonical and alias) add one pool per face trim (0.8.0,
 * {@link AttachmentLootPools}): a styled Universal Trim ({@code round_trim}, 0.9.0, for windows too)
 * for each face that carries one, under the host's explosion rule. A door evaluates its loot with its anchor's entity
 * supplied, from whichever quarter broke. So a window with two trims drops three items, and nothing
 * ever refunds a trim outside loot except shears. The upper arches (0.10.0) add two more pools, one
 * per face's arch sill ({@code WindowSillTrim}), so an arch with both trims and both sills drops five.
 * An archway (0.12.0) drops itself plus a Universal Trim per trimmed face ({@code ArchwayTrim}),
 * evaluated with its anchor's entity from whichever cell broke.
 */
public class RoundwoodBlockLoot extends BlockLootSubProvider {
    public RoundwoodBlockLoot() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        add(ModBlocks.roundDoor(), door(ModItems.ROUND_DOOR.get()));
        add(ModBlocks.archedDoor(), door(ModItems.ARCHED_DOOR.get()));
        dropSelf(ModBlocks.roundTrim());
        ModBlocks.windows().forEach(window -> add(window, window(window.asItem(), window.profile().takesSill())));
        ModBlocks.archways().forEach(archway -> add(archway,
                withFaces(createSingleItemTable(archway.asItem()), TrimSlot.PERIMETER, ArchwayTrimNbt.TAG)));
        ModBlocks.archPieces().forEach(piece -> add(piece,
                withFaces(createSingleItemTable(piece.asItem()), TrimSlot.PERIMETER, ArchPieceTrimNbt.TAG)));
        CrossWindowCompatibility.blocks().forEach(block -> dropOther(block, ModItems.window(block.design())));
        LegacyContent.doors().forEach(block -> add(block, door(ModItems.ROUND_DOOR.get())));
        LegacyContent.windows().forEach(block -> add(block, window(ModItems.ROUND_WINDOW.get(), false)));
        LegacyContent.trims().forEach(block -> dropOther(block, ModItems.ROUND_TRIM.get()));
    }

    /** The window's item, plus a Universal Trim per trimmed face and, on an upper arch, an Arch Sill per silled face. */
    private LootTable.Builder window(Item pane, boolean sills) {
        LootTable.Builder table = withFaces(createSingleItemTable(pane), TrimSlot.PERIMETER, WindowTrimNbt.TAG);
        if (sills) table = withFaces(table, TrimSlot.SILL, WindowSillNbt.TAG);
        return withFaces(table, TrimSlot.CROSS_BEAMS, WindowBeamNbt.TAG);
    }

    /** The door's item, plus a Universal Trim per trimmed face. */
    private LootTable.Builder door(Item door) {
        return withFaces(createSingleItemTable(door), TrimSlot.PERIMETER, DoorTrimNbt.TAG);
    }

    private LootTable.Builder withFaces(LootTable.Builder table, TrimSlot slot, String payload) {
        Item item = slot.item();
        AttachmentLootPools.faces(slot, item, payload, pool -> applyExplosionCondition(item, pool)).forEach(table::withPool);
        return table;
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.of(ModBlocks.canonical().stream(), ModBlocks.archPieces().stream(), LegacyContent.blocks().stream()).flatMap(stream -> stream).map(block -> (Block) block).toList();
    }
}
