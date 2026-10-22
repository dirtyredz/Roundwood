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

/** The door recipe: eight planks (mixed types allowed) around a gold nugget (the 0.4.0 shape). */
public final class DoorRecipes {
    private DoorRecipes() {
    }

    public static void build(Consumer<FinishedRecipe> writer, InventoryChangeTrigger.TriggerInstance unlock) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.ROUND_DOOR.get())
                .pattern("PPP").pattern("PNP").pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('N', Items.GOLD_NUGGET)
                .group(Roundwood.MOD_ID + ":round_door")
                .unlockedBy("has_planks", unlock)
                .save(writer);
    }
}
