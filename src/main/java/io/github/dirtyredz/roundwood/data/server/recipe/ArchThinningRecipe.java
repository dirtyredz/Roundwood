package io.github.dirtyredz.roundwood.data.server.recipe;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.item.StackPayloads;
import io.github.dirtyredz.roundwood.registry.ModRecipeSerializers;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * One full product becomes one thin product, keeping only explicitly supported metadata. The approved set is
 * the native equivalent of 1.20.1's approved tag keys: a name, lore and dyed colour ({@code display}),
 * enchantments, repair cost, tooltip hiding ({@code HideFlags}), unbreakability and adventure-mode predicates,
 * plus a valid TRIM appearance in custom data. Every other component change, including a removed default or
 * an unknown custom-data key, refuses the craft before any input is consumed. The output carries the input's
 * complete component patch, so no native metadata is lost. Serialization lives in {@link ArchThinningSerializer}.
 */
public final class ArchThinningRecipe extends ShapelessRecipe {
    private static final Set<DataComponentType<?>> SUPPORTED = Set.of(DataComponents.CUSTOM_DATA, DataComponents.CUSTOM_NAME,
            DataComponents.LORE, DataComponents.DYED_COLOR, DataComponents.ENCHANTMENTS, DataComponents.REPAIR_COST,
            DataComponents.HIDE_ADDITIONAL_TOOLTIP, DataComponents.UNBREAKABLE, DataComponents.CAN_BREAK, DataComponents.CAN_PLACE_ON);

    public ArchThinningRecipe(ShapelessRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.getIngredients());
        if (recipe.getIngredients().size() != 1 || recipe.getResultItem(RegistryAccess.EMPTY).getCount() != 1)
            throw new IllegalArgumentException("Thinning requires exactly one input and one output");
    }

    /** Whether the input's component changes are all approved (relative to its item's defaults). */
    public static boolean supportsMetadata(ItemStack stack) {
        return StackPayloads.onlyChanges(stack, SUPPORTED, Set.of()) && supportsCustomData(StackPayloads.customData(stack));
    }

    /** Custom data may hold only a canonical TRIM appearance; none at all is fine. */
    public static boolean supportsCustomData(@Nullable CompoundTag tag) {
        if (tag == null) return true;
        if (!Set.of(AppearanceNbt.TAG).containsAll(tag.getAllKeys())) return false;
        if (tag.contains(AppearanceNbt.TAG)) {
            var appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.TRIM::accepts);
            return appearance.isPresent() && AppearanceNbt.encode(appearance.get()).equals(tag.get(AppearanceNbt.TAG));
        }
        return true;
    }

    /** A datagen output that writes each shapeless recipe as a thinning recipe. */
    public static RecipeOutput output(RecipeOutput writer) {
        return new RecipeOutput() {
            @Override public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                writer.accept(id, new ArchThinningRecipe((ShapelessRecipe) recipe), advancement, conditions);
            }
            @Override public net.minecraft.advancements.Advancement.Builder advancement() { return writer.advancement(); }
        };
    }

    private static ItemStack input(CraftingInput container) {
        ItemStack input = ItemStack.EMPTY;
        for (ItemStack stack : container.items()) {
            if (stack.isEmpty()) continue;
            if (!input.isEmpty()) return ItemStack.EMPTY;
            input = stack;
        }
        return input.isEmpty() || !supportsMetadata(input) ? ItemStack.EMPTY : input;
    }

    @Override public boolean matches(CraftingInput container, Level level) {
        return !input(container).isEmpty() && super.matches(container, level);
    }

    @Override public ItemStack assemble(CraftingInput container, HolderLookup.Provider registries) {
        var source = input(container);
        if (source.isEmpty() || !getIngredients().get(0).test(source)) return ItemStack.EMPTY;
        return source.transmuteCopy(getResultItem(registries).getItem(), 1);
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.ARCH_THINNING.get(); }
}
