package io.github.dirtyredz.roundwood.data;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.data.server.recipe.ArchThinningRecipe;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchThinningRecipeTest {
    @Test void knownMetadataIsReadOnlyAndFuturePayloadsRefuse() {
        var tag = new CompoundTag();
        AppearanceNbt.write(tag, Appearance.DEFAULT);
        var display = new CompoundTag(); display.putString("Name", "\"A named arch\"");
        tag.put("display", display);
        tag.putInt("RepairCost", 3);
        var before = tag.copy();
        assertTrue(ArchThinningRecipe.supportsMetadata(tag));
        assertEquals(before, tag);
        tag.getCompound(AppearanceNbt.TAG).putInt("Version", 99);
        assertFalse(ArchThinningRecipe.supportsMetadata(tag));
        tag = before.copy(); tag.putString("FuturePayload", "keep me");
        assertFalse(ArchThinningRecipe.supportsMetadata(tag));
        tag = before.copy(); tag.putString("display", "invalid");
        assertFalse(ArchThinningRecipe.supportsMetadata(tag));
        assertTrue(ArchThinningRecipe.supportsMetadata(null));
    }
}
