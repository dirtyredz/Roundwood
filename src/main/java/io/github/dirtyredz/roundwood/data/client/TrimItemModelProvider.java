package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.trim.geometry.TrimGeometry;
import io.github.dirtyredz.roundwood.client.trim.TrimModelNames;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

/**
 * The trim's inventory models: the complete ring with its open centre, face-on, as one baked model.
 * Two templates (tiled and whole-image faces, as for the quarters in {@link TrimModelProvider}) carry
 * the ring geometry and the display transforms. The item itself is the tiled oak ring; the whole-ring
 * oak model is the source the client retextures for painted and masonry stacks.
 */
public class TrimItemModelProvider extends ItemModelProvider {
    static final String TEMPLATE = "template_round_trim";
    static final String WHOLE_TEMPLATE = "template_round_trim_whole";
    /** The ring's middle, in model pixels. */
    private static final Vector3f CENTRE = new Vector3f(TrimGeometry.OUTER / 2F, TrimGeometry.OUTER / 2F,
            (float) (TrimGeometry.BACK + TrimGeometry.FRONT) / 2F);

    public TrimItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood frame item models";
    }

    @Override
    protected void registerModels() {
        bind(TrimModelNames.TRIM_ID, template(TEMPLATE, false), TrimModelProvider.OAK_LOG);
        bind(TrimModelNames.WHOLE_ITEM, template(WHOLE_TEMPLATE, true), TrimModelProvider.OAK_PANEL);
    }

    private void bind(String name, ItemModelBuilder template, String texture) {
        getBuilder(name).parent(template)
                .texture(TrimGeometry.FACE, texture)
                .texture(TrimGeometry.EDGE, texture)
                .texture("particle", texture);
    }

    private ItemModelBuilder template(String name, boolean wholeFace) {
        ItemModelBuilder builder = getBuilder(name);
        builder.texture("particle", "#" + TrimGeometry.EDGE);
        GeometryModelWriter.addPieces(builder, TrimGeometry.pieces(), false, TrimModelProvider.style(wholeFace, 0, 0, false));
        ItemModelTransforms.assembly(builder, CENTRE);
        return builder;
    }
}
