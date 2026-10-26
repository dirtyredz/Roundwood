package io.github.dirtyredz.roundwood.data.server.recipe;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import java.util.function.Consumer;

/** Four raw-material recipes; one default-oak product, never a styled input. */
public final class ArchPieceRecipes {
    private ArchPieceRecipes() { }
    public static void build(Consumer<FinishedRecipe> writer, InventoryChangeTrigger.TriggerInstance unlock) {
        recipe(writer, unlock, ArchPieceDesign.TOP_1X1, "PPP", "P P");
        recipe(writer, unlock, ArchPieceDesign.TOP_1X2, "PPP", "P P", " S ");
        recipe(writer, unlock, ArchPieceDesign.TOP_2X3, "PPP", "P P", "S S");
        recipe(writer, unlock, ArchPieceDesign.KEYHOLE_SHOULDER, "PP", " P");
    }
    private static void recipe(Consumer<FinishedRecipe> writer, InventoryChangeTrigger.TriggerInstance unlock,
                               ArchPieceDesign design, String... rows) {
        var recipe = ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.archPiece(design))
                .define('P', ItemTags.PLANKS).group(Roundwood.MOD_ID + ":arch_piece").unlockedBy("has_planks", unlock);
        for (String row : rows) recipe.pattern(row);
        if (String.join("", rows).contains("S")) recipe.define('S', Items.STICK);
        recipe.save(writer);
    }
}
