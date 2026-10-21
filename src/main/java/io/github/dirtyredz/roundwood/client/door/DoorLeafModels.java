package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Names of the whole-leaf models, shared by the model generator and the renderer: one oak leaf per
 * hinge side ({@code block/round_door_leaf_left|right}), retextured per appearance at runtime. They
 * are not referenced by any blockstate, so the client registers them for baking explicitly.
 */
public final class DoorLeafModels {
    public static final String DOOR_ID = "round_door";

    private DoorLeafModels() {
    }

    /** Model path (without namespace) of the leaf for {@code hinge}. */
    public static String path(DoorHingeSide hinge) {
        return "block/" + DOOR_ID + "_leaf_" + hinge.getSerializedName();
    }

    public static ResourceLocation location(DoorHingeSide hinge) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, path(hinge));
    }
}
