package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.data.server.recipe.ArchThinningRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registration of metadata-preserving crafting conversions. */
public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Roundwood.MOD_ID);
    public static final RegistryObject<ArchThinningRecipe.Serializer> ARCH_THINNING = SERIALIZERS.register("arch_thinning", ArchThinningRecipe.Serializer::new);
    private ModRecipeSerializers() {}
}
