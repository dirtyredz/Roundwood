package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.client.door.DoorLeafModels;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

/**
 * The door's inventory model: the whole closed leaf (left hinge) face-on, from one template carrying
 * the display transforms, bound to oak; the client retextures its panel per stack appearance.
 */
public class DoorItemModelProvider extends ItemModelProvider {
    static final String TEMPLATE = "template_round_door";
    /** The disc's middle, in model pixels. */
    private static final Vector3f CENTRE = new Vector3f(DoorGeometry.DIAMETER / 2F, DoorGeometry.DIAMETER / 2F,
            (float) DoorGeometry.PIVOT_Z);

    public DoorItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood door item models";
    }

    @Override
    protected void registerModels() {
        ItemModelBuilder template = getBuilder(TEMPLATE)
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + DoorModelProvider.LEAF_TEMPLATE
                        + DoorHingeSide.LEFT.getSerializedName())));
        ItemModelTransforms.assembly(template, CENTRE);
        getBuilder(DoorLeafModels.DOOR_ID).parent(template)
                .texture(DoorGeometry.PANEL, modLoc(DoorModelProvider.OAK_PANEL))
                .texture(DoorGeometry.BRASS, modLoc("block/brass"))
                .texture("particle", modLoc(DoorModelProvider.OAK_PANEL));
    }
}
