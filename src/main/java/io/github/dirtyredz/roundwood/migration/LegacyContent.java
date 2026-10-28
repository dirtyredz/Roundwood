package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility;
import io.github.dirtyredz.roundwood.migration.retired.ArchwayCompatibility;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.item.UniversalTrimItem;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowItem;
import io.github.dirtyredz.roundwood.migration.block.LegacyDoorBlock;
import io.github.dirtyredz.roundwood.migration.block.LegacyTrimBlock;
import io.github.dirtyredz.roundwood.migration.block.LegacyWindowBlock;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Registers the hidden compatibility content: the 49 0.4.0 aliases ({@link LegacyAppearances}), a
 * deprecated block per old id (so saved chunks keep their states and block entities until converted)
 * and an item per old id (so stacks in chests and inventories keep their look; they place the
 * canonical block, and the trim aliases attach as the Universal Trim); and since 0.9.0 the 0.8.0
 * {@code window_trim} item, which keeps its id, name and NBT and behaves as the Universal Trim. None
 * is in a creative tab or a recipe, and recipe viewers hide them all ({@link #items}).
 */
@SuppressWarnings("deprecation")
public final class LegacyContent {
    private static final List<RegistryObject<LegacyDoorBlock>> DOORS = new ArrayList<>();
    private static final List<RegistryObject<LegacyWindowBlock>> WINDOWS = new ArrayList<>();
    private static final List<RegistryObject<LegacyTrimBlock>> TRIMS = new ArrayList<>();
    private static final List<RegistryObject<Item>> ITEMS = new ArrayList<>();
    /** The 0.8.0 window trim's registry path (an item only). */
    public static final String WINDOW_TRIM_ID = "window_trim";
    private static RegistryObject<Item> windowTrim;

    private LegacyContent() {
    }

    /** Called once from the bootstrap, before the registers fire. */
    public static void register(DeferredRegister<Block> blocks, DeferredRegister<Item> items) {
        for (LegacyAppearances.Entry entry : LegacyAppearances.DOORS) {
            RegistryObject<LegacyDoorBlock> block = blocks.register(entry.id(), () -> new LegacyDoorBlock(
                    ModBlocks.doorProperties(), entry, ModBlocks::roundDoor, ModBlockEntities::roundDoor));
            DOORS.add(block);
            ITEMS.add(items.register(entry.id(), () -> new RoundDoorItem(ModBlocks.roundDoor(), new Item.Properties(),
                    entry.appearance(), entry.id())));
        }
        for (LegacyAppearances.Entry entry : LegacyAppearances.WINDOWS) {
            WINDOWS.add(blocks.register(entry.id(), () -> new LegacyWindowBlock(
                    ModBlocks.windowProperties(), entry, ModBlocks::roundWindow, ModBlockEntities::roundWindow)));
            ITEMS.add(items.register(entry.id(), () -> new RoundWindowItem(ModBlocks.roundWindow(), new Item.Properties(),
                    entry.appearance(), entry.id())));
        }
        for (LegacyAppearances.Entry entry : LegacyAppearances.TRIMS) {
            TRIMS.add(blocks.register(entry.id(), () -> new LegacyTrimBlock(
                    ModBlocks.trimProperties(), entry, ModBlocks::roundTrim, ModBlockEntities::roundTrim)));
            ITEMS.add(items.register(entry.id(), () -> new UniversalTrimItem(ModBlocks.roundTrim(), new Item.Properties(),
                    entry.appearance(), entry.id())));
        }
        windowTrim = items.register(WINDOW_TRIM_ID, () -> new UniversalTrimItem(ModBlocks.roundTrim(), new Item.Properties(),
                Appearance.DEFAULT, WINDOW_TRIM_ID));
    }

    /** The hidden 0.8.0 window trim (behaves as the Universal Trim). */
    public static Item windowTrim() {
        return windowTrim.get();
    }

    public static List<LegacyDoorBlock> doors() {
        return DOORS.stream().map(RegistryObject::get).toList();
    }

    public static List<LegacyWindowBlock> windows() {
        return WINDOWS.stream().map(RegistryObject::get).toList();
    }

    public static List<LegacyTrimBlock> trims() {
        return TRIMS.stream().map(RegistryObject::get).toList();
    }

    /** Every alias block: doors, windows, trims, each in 0.4.0 order. */
    public static List<Block> blocks() {
        return Stream.of(doors(), windows(), trims(), CrossWindowCompatibility.blocks(), ArchwayCompatibility.blocks()).flatMap(List::stream).map(Block.class::cast).toList();
    }

    /** The 49 0.4.0 alias items, in the same order as {@link #blocks()}'s families. */
    public static List<Item> aliasItems() {
        return ITEMS.stream().map(RegistryObject::get).toList();
    }

    /** Every hidden compatibility item: the 49 aliases, then the window trim. */
    public static List<Item> items() {
        return Stream.of(aliasItems(), List.of(windowTrim()), CrossWindowCompatibility.items(), ArchwayCompatibility.items()).flatMap(List::stream).toList();
    }
}
