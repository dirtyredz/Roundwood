package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.attachment.item.ArchSillTrimItem;
import io.github.dirtyredz.roundwood.block.attachment.item.UniversalTrimItem;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowItem;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
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
 * The 30 advertised items, in creative-tab order: the round door and (0.11.0) the arched door, the
 * 26 windows (each plain piece followed by
 * its crossed variant), the Universal Trim ({@code round_trim}, 0.9.0: the one perimeter attachment
 * for windows and doors; it keeps its block association with the compatibility frame) and the Arch
 * Sill ({@code arch_sill_trim}, 0.10.0: an item only, the upper arches' sill attachment). Their
 * appearance travels in the stack. The hidden compatibility items (the 0.4.0 aliases and the 0.8.0
 * {@code window_trim}) are registered by {@code migration/LegacyContent}.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Roundwood.MOD_ID);

    public static final RegistryObject<RoundDoorItem> ROUND_DOOR = ITEMS.register("round_door",
            () -> new RoundDoorItem(ModBlocks.roundDoor(), new Item.Properties()));
    public static final RegistryObject<RoundDoorItem> ARCHED_DOOR = ITEMS.register("arched_door",
            () -> new RoundDoorItem(ModBlocks.archedDoor(), new Item.Properties()));
    private static final Map<WindowDesign, RegistryObject<RoundWindowItem>> WINDOWS = registerWindows();
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW = WINDOWS.get(WindowDesign.plain(WindowProfile.ROUND));
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW_ARCH_LEFT = WINDOWS.get(WindowDesign.plain(WindowProfile.ARCH_LEFT));
    public static final RegistryObject<RoundWindowItem> ROUND_WINDOW_ARCH_RIGHT = WINDOWS.get(WindowDesign.plain(WindowProfile.ARCH_RIGHT));
    public static final RegistryObject<UniversalTrimItem> ROUND_TRIM = ITEMS.register(UniversalTrimItem.ID,
            () -> new UniversalTrimItem(ModBlocks.roundTrim(), new Item.Properties()));
    public static final RegistryObject<ArchSillTrimItem> ARCH_SILL_TRIM = ITEMS.register(ArchSillTrimItem.ID,
            () -> new ArchSillTrimItem(new Item.Properties()));

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

    /** The 26 window items, in creative order. */
    public static List<Item> windows() {
        return WINDOWS.values().stream().map(RegistryObject::get).map(Item.class::cast).toList();
    }

    /** The advertised items in creative order: the two doors, the 26 windows, the Universal Trim, the Arch Sill. */
    public static List<Item> all() {
        List<Item> out = new ArrayList<>();
        out.add(ROUND_DOOR.get());
        out.add(ARCHED_DOOR.get());
        out.addAll(windows());
        out.add(ROUND_TRIM.get());
        out.add(ARCH_SILL_TRIM.get());
        return List.copyOf(out);
    }
}
