package io.github.dirtyredz.roundwood.data.recipes;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.data.server.recipe.ArchThinningRecipe;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The custom-data half of the thinning policy (the native-component half needs registered items and is
 * covered by {@code ArchThinningGameTests}): only a canonical TRIM appearance may ride in custom data.
 */
class ArchThinningRecipeTest {
    @Test void knownCustomDataIsReadOnlyAndFuturePayloadsRefuse() {
        var tag = new CompoundTag();
        AppearanceNbt.write(tag, Appearance.DEFAULT);
        var before = tag.copy();
        assertTrue(ArchThinningRecipe.supportsCustomData(tag));
        assertEquals(before, tag);
        tag.getCompound(AppearanceNbt.TAG).putInt("Version", 99);
        assertFalse(ArchThinningRecipe.supportsCustomData(tag));
        tag = before.copy(); tag.putString("FuturePayload", "keep me");
        assertFalse(ArchThinningRecipe.supportsCustomData(tag));
        // 1.20.1 display metadata now lives in native components; left over in custom data it is unknown.
        tag = before.copy(); tag.put("display", new CompoundTag());
        assertFalse(ArchThinningRecipe.supportsCustomData(tag));
        assertTrue(ArchThinningRecipe.supportsCustomData(null));
    }
}
