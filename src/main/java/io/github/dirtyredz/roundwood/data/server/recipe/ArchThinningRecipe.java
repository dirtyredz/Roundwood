package io.github.dirtyredz.roundwood.data.server.recipe;

import com.google.gson.JsonObject;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.registry.ModRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import java.util.Set;

/** One full product becomes one thin product, keeping only explicitly supported metadata. */
public final class ArchThinningRecipe extends ShapelessRecipe {
    private static final Set<String> SUPPORTED = Set.of(AppearanceNbt.TAG, "display", "Enchantments",
            "RepairCost", "HideFlags", "Unbreakable", "CanDestroy", "CanPlaceOn");

    public ArchThinningRecipe(ShapelessRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.getIngredients());
        if (recipe.getIngredients().size() != 1 || recipe.getResultItem(RegistryAccess.EMPTY).getCount() != 1)
            throw new IllegalArgumentException("Thinning requires exactly one input and one output");
    }

    public static boolean supportsMetadata(CompoundTag tag) {
        if (tag == null) return true;
        if (!SUPPORTED.containsAll(tag.getAllKeys())) return false;
        if (tag.contains(AppearanceNbt.TAG)) {
            var appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.TRIM::accepts);
            if (appearance.isEmpty() || !AppearanceNbt.encode(appearance.get()).equals(tag.get(AppearanceNbt.TAG))) return false;
        }
        if (tag.contains("display") && !tag.contains("display", Tag.TAG_COMPOUND)) return false;
        for (String key : new String[]{"Enchantments", "CanDestroy", "CanPlaceOn"})
            if (tag.contains(key) && !tag.contains(key, Tag.TAG_LIST)) return false;
        for (String key : new String[]{"RepairCost", "HideFlags"})
            if (tag.contains(key) && !tag.contains(key, Tag.TAG_INT)) return false;
        return !tag.contains("Unbreakable") || tag.contains("Unbreakable", Tag.TAG_BYTE);
    }

    private static ItemStack input(CraftingContainer container) {
        ItemStack input = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (!input.isEmpty()) return ItemStack.EMPTY;
            input = stack;
        }
        if (input.isEmpty() || !supportsMetadata(input.getTag())) return ItemStack.EMPTY;
        var saved = input.serializeNBT();
        if (saved.contains("ForgeCaps") && (!saved.contains("ForgeCaps", Tag.TAG_COMPOUND) || !saved.getCompound("ForgeCaps").isEmpty())) return ItemStack.EMPTY;
        return input;
    }

    @Override public boolean matches(CraftingContainer container, Level level) {
        return !input(container).isEmpty() && super.matches(container, level);
    }

    @Override public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        var source = input(container);
        if (source.isEmpty() || !getIngredients().get(0).test(source)) return ItemStack.EMPTY;
        var output = super.assemble(container, registries);
        if (source.hasTag()) output.setTag(source.getTag().copy());
        return output;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.ARCH_THINNING.get(); }

    public static final class Serializer implements RecipeSerializer<ArchThinningRecipe> {
        private final ShapelessRecipe.Serializer delegate = new ShapelessRecipe.Serializer();
        @Override public ArchThinningRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ArchThinningRecipe(delegate.fromJson(id, json));
        }
        @Override public ArchThinningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ArchThinningRecipe(delegate.fromNetwork(id, buffer));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ArchThinningRecipe recipe) { delegate.toNetwork(buffer, recipe); }
    }
}
