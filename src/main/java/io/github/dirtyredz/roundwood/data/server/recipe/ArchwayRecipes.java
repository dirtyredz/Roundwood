package io.github.dirtyredz.roundwood.data.server.recipe;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * The archway recipes (0.12.0), straight from planks (P, mixed types allowed) and sticks (S), so no
 * recipe consumes a styled archway; the result is oak, restyled in the world (wood, paint or masonry):
 * <ul>
 *     <li>archway: seven planks standing in an arch, the passage left empty;</li>
 *     <li>keyhole archway: the same arch with a stick in each top corner (its shoulders).</li>
 * </ul>
 * Neither matches another Roundwood grid, mirrored or moved (the unit tests check).
 */
public final class ArchwayRecipes {
    private ArchwayRecipes() {
    }

    public static void build(Consumer<FinishedRecipe> writer, InventoryChangeTrigger.TriggerInstance unlock) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.archway(ArchwayDesign.ORDINARY))
                .pattern("PPP").pattern("P P").pattern("P P")
                .define('P', ItemTags.PLANKS)
                .group(Roundwood.MOD_ID + ":archway")
                .unlockedBy("has_planks", unlock)
                .save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.archway(ArchwayDesign.KEYHOLE))
                .pattern("SPS").pattern("P P").pattern("P P")
                .define('P', ItemTags.PLANKS)
                .define('S', Items.STICK)
                .group(Roundwood.MOD_ID + ":archway")
                .unlockedBy("has_planks", unlock)
                .save(writer);
    }
}
