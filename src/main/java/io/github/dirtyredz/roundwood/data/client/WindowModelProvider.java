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
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Round window blockstates and models, multipart like a vanilla pane: the core (wooden face with
 * round glass) for every state of an axis, turned 90 degrees for {@code axis=z}, plus one shared
 * glass arm for each connected side across the plane. In-plane connections add nothing (the
 * square face already meets its neighbour). The item uses the core template. All models are
 * cutout; {@code waterlogged} never selects a model.
 */
public class WindowModelProvider extends BlockStateProvider {
    public static final String TEMPLATE = "template_round_window";
    public static final String ARM = "round_window_arm";
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
        // The arm's faces against the panel (its end, and the short reach on to the glass) are buried.
        BlockModelBuilder arm = GeometryModelWriter.addPieces(models().getBuilder(ARM).renderType(CUTOUT),
                        WindowGeometry.arm(), true,
                        (part, face) -> face != Direction.SOUTH
                                && !(face == Direction.NORTH && part.minZ() >= WindowGeometry.PANEL_BACK))
                .texture(WindowGeometry.GLASS, GLASS)
                .texture("particle", GLASS);

        for (Finish finish : FinishCatalog.ALL) {
            BlockModelBuilder core = models().getBuilder(finish.windowId())
                    .parent(template)
                    .renderType(CUTOUT)
                    .texture(WindowGeometry.FRAME, finish.panelTexture())
                    .texture("particle", finish.panelTexture());
            MultiPartBlockStateBuilder states = getMultipartBuilder(ModBlocks.window(finish));
            states.part().modelFile(core).addModel().condition(RoundWindowBlock.AXIS, Direction.Axis.X).end();
            states.part().modelFile(core).rotationY(90).addModel().condition(RoundWindowBlock.AXIS, Direction.Axis.Z).end();
            // The arm is authored towards the north edge; each side across the plane turns it.
            arm(states, arm, Direction.Axis.X, CrossCollisionBlock.NORTH, 0);
            arm(states, arm, Direction.Axis.X, CrossCollisionBlock.SOUTH, 180);
            arm(states, arm, Direction.Axis.Z, CrossCollisionBlock.EAST, 90);
            arm(states, arm, Direction.Axis.Z, CrossCollisionBlock.WEST, 270);
        }
    }

    private static void arm(MultiPartBlockStateBuilder states, ModelFile arm, Direction.Axis axis, BooleanProperty side,
                            int rotation) {
        states.part().modelFile(arm).rotationY(rotation).addModel()
                .condition(RoundWindowBlock.AXIS, axis).condition(side, true).end();
    }
}
