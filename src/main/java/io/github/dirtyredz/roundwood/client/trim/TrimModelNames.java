package io.github.dirtyredz.roundwood.client.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import net.minecraft.resources.ResourceLocation;

/**
 * Names of the frame's source models, shared by the model generator and the client (like
 * {@code client/door/DoorLeafModels}): per quarter an oak model in each mapping family, tiled
 * ({@code block/round_trim_<part>}) and whole-ring ({@code block/round_trim_whole_<part>}), authored
 * facing north. No blockstate references them; the client registers them for baking and retextures
 * and turns them per appearance. (The 0.8.0 whole-ring item model is gone: the {@code round_trim}
 * item is the Universal Trim's square icon since 0.9.0.)
 */
public final class TrimModelNames {
    public static final String TRIM_ID = "round_trim";

    private TrimModelNames() {
    }

    /** Model path (without namespace) of a quarter. */
    public static String quarter(TrimPart part, boolean whole) {
        return "block/" + TRIM_ID + (whole ? "_whole_" : "_") + part.getSerializedName();
    }

    public static ResourceLocation quarterLocation(TrimPart part, boolean whole) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, quarter(part, whole));
    }
}
