package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Names of the whole-leaf models, shared by the model generator and the renderer. Each door finish
 * has one leaf per hinge side ({@code block/<finish>_round_door_leaf_left|right}); they are not
 * referenced by any blockstate, so the client registers them for baking explicitly.
 */
public final class DoorLeafModels {
    private DoorLeafModels() {
    }

    /** Model path (without namespace) for a door id such as {@code oak_round_door}. */
    public static String path(String doorId, DoorHingeSide hinge) {
        return "block/" + doorId + "_leaf_" + hinge.getSerializedName();
    }

    public static ResourceLocation location(String doorId, DoorHingeSide hinge) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, path(doorId, hinge));
    }
}
