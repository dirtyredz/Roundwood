package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimItem;
import io.github.dirtyredz.roundwood.block.window.RoundWindowItem;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The thirteen advertised items, in creative-tab order: door, the ten windows (each plain piece
 * followed by its crossed variant), the window trim and the door trim ({@code round_trim}, 0.8.0: an
 * attachment item that keeps its block association with the compatibility frame).
 * Their appearance travels in the stack; the hidden 0.4.0 alias items are registered by
 * {@code migration/LegacyContent}.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Roundwood.MOD_ID);

    public static final RegistryObject<RoundDoorItem> ROUND_DOOR = ITEMS.register("round_door",
            () -> new RoundDoorItem(ModBlocks.roundDoor(), new Item.Properties()));
    private static final Map<WindowDesign, RegistryObject<RoundWindowItem>> WINDOWS = registerWindows();
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW = WINDOWS.get(WindowDesign.plain(WindowProfile.ROUND));
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW_ARCH_LEFT = WINDOWS.get(WindowDesign.plain(WindowProfile.ARCH_LEFT));
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW_ARCH_RIGHT = WINDOWS.get(WindowDesign.plain(WindowProfile.ARCH_RIGHT));
    public static final RegistryObject<WindowTrimItem> WINDOW_TRIM = ITEMS.register(WindowTrimItem.ID,
            () -> new WindowTrimItem(new Item.Properties()));
    public static final RegistryObject<DoorTrimItem> ROUND_TRIM = ITEMS.register(DoorTrimItem.ID,
            () -> new DoorTrimItem(ModBlocks.roundTrim(), new Item.Properties()));

    private ModItems() {
    }

    private static Map<WindowDesign, RegistryObject<RoundWindowItem>> registerWindows() {
        Map<WindowDesign, RegistryObject<RoundWindowItem>> out = new LinkedHashMap<>();
        for (WindowDesign design : WindowDesign.all()) {
            out.put(design, ITEMS.register(design.id(), () -> new RoundWindowItem(ModBlocks.window(design), new Item.Properties())));
        }
        return Collections.unmodifiableMap(out);
    }

    /** The window item with {@code design}. */
    public static RoundWindowItem window(WindowDesign design) {
        return WINDOWS.get(design).get();
    }

    /** The ten window items, in creative order. */
    public static List<Item> windows() {
        return WINDOWS.values().stream().map(RegistryObject::get).map(Item.class::cast).toList();
    }

    /** The canonical items in creative order: door, the ten windows, window trim, round trim. */
    public static List<Item> all() {
        List<Item> out = new ArrayList<>();
        out.add(ROUND_DOOR.get());
        out.addAll(windows());
        out.add(WINDOW_TRIM.get());
        out.add(ROUND_TRIM.get());
        return List.copyOf(out);
    }
}
