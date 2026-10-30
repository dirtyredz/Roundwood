package io.github.dirtyredz.roundwood.data.server.recipe;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;


/**
 * The 26 window recipes, each straight from planks (P, mixed types allowed), a glass pane (G) and,
 * for a crossed piece, sticks (S), so no recipe ever consumes a styled window and loses its look:
 * <ul>
 *     <li>round window: four planks around a glass pane; with cross beams, sticks in the corners;</li>
 *     <li>left and right arch: four planks and a pane in two shapes that are not mirror images
 *     (0.6.0); with cross beams, two sticks take planks' places;</li>
 *     <li>bottom left and right corner (0.7.0): an L of planks under a pane, the right one a T; with
 *     cross beams, two sticks beside the pane;</li>
 *     <li>lower left and right arch (0.10.0): five and six planks (an upper arch's shape reflected
 *     would be a bottom corner's);</li>
 *     <li>top left and right corner (0.10.0): five and four planks under a full top row (a bottom
 *     corner's reflection would be an upper arch's);</li>
 *     <li>single edges (0.10.0): three planks along the framed side of a pane; left and right differ
 *     by where the pane sits, since shaped recipes also match mirrored.</li>
 * </ul>
 * No two shapes match each other's grid, mirrored or moved (the unit and GameTests check).
 */
public final class WindowRecipes {
    private WindowRecipes() {
    }

    public static void build(RecipeOutput writer, Criterion<InventoryChangeTrigger.TriggerInstance> unlock) {
        net.minecraft.data.recipes.ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS,
                ModItems.window(WindowDesign.plain(WindowProfile.BLANK)))
                .requires(Items.GLASS_PANE)
                .unlockedBy("has_glass_pane", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GLASS_PANE))
                .save(writer);
        window(writer, unlock, WindowProfile.ROUND, WindowPattern.PLAIN, "round_window", " P ", "PGP", " P ");
        window(writer, unlock, WindowProfile.ARCH_LEFT, WindowPattern.PLAIN, "round_window_arch", "PP", "PG", "P ");
        window(writer, unlock, WindowProfile.ARCH_RIGHT, WindowPattern.PLAIN, "round_window_arch", "PPP", " G ", " P ");
        window(writer, unlock, WindowProfile.BOTTOM_LEFT, WindowPattern.PLAIN, "round_window_bottom", "P ", "PG", "PP");
        window(writer, unlock, WindowProfile.BOTTOM_RIGHT, WindowPattern.PLAIN, "round_window_bottom", " P ", " G ", "PPP");
        // 0.10.0
        window(writer, unlock, WindowProfile.ARCH_BOTTOM_LEFT, WindowPattern.PLAIN, "round_window_arch_bottom", "PP", "PG", "PP");
        window(writer, unlock, WindowProfile.ARCH_BOTTOM_RIGHT, WindowPattern.PLAIN, "round_window_arch_bottom", "PPP", " G ", "PPP");
        window(writer, unlock, WindowProfile.TOP_LEFT, WindowPattern.PLAIN, "round_window_top", "PPP", "PG ", "P  ");
        window(writer, unlock, WindowProfile.TOP_RIGHT, WindowPattern.PLAIN, "round_window_top", "PPP", " G ", "  P");
        window(writer, unlock, WindowProfile.EDGE_TOP, WindowPattern.PLAIN, "round_window_edge", "PPP", " G ");
        window(writer, unlock, WindowProfile.EDGE_LEFT, WindowPattern.PLAIN, "round_window_edge", "P ", "PG", "P ");
        window(writer, unlock, WindowProfile.EDGE_BOTTOM, WindowPattern.PLAIN, "round_window_edge", " G ", "PPP");
        window(writer, unlock, WindowProfile.EDGE_RIGHT, WindowPattern.PLAIN, "round_window_edge", "GP", " P", " P");
    }

    private static void window(RecipeOutput writer, Criterion<InventoryChangeTrigger.TriggerInstance> unlock,
                               WindowProfile profile, WindowPattern pattern, String group, String... rows) {
        Item result = ModItems.window(new WindowDesign(profile, pattern));
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, result);
        for (String row : rows) {
            builder.pattern(row);
        }
        String all = String.join("", rows);
        builder.define('P', ItemTags.PLANKS).define('G', Items.GLASS_PANE);
        if (all.indexOf('S') >= 0) {
            builder.define('S', Items.STICK);
        }
        builder.group(Roundwood.MOD_ID + ":" + group)
                .unlockedBy("has_planks", unlock)
                .save(writer);
    }
}
