package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.window.WindowGeometry;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Round window blockstates and models: one cutout template (wooden housing + round glass) bound per
 * finish, turned 90 degrees for {@code axis=z}. {@code waterlogged} never selects a model.
 */
public class WindowModelProvider extends BlockStateProvider {
    public static final String TEMPLATE = "template_round_window";
    static final String CUTOUT = "cutout";
    private static final ResourceLocation GLASS = ResourceLocation.fromNamespaceAndPath("minecraft", "block/glass");

    public WindowModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood window blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        BlockModelBuilder template = models().getBuilder(TEMPLATE).renderType(CUTOUT);
        GeometryModelWriter.addPieces(template, WindowGeometry.pieces(), true)
                .texture(WindowGeometry.GLASS, GLASS)
                .texture("particle", "#" + WindowGeometry.FRAME);

        for (Finish finish : FinishCatalog.ALL) {
            BlockModelBuilder model = models().getBuilder(finish.windowId())
                    .parent(template)
                    .renderType(CUTOUT)
                    .texture(WindowGeometry.FRAME, finish.panelTexture())
                    .texture("particle", finish.panelTexture());
            RoundWindowBlock block = ModBlocks.window(finish);
            getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(state.getValue(RoundWindowBlock.AXIS) == Direction.Axis.Z ? 90 : 0)
                    .build(), RoundWindowBlock.WATERLOGGED);
        }
    }
}
