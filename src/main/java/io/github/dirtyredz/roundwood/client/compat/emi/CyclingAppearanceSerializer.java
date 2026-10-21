package io.github.dirtyredz.roundwood.client.compat.emi;

import com.google.gson.JsonElement;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;

/**
 * Favourites, bookmarks and recipe-tree copies of a preview entry are saved as the ordinary default
 * stack (EMI's own item form), so they reload as a plain round door, window or trim and saved data
 * never depends on the preview. Nothing is ever written in this serializer's own type.
 */
public final class CyclingAppearanceSerializer implements EmiIngredientSerializer<CyclingAppearanceStack> {
    @Override
    public String getType() {
        return "roundwood_preview";
    }

    @Override
    public EmiIngredient deserialize(JsonElement element) {
        return EmiIngredientSerializer.getDeserialized(element);
    }

    @Override
    public JsonElement serialize(CyclingAppearanceStack stack) {
        return EmiIngredientSerializer.getSerialized(stack.plain());
    }
}
