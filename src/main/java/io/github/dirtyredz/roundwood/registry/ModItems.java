package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.adapter.ThinArchPieceBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.migration.item.RetiredArchwayItem;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.archway.ArchwayItem;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.attachment.item.ArchSillTrimItem;
import io.github.dirtyredz.roundwood.block.attachment.item.StyledAttachmentItem;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.migration.item.RetiredCrossWindowItem;
import io.github.dirtyredz.roundwood.block.attachment.item.UniversalTrimItem;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowItem;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Twenty-two advertised items: four doors, thirteen plain profiles, four arch pieces and three attachments.
 * The window map also retains the thirteen hidden crossed items. Frozen aliases and window_trim
 * remain registered by LegacyContent. The two retired archways are non-placeable migration sources.
 * Looks travel in stack data.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Roundwood.MOD_ID);

    public static final DeferredHolder<Item, RoundDoorItem> ROUND_DOOR = ITEMS.register("round_door",
            () -> new RoundDoorItem(ModBlocks.roundDoor(), new Item.Properties()));
    public static final DeferredHolder<Item, RoundDoorItem> ARCHED_DOOR = ITEMS.register("arched_door",
            () -> new RoundDoorItem(ModBlocks.archedDoor(), new Item.Properties()));
    public static final DeferredHolder<Item, RoundDoorItem> TALL_DOUBLE_ARCHED_DOOR = ITEMS.register("tall_double_arched_door",
            () -> new RoundDoorItem(ModBlocks.tallDoubleArchedDoor(), new Item.Properties()));
    public static final DeferredHolder<Item, RoundDoorItem> DOUBLE_ARCHED_DOOR = ITEMS.register("double_arched_door",
            () -> new RoundDoorItem(ModBlocks.doubleArchedDoor(), new Item.Properties()));
    private static final Map<WindowDesign, DeferredHolder<Item, RoundWindowItem>> WINDOWS = registerWindows();
    public static final DeferredHolder<Item, RoundWindowItem> ROUND_WINDOW = WINDOWS.get(WindowDesign.plain(WindowProfile.ROUND));
    public static final DeferredHolder<Item, RoundWindowItem> ROUND_WINDOW_ARCH_LEFT = WINDOWS.get(WindowDesign.plain(WindowProfile.ARCH_LEFT));
    public static final DeferredHolder<Item, RoundWindowItem> ROUND_WINDOW_ARCH_RIGHT = WINDOWS.get(WindowDesign.plain(WindowProfile.ARCH_RIGHT));
    private static final Map<ArchwayDesign, DeferredHolder<Item, ArchwayItem>> ARCHWAYS = registerArchways();
    private static final Map<ArchPieceDesign, DeferredHolder<Item, ArchPieceItem>> ARCH_PIECES = registerArchPieces(ArchPieceThickness.FULL);
    private static final Map<ArchPieceDesign, DeferredHolder<Item, ArchPieceItem>> THIN_ARCH_PIECES = registerArchPieces(ArchPieceThickness.THIN);
    public static final DeferredHolder<Item, UniversalTrimItem> ROUND_TRIM = ITEMS.register(UniversalTrimItem.ID,
            () -> new UniversalTrimItem(ModBlocks.roundTrim(), new Item.Properties()));
    public static final DeferredHolder<Item, ArchSillTrimItem> ARCH_SILL_TRIM = ITEMS.register(ArchSillTrimItem.ID,
            () -> new ArchSillTrimItem(new Item.Properties()));

    public static final DeferredHolder<Item, StyledAttachmentItem> CROSS_BEAMS = ITEMS.register("cross_beams",
            () -> new StyledAttachmentItem(TrimSlot.CROSS_BEAMS, new Item.Properties()));

    private ModItems() {
    }

    private static Map<WindowDesign, DeferredHolder<Item, RoundWindowItem>> registerWindows() {
        Map<WindowDesign, DeferredHolder<Item, RoundWindowItem>> out = new LinkedHashMap<>();
        for (WindowDesign design : ModBlocks.WindowProducts.all()) {
            out.put(design, ITEMS.register(design.id(), () -> design.pattern() == WindowPattern.CROSS
                    ? new RetiredCrossWindowItem(design, new Item.Properties())
                    : new RoundWindowItem(ModBlocks.window(design), new Item.Properties())));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<ArchwayDesign, DeferredHolder<Item, ArchwayItem>> registerArchways() {
        Map<ArchwayDesign, DeferredHolder<Item, ArchwayItem>> out = new LinkedHashMap<>();
        for (ArchwayDesign design : ArchwayDesign.values()) {
            out.put(design, ITEMS.register(design.id(), () -> new RetiredArchwayItem(ModBlocks.archway(design), new Item.Properties())));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<ArchPieceDesign, DeferredHolder<Item, ArchPieceItem>> registerArchPieces(ArchPieceThickness thickness) {
        Map<ArchPieceDesign, DeferredHolder<Item, ArchPieceItem>> out = new LinkedHashMap<>();
        for (ArchPieceDesign design : ArchPieceDesign.values()) {
            out.put(design, ITEMS.register(thickness.id(design), () -> new ArchPieceItem(ModBlocks.archPiece(design, thickness), new Item.Properties())));
        }
        return Collections.unmodifiableMap(out);
    }
    public static ArchPieceItem archPiece(ArchPieceDesign design) { return ARCH_PIECES.get(design).get(); }
    public static ArchPieceItem archPiece(ArchPieceDesign design, ArchPieceThickness thickness) {
        return (thickness == ArchPieceThickness.FULL ? ARCH_PIECES : THIN_ARCH_PIECES).get(design).get();
    }
    public static List<Item> archPieces() { return java.util.stream.Stream.concat(ARCH_PIECES.values().stream(), THIN_ARCH_PIECES.values().stream()).map(e -> (Item) e.get()).toList(); }

    /** The archway item with {@code design}. */
    public static ArchwayItem archway(ArchwayDesign design) {
        return ARCHWAYS.get(design).get();
    }

    /** The two archway items, ordinary then keyhole. */
    public static List<Item> archways() {
        return ARCHWAYS.values().stream().map(DeferredHolder::get).map(Item.class::cast).toList();
    }

    /** The window item with {@code design}. */
    public static RoundWindowItem window(WindowDesign design) {
        return WINDOWS.get(design).get();
    }

    /** The 13 plain window items, in creative order. */
    public static List<Item> windows() {
        return WINDOWS.entrySet().stream().filter(e -> e.getKey().pattern() == WindowPattern.PLAIN).map(e -> (Item) e.getValue().get()).toList();
    }

    /** The advertised items: two doors, 13 windows, four arch pieces and three attachments. */
    public static List<Item> all() {
        List<Item> out = new ArrayList<>();
        out.add(ROUND_DOOR.get());
        out.add(ARCHED_DOOR.get());
        out.add(DOUBLE_ARCHED_DOOR.get());
        out.add(TALL_DOUBLE_ARCHED_DOOR.get());
        out.addAll(windows());
        out.addAll(archPieces());
        out.add(ROUND_TRIM.get());
        out.add(ARCH_SILL_TRIM.get());
        out.add(CROSS_BEAMS.get());
        return List.copyOf(out);
    }
}
