package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

/**
 * The ten window items' inventory models: each piece's upright, centred, unconnected and untrimmed
 * block model (front towards the viewer, so a left piece reads as the left half), bound to oak, with
 * the one-block transforms; the client swaps the wood per stack appearance.
 */
public class WindowItemModelProvider extends ItemModelProvider {
    private static final Vector3f CENTRE = new Vector3f(8, 8, 8);

    public WindowItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood window item models";
    }

    @Override
    protected void registerModels() {
        for (WindowDesign design : WindowDesign.all()) {
            ItemModelBuilder item = getBuilder(design.id())
                    .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + WindowDesign.plain(design.profile()).id())));
            ItemModelTransforms.block(item, CENTRE);
        }
    }
}
