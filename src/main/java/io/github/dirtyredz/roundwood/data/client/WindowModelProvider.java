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
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Round window blockstates and models. The rim-and-glass core is a template bound per finish; the
 * connection pieces are glass only and shared. Multipart conditions mirror
 * {@link WindowGeometry#shape}: for {@code axis=x}, east/west neighbours fill the corners and
 * north/south neighbours get an arm; {@code axis=z} is the same turned 90 degrees. All models are
 * cutout. {@code waterlogged} never selects a model.
 */
public class WindowModelProvider extends BlockStateProvider {
    private static final String CUTOUT = "cutout";
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
        BlockModelBuilder coreTemplate = models().getBuilder("template_round_window").renderType(CUTOUT);
        GeometryModelWriter.addPieces(coreTemplate, WindowGeometry.core())
                .texture(WindowGeometry.GLASS, GLASS)
                .texture("particle", "#" + WindowGeometry.RIM);

        BlockModelBuilder fill = GeometryModelWriter.addPieces(
                        models().getBuilder("round_window_side_fill").renderType(CUTOUT), WindowGeometry.sideFill())
                .texture(WindowGeometry.GLASS, GLASS)
                .texture("particle", GLASS);
        BlockModelBuilder arm = GeometryModelWriter.addPieces(
                        models().getBuilder("round_window_arm").renderType(CUTOUT), WindowGeometry.arm())
                .texture(WindowGeometry.GLASS, GLASS)
                .texture("particle", GLASS);

        for (Finish finish : FinishCatalog.ALL) {
            BlockModelBuilder core = models().getBuilder(finish.windowId())
                    .parent(coreTemplate)
                    .renderType(CUTOUT)
                    .texture(WindowGeometry.RIM, finish.panelTexture())
                    .texture("particle", finish.panelTexture());
            RoundWindowBlock block = ModBlocks.window(finish);
            MultiPartBlockStateBuilder states = getMultipartBuilder(block);
            for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                int base = axis == Direction.Axis.X ? 0 : 90;
                states.part().modelFile(core).rotationY(base).addModel().condition(RoundWindowBlock.AXIS, axis).end();
                // Model pieces are authored for axis x: fill on the east side, arm on the north side.
                connection(states, axis, fill, base, inPlane(axis, true));
                connection(states, axis, fill, base + 180, inPlane(axis, false));
                connection(states, axis, arm, base, across(axis, true));
                connection(states, axis, arm, base + 180, across(axis, false));
            }
            itemModels().getBuilder(finish.windowId()).parent(core);
        }
    }

    private static void connection(MultiPartBlockStateBuilder states, Direction.Axis axis, ModelFile model,
                                   int rotation, BooleanProperty side) {
        states.part().modelFile(model).rotationY(rotation % 360).addModel()
                .condition(RoundWindowBlock.AXIS, axis)
                .condition(side, true)
                .end();
    }

    /** The in-plane side: east/west for axis x (first = east), south/north for axis z (first = south). */
    private static BooleanProperty inPlane(Direction.Axis axis, boolean first) {
        if (axis == Direction.Axis.X) {
            return first ? IronBarsBlock.EAST : IronBarsBlock.WEST;
        }
        return first ? IronBarsBlock.SOUTH : IronBarsBlock.NORTH;
    }

    /** The perpendicular side: north/south for axis x (first = north), east/west for axis z (first = east). */
    private static BooleanProperty across(Direction.Axis axis, boolean first) {
        if (axis == Direction.Axis.X) {
            return first ? IronBarsBlock.NORTH : IronBarsBlock.SOUTH;
        }
        return first ? IronBarsBlock.EAST : IronBarsBlock.WEST;
    }
}
