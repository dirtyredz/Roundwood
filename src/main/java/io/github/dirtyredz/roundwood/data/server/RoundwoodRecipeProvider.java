package io.github.dirtyredz.roundwood.data.server;

import io.github.dirtyredz.roundwood.data.server.recipe.DoorRecipes;
import io.github.dirtyredz.roundwood.data.server.recipe.TrimRecipes;
import io.github.dirtyredz.roundwood.data.server.recipe.WindowRecipes;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;

import java.util.function.Consumer;

/**
 * Coordinates the 30 recipes (0.10.0: split by family under {@code recipe/}): the two doors
 * ({@link DoorRecipes}), the 26 windows ({@link WindowRecipes}) and the two attachments
 * ({@link TrimRecipes}). Every recipe takes raw materials over any planks (mixed types allowed), so
 * no recipe ever consumes a styled object and loses its look; each is unlocked by holding planks. No
 * two shapes match each other's grid, mirrored or moved (the unit and GameTests check). The result
 * is always the default oak object; its look is chosen in the world (planks, dye and, for trims,
 * masonry), so there are no per-wood, painting or masonry recipes.
 */
public class RoundwoodRecipeProvider extends RecipeProvider {
    public RoundwoodRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        InventoryChangeTrigger.TriggerInstance planks = has(ItemTags.PLANKS);
        DoorRecipes.build(writer, planks);
        WindowRecipes.build(writer, planks);
        TrimRecipes.build(writer, planks);
    }
}
