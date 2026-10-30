package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.data.server.recipe.ArchThinningSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registration of metadata-preserving crafting conversions. */
public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Roundwood.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, ArchThinningSerializer> ARCH_THINNING = SERIALIZERS.register("arch_thinning", ArchThinningSerializer::new);
    private ModRecipeSerializers() {}
}
