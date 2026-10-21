package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.WindowGeometry;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

/**
 * Window inventory models: the whole square housing with its round glazing (the core template,
 * translucent like the block); each finish only binds the wood.
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
        for (Finish finish : FinishCatalog.ALL) {
            getBuilder(finish.windowId()).parent(template)
                    .texture(WindowGeometry.FRAME, finish.panelTexture())
                    .texture("particle", finish.panelTexture());
        }
    }
}
