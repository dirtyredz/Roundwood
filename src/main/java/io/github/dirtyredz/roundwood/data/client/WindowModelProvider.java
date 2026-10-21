package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.WindowGeometry;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
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
 * translucent (Roundwood's tinted glazing); {@code waterlogged} never selects a model.
 */
public class WindowModelProvider extends BlockStateProvider {
    public static final String TEMPLATE = "template_round_window";
    public static final String ARM = "round_window_arm";
    /** Translucent: the glazing is see-through tinted glass (the wood is opaque either way). */
    static final String RENDER_TYPE = "translucent";
    /** Roundwood's own glazing: vanilla glass.png is transparent inside its border, where the round glass samples. */
    static final String GLASS = "block/window_glass";

    public WindowModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood window blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        BlockModelBuilder template = models().getBuilder(TEMPLATE).renderType(RENDER_TYPE);
        GeometryModelWriter.addPieces(template, WindowGeometry.pieces(), true)
                .texture(WindowGeometry.GLASS, modLoc(GLASS))
                .texture("particle", "#" + WindowGeometry.FRAME);
        // The arm's faces against the panel (its end, and the short reach on to the glass) are buried.
        BlockModelBuilder arm = GeometryModelWriter.addPieces(models().getBuilder(ARM).renderType(RENDER_TYPE),
                        WindowGeometry.arm(), true, new GeometryModelWriter.FaceStyle() {
                            @Override
                            public boolean draw(Box part, Direction face) {
                                return face != Direction.SOUTH
                                        && !(face == Direction.NORTH && part.minZ() >= WindowGeometry.PANEL_BACK);
                            }
                        })
                .texture(WindowGeometry.GLASS, modLoc(GLASS))
                .texture("particle", modLoc(GLASS));

        for (Finish finish : FinishCatalog.ALL) {
            BlockModelBuilder core = models().getBuilder(finish.windowId())
                    .parent(template)
                    .renderType(RENDER_TYPE)
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
