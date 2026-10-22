package io.github.dirtyredz.roundwood.data.server.recipe;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * The attachment recipes, from planks (mixed types allowed) and sticks:
 * <ul>
 *     <li>Universal Trim: four planks in the corners (the 0.4.0 shape). The 0.8.0 window trim recipe is
 *     gone (0.9.0): the Universal Trim fits windows too.</li>
 *     <li>Arch Sill (0.10.0): a row of three planks on two sticks. Never three planks alone, which
 *     would be a slab of one wood.</li>
 * </ul>
 */
public final class TrimRecipes {
    private TrimRecipes() {
    }

    public static void build(Consumer<FinishedRecipe> writer, InventoryChangeTrigger.TriggerInstance unlock) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.CROSS_BEAMS.get())
                .pattern(" S ").pattern("SSS").pattern(" S ").define('S', Items.STICK)
                .unlockedBy("has_stick", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STICK)).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.ROUND_TRIM.get())
                .pattern("P P").pattern("   ").pattern("P P")
                .define('P', ItemTags.PLANKS)
                .group(Roundwood.MOD_ID + ":round_trim")
                .unlockedBy("has_planks", unlock)
                .save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.ARCH_SILL_TRIM.get())
                .pattern("PPP").pattern("S S")
                .define('P', ItemTags.PLANKS)
                .define('S', Items.STICK)
                .group(Roundwood.MOD_ID + ":arch_sill_trim")
                .unlockedBy("has_planks", unlock)
                .save(writer);
    }
}
