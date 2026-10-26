package io.github.dirtyredz.roundwood.data.server.recipe;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.registry.ModRecipeSerializers;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import java.util.function.Consumer;

/** Four raw-material recipes; one default-oak product, never a styled input. */
public final class ArchPieceRecipes {
    private ArchPieceRecipes() { }
    public static void build(Consumer<FinishedRecipe> writer, InventoryChangeTrigger.TriggerInstance unlock) {
        for (var design : ArchPieceDesign.values()) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModItems.archPiece(design, ArchPieceThickness.THIN))
                    .requires(ModItems.archPiece(design)).group(Roundwood.MOD_ID + ":arch_thinning")
                    .unlockedBy("has_full_arch", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.archPiece(design)))
                    .save(recipe -> writer.accept(new FinishedRecipe() {
                        @Override public void serializeRecipeData(com.google.gson.JsonObject json) { recipe.serializeRecipeData(json); }
                        @Override public net.minecraft.resources.ResourceLocation getId() { return recipe.getId(); }
                        @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() { return ModRecipeSerializers.ARCH_THINNING.get(); }
                        @Override public com.google.gson.JsonObject serializeAdvancement() { return recipe.serializeAdvancement(); }
                        @Override public net.minecraft.resources.ResourceLocation getAdvancementId() { return recipe.getAdvancementId(); }
                    }));
        }
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
