package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Natural finishes are crafted from their planks; painted finishes are dyed from oak or from another
 * painted finish. The paint ingredient lists every other paintable item (as vanilla wool dyeing
 * does), so no recipe turns an item into itself.
 */
public class RoundwoodRecipeProvider extends RecipeProvider {
    public RoundwoodRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        for (Finish finish : FinishCatalog.NATURAL) {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.door(finish))
                    .pattern("PPP").pattern("PNP").pattern("PPP")
                    .define('P', finish.planks())
                    .define('N', Items.GOLD_NUGGET)
                    .group(Roundwood.MOD_ID + ":round_door")
                    .unlockedBy("has_planks", has(finish.planks()))
                    .save(writer);
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.window(finish))
                    .pattern(" P ").pattern("PGP").pattern(" P ")
                    .define('P', finish.planks())
                    .define('G', Items.GLASS_PANE)
                    .group(Roundwood.MOD_ID + ":round_window")
                    .unlockedBy("has_planks", has(finish.planks()))
                    .save(writer);
        }
        for (Finish finish : FinishCatalog.PAINTED) {
            paint(writer, finish, RecipeCategory.REDSTONE, ModBlocks::door, "painted_round_door");
            paint(writer, finish, RecipeCategory.DECORATIONS, ModBlocks::window, "painted_round_window");
        }
    }

    private static void paint(Consumer<FinishedRecipe> writer, Finish finish, RecipeCategory category,
                              Function<Finish, ? extends ItemLike> block, String group) {
        List<ItemLike> sources = new ArrayList<>();
        sources.add(block.apply(FinishCatalog.PAINT_BASE));
        for (Finish other : FinishCatalog.PAINTED) {
            if (other != finish) {
                sources.add(block.apply(other));
            }
        }
        DyeItem dye = DyeItem.byColor(finish.paint().orElseThrow());
        ItemLike result = block.apply(finish);
        ShapelessRecipeBuilder.shapeless(category, result)
                .requires(Ingredient.of(sources.toArray(ItemLike[]::new)))
                .requires(dye)
                .group(Roundwood.MOD_ID + ":" + group)
                .unlockedBy("has_dye", has(dye))
                .save(writer, ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, getItemName(result) + "_from_painting"));
    }
}
