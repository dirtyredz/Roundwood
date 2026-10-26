package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.migration.LegacyAppearances;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * English names: the twelve canonical blocks (the round trim block and item read "Door Trim" since 0.8.0)
 * and the window trim, the hidden 0.4.0 aliases under their old names (old
 * stacks in chests still read "Spruce Round Door"), the creative tab, the appearance tooltip lines,
 * the restyling hints and the recipe-viewer preview note.
 */
public class RoundwoodLanguageProvider extends LanguageProvider {
    public RoundwoodLanguageProvider(PackOutput output) {
        super(output, Roundwood.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + Roundwood.MOD_ID, "Roundwood");
        add(ModBlocks.roundDoor(), "Round Door");
        add(ModBlocks.archedDoor(), "Arched Door");
        add(ModBlocks.doubleArchedDoor(), "Double Arched Door");
        add(ModBlocks.tallDoubleArchedDoor(), "Tall Double Arched Door");
        for (WindowDesign design : WindowDesign.all()) {
            add(ModBlocks.window(design), windowName(design));
        }
        add(ModBlocks.archway(ArchwayDesign.ORDINARY), "Archway");
        add(ModBlocks.archway(ArchwayDesign.KEYHOLE), "Keyhole Archway");
        add(ModBlocks.archPiece(io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign.TOP_1X1), "Arch Top 1x1");
        add(ModBlocks.archPiece(io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign.TOP_1X2), "Arch Top 1x2");
        add(ModBlocks.archPiece(io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign.TOP_2X3), "Arch Top 2x3");
        add(ModBlocks.archPiece(io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign.KEYHOLE_SHOULDER), "Keyhole Shoulder");
        add("tooltip.roundwood.arch_piece", "Size is height x width; landing cell is the lower-left anchor; all cells must be dry and clear");
        add("tooltip.roundwood.keyhole_shoulder", "Aim at the left or right half to choose the shoulder; midpoint chooses left");
        add("tooltip.roundwood.retired_archway", "Retired archway: converts to modular arch pieces");
        add(ModBlocks.roundTrim(), "Universal Trim");
        add(ModItems.ARCH_SILL_TRIM.get(), "Arch Sill");
        add(ModItems.CROSS_BEAMS.get(), "Cross Beams");
        add("tooltip.roundwood.cross_beams", "Fits across one window pane; aim at a beam to restyle it or remove it with shears");
        add(LegacyContent.windowTrim(), "Window Trim");
        for (LegacyAppearances.Entry entry : LegacyAppearances.ALL) {
            add("block." + Roundwood.MOD_ID + "." + entry.id(), title(entry.id()));
        }
        add("tooltip.roundwood.wood", "Wood: %s");
        add("tooltip.roundwood.masonry", "Masonry: %s");
        add("tooltip.roundwood.paint", "Paint: %s");
        add("tooltip.roundwood.unavailable", "Material %s is not installed (shown as oak)");
        add("tooltip.roundwood.restyle", "Restyle placed: planks or dye (doors: sneak + planks)");
        add("tooltip.roundwood.universal_trim",
                "Trims the clicked face of a window, door, arch piece or retired archway; restyle with planks, masonry or dye, remove with shears");
        add("tooltip.roundwood.glass", "Glass: %s");
        add("message.roundwood.aim_at_leaf", "Aim at the door leaf to restyle or paint it");
        add("message.roundwood.masonry_unpaintable", "Masonry trims cannot be painted");
        add("message.roundwood.masonry_trim_only", "Aim at a trim or cross beam to apply masonry");
        add("tooltip.roundwood.arch_sill_trim",
                "Fits under the clicked face of an upper window arch; restyle with planks, masonry or dye, remove with shears");
        add("message.roundwood.beams_window_only", "Cross Beams fit only a window pane");
        add("message.roundwood.sill_upper_arch_only", "An arch sill fits only an upper window arch");
    }

    /** "Round Window Arch with Cross Beams, Left", "Round Window Corner, Bottom Right" and so on. */
    static String windowName(WindowDesign design) {
        String beams = design.pattern() == WindowPattern.CROSS ? " with Cross Beams" : "";
        return switch (design.profile()) {
            case ROUND -> "Round Window" + beams;
            case ARCH_LEFT -> "Round Window Arch" + beams + ", Left";
            case ARCH_RIGHT -> "Round Window Arch" + beams + ", Right";
            case BOTTOM_LEFT -> "Round Window Corner" + beams + ", Bottom Left";
            case BOTTOM_RIGHT -> "Round Window Corner" + beams + ", Bottom Right";
            case ARCH_BOTTOM_LEFT -> "Round Window Arch" + beams + ", Bottom Left";
            case ARCH_BOTTOM_RIGHT -> "Round Window Arch" + beams + ", Bottom Right";
            case TOP_LEFT -> "Round Window Corner" + beams + ", Top Left";
            case TOP_RIGHT -> "Round Window Corner" + beams + ", Top Right";
            case EDGE_TOP -> "Round Window Edge" + beams + ", Top";
            case EDGE_LEFT -> "Round Window Edge" + beams + ", Left";
            case EDGE_BOTTOM -> "Round Window Edge" + beams + ", Bottom";
            case EDGE_RIGHT -> "Round Window Edge" + beams + ", Right";
        };
    }

    /** {@code dark_oak_round_door} becomes "Dark Oak Round Door". */
    private static String title(String id) {
        return Arrays.stream(id.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
