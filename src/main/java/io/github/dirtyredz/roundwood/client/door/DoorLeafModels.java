package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import io.github.dirtyredz.roundwood.geometry.Box;

/**
 * Names of the whole-leaf models, shared by the model generator and the renderer: one oak leaf per
 * design and hinge side ({@code block/round_door_leaf_left|right}, since 0.11.0 also
 * {@code block/arched_door_leaf_left|right}), retextured per appearance at runtime. They are not
 * referenced by any blockstate, so the client registers them for baking explicitly.
 */
public final class DoorLeafModels {
    /** The round door's id (its item and empty world model; the 0.4.0 aliases point at them). */
    public static final String DOOR_ID = DoorDesign.ROUND.id();
    private static final ResourceLocation[][] LOCATIONS = new ResourceLocation[DoorDesign.values().length][DoorHingeSide.values().length];
    static {
        for (var design : DoorDesign.values()) for (var hinge : DoorHingeSide.values())
            LOCATIONS[design.ordinal()][hinge.ordinal()] = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, path(design, hinge));
    }

    public static double modelYScale(DoorDesign design) { return Math.min(1, 48.0 / design.height()); }

    public static Box modelBox(DoorDesign design, Box b) {
        if (!design.doubleLeaf()) return b;
        double scale = modelYScale(design);
        return new Box(b.minX()-16, b.minY()*scale-16, b.minZ(), b.maxX()-16, b.maxY()*scale-16, b.maxZ());
    }

    public static Box authoredBox(DoorDesign design, Box b) {
        if (!design.doubleLeaf()) return b;
        double scale = modelYScale(design);
        return new Box(b.minX()+16, (b.minY()+16)/scale, b.minZ(), b.maxX()+16, (b.maxY()+16)/scale, b.maxZ());
    }

    private DoorLeafModels() {
    }

    /** Model path (without namespace) of {@code design}'s leaf for {@code hinge}. */
    public static String path(DoorDesign design, DoorHingeSide hinge) {
        return "block/" + design.id() + "_leaf_" + hinge.getSerializedName();
    }

    public static ResourceLocation location(DoorDesign design, DoorHingeSide hinge) {
        return LOCATIONS[design.ordinal()][hinge.ordinal()];
    }
}
