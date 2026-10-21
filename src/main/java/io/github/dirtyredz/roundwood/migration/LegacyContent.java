package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.block.door.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimItem;
import io.github.dirtyredz.roundwood.block.window.RoundWindowItem;
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
 * Registers the 49 hidden 0.4.0 aliases ({@link LegacyAppearances}): a deprecated block per old id
 * (so saved chunks keep their states and block entities until converted) and an item per old id
 * (so stacks in chests and inventories keep their look; they place the canonical block). Aliases are
 * in no creative tab, have no recipes and are hidden from recipe viewers.
 */
@SuppressWarnings("deprecation")
public final class LegacyContent {
    private static final List<RegistryObject<LegacyDoorBlock>> DOORS = new ArrayList<>();
    private static final List<RegistryObject<LegacyWindowBlock>> WINDOWS = new ArrayList<>();
    private static final List<RegistryObject<LegacyTrimBlock>> TRIMS = new ArrayList<>();
    private static final List<RegistryObject<Item>> ITEMS = new ArrayList<>();

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
            ITEMS.add(items.register(entry.id(), () -> new DoorTrimItem(ModBlocks.roundTrim(), new Item.Properties(),
                    entry.appearance(), entry.id())));
        }
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
        return Stream.of(doors(), windows(), trims()).flatMap(List::stream).map(Block.class::cast).toList();
    }

    /** Every alias item, in the same order as {@link #blocks()}'s families. */
    public static List<Item> items() {
        return ITEMS.stream().map(RegistryObject::get).toList();
    }
}
