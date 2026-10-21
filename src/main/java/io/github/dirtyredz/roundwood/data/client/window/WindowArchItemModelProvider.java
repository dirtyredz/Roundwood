package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowProfile;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

/**
 * The arch pieces' inventory models: each piece's upright, centred core (front towards the viewer,
 * so the left piece reads as the left half), bound to oak, with the window's one-block transforms;
 * the client swaps the wood per stack appearance.
 */
public class WindowArchItemModelProvider extends ItemModelProvider {
    private static final Vector3f CENTRE = new Vector3f(8, 8, 8);

    public WindowArchItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood arch window item models";
    }

    @Override
    protected void registerModels() {
        for (WindowProfile profile : new WindowProfile[] {WindowProfile.ARCH_LEFT, WindowProfile.ARCH_RIGHT}) {
            ItemModelBuilder item = getBuilder(profile.id())
                    .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + profile.id())));
            ItemModelTransforms.block(item, CENTRE);
        }
    }
}
