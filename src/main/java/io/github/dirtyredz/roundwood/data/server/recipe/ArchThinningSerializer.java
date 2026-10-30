package io.github.dirtyredz.roundwood.data.server.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * {@code roundwood:arch_thinning}: the vanilla shapeless JSON and wire format, validated into an
 * {@link ArchThinningRecipe} (exactly one ingredient and one result).
 */
public final class ArchThinningSerializer implements RecipeSerializer<ArchThinningRecipe> {
    private static final MapCodec<ArchThinningRecipe> CODEC = RecipeSerializer.SHAPELESS_RECIPE.codec()
            .flatXmap(ArchThinningSerializer::validated, recipe -> DataResult.success(recipe));
    private static final StreamCodec<RegistryFriendlyByteBuf, ArchThinningRecipe> STREAM_CODEC =
            RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(ArchThinningRecipe::new, recipe -> recipe);

    @Override public MapCodec<ArchThinningRecipe> codec() { return CODEC; }

    @Override public StreamCodec<RegistryFriendlyByteBuf, ArchThinningRecipe> streamCodec() { return STREAM_CODEC; }

    private static DataResult<ArchThinningRecipe> validated(ShapelessRecipe recipe) {
        try {
            return DataResult.success(new ArchThinningRecipe(recipe));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }
}
