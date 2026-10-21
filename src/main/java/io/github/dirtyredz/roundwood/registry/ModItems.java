package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimItem;
import io.github.dirtyredz.roundwood.block.window.RoundWindowItem;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowProfile;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * The five advertised items (door, window, the two arch pieces, trim), in creative-tab order. Their appearance travels
 * in the stack; the hidden 0.4.0 alias items are registered by {@code migration/LegacyContent}.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Roundwood.MOD_ID);

    public static final RegistryObject<RoundDoorItem> ROUND_DOOR = ITEMS.register("round_door",
            () -> new RoundDoorItem(ModBlocks.roundDoor(), new Item.Properties()));
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW = ITEMS.register("round_window",
            () -> new RoundWindowItem(ModBlocks.roundWindow(), new Item.Properties()));
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW_ARCH_LEFT = ITEMS.register(WindowProfile.ARCH_LEFT.id(),
            () -> new RoundWindowItem(ModBlocks.archLeft(), new Item.Properties()));
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW_ARCH_RIGHT = ITEMS.register(WindowProfile.ARCH_RIGHT.id(),
            () -> new RoundWindowItem(ModBlocks.archRight(), new Item.Properties()));
    public static final RegistryObject<RoundTrimItem> ROUND_TRIM = ITEMS.register("round_trim",
            () -> new RoundTrimItem(ModBlocks.roundTrim(), new Item.Properties()));

    private ModItems() {
    }

    /** The canonical items in creative order: door, window, left arch, right arch, trim. */
    public static List<Item> all() {
        return List.of(ROUND_DOOR.get(), ROUND_WINDOW.get(), ROUND_WINDOW_ARCH_LEFT.get(), ROUND_WINDOW_ARCH_RIGHT.get(),
                ROUND_TRIM.get());
    }
}
