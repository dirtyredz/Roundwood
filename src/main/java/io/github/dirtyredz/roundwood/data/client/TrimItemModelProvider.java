package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.trim.geometry.TrimGeometry;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.variant.TrimMaterial;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

/**
 * Frame inventory models: the complete ring with its open centre, face-on, as one baked model. Two
 * templates (tiled and whole-image faces, as for the quarters in {@link TrimModelProvider}) carry the
 * ring geometry and the display transforms; each material only binds textures.
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
        ItemModelBuilder tiled = template(TEMPLATE, false);
        ItemModelBuilder whole = template(WHOLE_TEMPLATE, true);
        for (TrimMaterial material : FinishCatalog.TRIMS) {
            getBuilder(material.trimId()).parent(material.wholeFace() ? whole : tiled)
                    .texture(TrimGeometry.FACE, material.faceTexture())
                    .texture(TrimGeometry.EDGE, material.edgeTexture())
                    .texture("particle", material.edgeTexture());
        }
    }

    private ItemModelBuilder template(String name, boolean wholeFace) {
        ItemModelBuilder builder = getBuilder(name);
        builder.texture("particle", "#" + TrimGeometry.EDGE);
        GeometryModelWriter.addPieces(builder, TrimGeometry.pieces(), false, TrimModelProvider.style(wholeFace, 0, 0, false));
        ItemModelTransforms.assembly(builder, CENTRE);
        return builder;
    }
}
