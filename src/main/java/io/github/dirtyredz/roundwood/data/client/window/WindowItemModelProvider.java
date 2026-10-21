package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowGeometry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

/**
 * The window's inventory model: the whole square housing with its round glazing (the core template,
 * translucent like the block), bound to oak; the client swaps the wood per stack appearance.
 */
public class WindowItemModelProvider extends ItemModelProvider {
    static final String TEMPLATE = "template_round_window";
    /** The window block's middle, in model pixels. */
    private static final Vector3f CENTRE = new Vector3f(WindowGeometry.SIZE / 2F, WindowGeometry.SIZE / 2F,
            WindowGeometry.SIZE / 2F);

    public WindowItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood window item models";
    }

    @Override
    protected void registerModels() {
        ItemModelBuilder template = getBuilder(TEMPLATE)
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + WindowModelProvider.TEMPLATE)));
        ItemModelTransforms.block(template, CENTRE);
        getBuilder(WindowModelProvider.CORE).parent(template)
                .texture(WindowGeometry.FRAME, WindowModelProvider.OAK_PLANKS)
                .texture("particle", WindowModelProvider.OAK_PLANKS);
    }
}
