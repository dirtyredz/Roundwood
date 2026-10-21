package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The 49 block and item ids Roundwood 0.4.0 registered, frozen with the appearance each one had:
 * 15 doors and 15 windows (eleven natural woods, then green, yellow, red and blue paint on oak) and
 * 19 trims (the same fifteen plus four masonry). These ids are kept as hidden aliases so saved
 * worlds and inventories keep their looks; nothing here may change once released.
 */
public final class LegacyAppearances {
    /** One historical id: its path, its family and the appearance it stood for. */
    public record Entry(String id, AppearanceFamily family, Appearance appearance) {
    }

    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
            "cherry", "bamboo", "crimson", "warped"};
    private static final DyeColor[] PAINTS = {DyeColor.GREEN, DyeColor.YELLOW, DyeColor.RED, DyeColor.BLUE};
    private static final String[][] MASONRY = {{"brick", "bricks"}, {"stone_brick", "stone_bricks"},
            {"deepslate_brick", "deepslate_bricks"}, {"mud_brick", "mud_bricks"}};

    public static final List<Entry> DOORS = finishes(AppearanceFamily.DOOR, "_round_door");
    public static final List<Entry> WINDOWS = finishes(AppearanceFamily.WINDOW, "_round_window");
    public static final List<Entry> TRIMS = trims();
    public static final List<Entry> ALL = concat(DOORS, WINDOWS, TRIMS);
    private static final Map<String, Entry> BY_ID = ALL.stream().collect(Collectors.toUnmodifiableMap(Entry::id, Function.identity()));

    private LegacyAppearances() {
    }

    public static Optional<Entry> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    private static List<Entry> finishes(AppearanceFamily family, String suffix) {
        List<Entry> out = new ArrayList<>();
        for (String wood : WOODS) {
            out.add(new Entry(wood + suffix, family, Appearance.wood(planks(wood))));
        }
        for (DyeColor dye : PAINTS) {
            out.add(new Entry(dye.getName() + suffix, family, Appearance.DEFAULT.painted(dye)));
        }
        return List.copyOf(out);
    }

    private static List<Entry> trims() {
        List<Entry> out = new ArrayList<>(finishes(AppearanceFamily.TRIM, "_round_trim"));
        for (String[] masonry : MASONRY) {
            out.add(new Entry(masonry[0] + "_round_trim", AppearanceFamily.TRIM,
                    Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", masonry[1]))));
        }
        return List.copyOf(out);
    }

    private static ResourceLocation planks(String wood) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", wood + "_planks");
    }

    @SafeVarargs
    private static List<Entry> concat(List<Entry>... lists) {
        List<Entry> out = new ArrayList<>();
        for (List<Entry> list : lists) {
            out.addAll(list);
        }
        return List.copyOf(out);
    }
}
