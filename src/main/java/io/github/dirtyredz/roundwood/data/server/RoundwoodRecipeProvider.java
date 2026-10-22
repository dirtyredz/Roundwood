package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Eleven recipes over any planks (mixed types allowed), each straight from raw materials, so no
 * recipe ever consumes a styled window and loses its look:
 * <ul>
 *     <li>door: eight planks around a gold nugget; Universal Trim: four planks in the corners (the
 *     0.4.0 shapes);</li>
 *     <li>round window: four planks around a glass pane; with cross beams, sticks in the corners;</li>
 *     <li>left and right arch: four planks and a glass pane in two shapes that are not mirror images
 *     (0.6.0); with cross beams, two sticks take planks' places;</li>
 *     <li>bottom left and right corner (0.7.0): an L of planks under a glass pane, the right one a
 *     T; with cross beams, two sticks beside the pane.</li>
 * </ul>
 * The 0.8.0 window trim recipe is gone (0.9.0): the Universal Trim fits windows too.
 * No two shapes match each other's grid, mirrored or moved (the unit and GameTests check). The result
 * is always the default oak object; its look is chosen in the world (planks, dye and, for trims,
 * masonry), so there are no per-wood, painting or masonry recipes.
 */
public class RoundwoodRecipeProvider extends RecipeProvider {
    public RoundwoodRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.ROUND_DOOR.get())
                .pattern("PPP").pattern("PNP").pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('N', Items.GOLD_NUGGET)
                .group(Roundwood.MOD_ID + ":round_door")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(writer);
        window(writer, WindowProfile.ROUND, WindowPattern.PLAIN, "round_window", " P ", "PGP", " P ");
        window(writer, WindowProfile.ARCH_LEFT, WindowPattern.PLAIN, "round_window_arch", "PP", "PG", "P ");
        window(writer, WindowProfile.ARCH_RIGHT, WindowPattern.PLAIN, "round_window_arch", "PPP", " G ", " P ");
        window(writer, WindowProfile.BOTTOM_LEFT, WindowPattern.PLAIN, "round_window_bottom", "P ", "PG", "PP");
        window(writer, WindowProfile.BOTTOM_RIGHT, WindowPattern.PLAIN, "round_window_bottom", " P ", " G ", "PPP");
        window(writer, WindowProfile.ROUND, WindowPattern.CROSS, "round_window_cross", "SPS", "PGP", "SPS");
        window(writer, WindowProfile.ARCH_LEFT, WindowPattern.CROSS, "round_window_arch_cross", "PPS", "PGS", "P  ");
        window(writer, WindowProfile.ARCH_RIGHT, WindowPattern.CROSS, "round_window_arch_cross", "PPP", "SGS", " P ");
        window(writer, WindowProfile.BOTTOM_LEFT, WindowPattern.CROSS, "round_window_bottom_cross", "P  ", "PGS", "PPS");
        window(writer, WindowProfile.BOTTOM_RIGHT, WindowPattern.CROSS, "round_window_bottom_cross", " P ", "SGS", "PPP");
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.ROUND_TRIM.get())
                .pattern("P P").pattern("   ").pattern("P P")
                .define('P', ItemTags.PLANKS)
                .group(Roundwood.MOD_ID + ":round_trim")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(writer);
    }

    private static void window(Consumer<FinishedRecipe> writer, WindowProfile profile, WindowPattern pattern, String group,
                               String... rows) {
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
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(writer);
    }
}
