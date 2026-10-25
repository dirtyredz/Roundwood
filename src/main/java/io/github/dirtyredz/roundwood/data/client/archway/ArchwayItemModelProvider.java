package io.github.dirtyredz.roundwood.data.client.archway;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;
import io.github.dirtyredz.roundwood.geometry.PixelSlab;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

import java.util.List;

/**
 * The archways' inventory models (0.12.0), one per design: the whole frame face-on, full depth,
 * from the same mask as its shapes ({@link ArchwayDesign#inFrame}), cut per cell by {@link PixelSlab}, in oak planks tiled like a
 * block (front and back {@code #face}, sides {@code #edge}), with the two-block assembly's display
 * transforms centred on the frame. A styled stack is baked from the same mask on the client
 * ({@code client/archway/ArchwayModels#item}), which copies these transforms.
 */
public class ArchwayItemModelProvider extends ItemModelProvider {
    static final String FACE = "face";
    static final String EDGE = "edge";
    private static final Vector3f CENTRE = new Vector3f(ArchwayDesign.SIZE / 2F, ArchwayDesign.SIZE / 2F,
            (float) (ArchwayDesign.BACK + ArchwayDesign.FRONT) / 2F);

    public ArchwayItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood archway item models";
    }

    @Override
    protected void registerModels() {
        for (ArchwayDesign design : ArchwayDesign.values()) {
            ItemModelBuilder item = getBuilder(design.id());
            for (int column = 0; column < 2; column++) {
                for (int row = 0; row < 2; row++) {
                    List<Surface> cell = PixelSlab.cellFaces(design::inFrame, 16 * column, 16 * row, ArchwayDesign.BACK,
                            ArchwayDesign.FRONT, "");
                    GeometryModelWriter.addSurfaces(item, cell, false,
                            surface -> surface.face().getAxis() == Direction.Axis.Z ? FACE : EDGE, 16 * column, 16 * row);
                }
            }
            item.texture(FACE, ArchwayModelProvider.OAK_PLANKS).texture(EDGE, ArchwayModelProvider.OAK_PLANKS)
                    .texture("particle", ArchwayModelProvider.OAK_PLANKS);
            ItemModelTransforms.assembly(item, CENTRE);
        }
    }

}
