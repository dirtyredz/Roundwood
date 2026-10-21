package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Five recipes over any planks (mixed types allowed): eight around a gold nugget make a door, four
 * around a glass pane a window, four in the grid's corners (no vanilla recipe uses that shape) a
 * trim (the 0.4.0 shapes and yields), and four planks with a glass pane in two different shapes make
 * the left and right arch pieces (deliberately not mirror images, so neither can match the other's
 * grid). The result is always the default oak object; its look is chosen in
 * the world (planks and dye), so there are no per-wood, painting or masonry recipes.
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
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.ROUND_WINDOW.get())
                .pattern(" P ").pattern("PGP").pattern(" P ")
                .define('P', ItemTags.PLANKS)
                .define('G', Items.GLASS_PANE)
                .group(Roundwood.MOD_ID + ":round_window")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.ROUND_WINDOW_ARCH_LEFT.get())
                .pattern("PP").pattern("PG").pattern("P ")
                .define('P', ItemTags.PLANKS)
                .define('G', Items.GLASS_PANE)
                .group(Roundwood.MOD_ID + ":round_window_arch")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.ROUND_WINDOW_ARCH_RIGHT.get())
                .pattern("PPP").pattern(" G ").pattern(" P ")
                .define('P', ItemTags.PLANKS)
                .define('G', Items.GLASS_PANE)
                .group(Roundwood.MOD_ID + ":round_window_arch")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.ROUND_TRIM.get())
                .pattern("P P").pattern("   ").pattern("P P")
                .define('P', ItemTags.PLANKS)
                .group(Roundwood.MOD_ID + ":round_trim")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(writer);
    }
}
