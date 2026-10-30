package io.github.dirtyredz.roundwood.data.server.recipe;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;


/**
 * The door recipes, any planks (mixed types allowed) and a gold nugget for the hardware: the round
 * door's eight planks around the nugget (the 0.4.0 shape), and the arched door's (0.11.0) six planks
 * standing in an arch over it, the top corners left empty (a crown), so neither grid matches the
 * other or vanilla's six-plank door.
 */
public final class DoorRecipes {
    private DoorRecipes() {
    }

    public static void build(RecipeOutput writer, Criterion<InventoryChangeTrigger.TriggerInstance> unlock) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.ROUND_DOOR.get())
                .pattern("PPP").pattern("PNP").pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('N', Items.GOLD_NUGGET)
                .group(Roundwood.MOD_ID + ":round_door")
                .unlockedBy("has_planks", unlock)
                .save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.ARCHED_DOOR.get())
                .pattern(" P ").pattern("PNP").pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('N', Items.GOLD_NUGGET)
                .group(Roundwood.MOD_ID + ":arched_door")
                .unlockedBy("has_planks", unlock)
                .save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.DOUBLE_ARCHED_DOOR.get())
                .pattern("PNP").pattern("PPP").pattern("PNP")
                .define('P', ItemTags.PLANKS).define('N', Items.GOLD_NUGGET)
                .group(Roundwood.MOD_ID + ":double_arched_door")
                .unlockedBy("has_planks", unlock).save(writer);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.TALL_DOUBLE_ARCHED_DOOR.get())
                .pattern("PPP").pattern("NDN").pattern("PPP")
                .define('P', ItemTags.PLANKS).define('N', Items.GOLD_NUGGET)
                .define('D', ModItems.DOUBLE_ARCHED_DOOR.get())
                .group(Roundwood.MOD_ID + ":tall_double_arched_door")
                .unlockedBy("has_planks", unlock).save(writer);
    }
}
