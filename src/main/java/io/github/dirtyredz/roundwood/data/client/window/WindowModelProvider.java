package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * The round window's blockstate and models, multipart like a vanilla pane. Centred (the 0.5.0
 * models, unchanged): the core (wooden face with round glass) per axis, turned 90 degrees for
 * {@code axis=z}, plus one shared glass arm for each connected side across the plane; in-plane
 * connections add nothing. Flush against a side: the posed core, hub, arms and caps of
 * {@link WindowPaneModelWriter} (three canonical poses, turned per side). The cores bind oak planks;
 * the client swaps only those quads' sprite per window appearance. All models are translucent
 * (Roundwood's tinted glazing); {@code waterlogged} never selects a model.
 */
public class WindowModelProvider extends BlockStateProvider {
    public static final String TEMPLATE = "template_round_window";
    public static final String ARM = "round_window_arm";
    public static final String CORE = "round_window";
    /** The wood bound into the core: the template the client retextures from. */
    public static final String OAK_PLANKS = WindowPaneModelWriter.OAK_PLANKS;
    /** Translucent: the glazing is see-through tinted glass (the wood is opaque either way). */
    static final String RENDER_TYPE = WindowPaneModelWriter.RENDER_TYPE;
    /** Roundwood's own glazing: vanilla glass.png is transparent inside its border, where the round glass samples. */
    static final String GLASS = WindowPaneModelWriter.GLASS;

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
        BlockModelBuilder core = models().getBuilder(CORE)
                .parent(template)
                .renderType(RENDER_TYPE)
                .texture(WindowGeometry.FRAME, OAK_PLANKS)
                .texture("particle", OAK_PLANKS);
        for (WindowPose canonical : WindowPaneModelWriter.canonicalPoses(WindowProfile.ROUND)) {
            WindowPaneModelWriter.writeModels(models(), WindowProfile.ROUND, canonical);
        }
        panes(this, ModBlocks.roundWindow(), core, arm);
    }

    /**
     * The multipart blockstate of a round window block (canonical or alias): the centred core and arms
     * ({@code core}, {@code arm}), then every flush position's posed parts.
     */
    public static void panes(BlockStateProvider provider, Block window, ModelFile core, ModelFile arm) {
        MultiPartBlockStateBuilder states = provider.getMultipartBuilder(window);
        states.part().modelFile(core).addModel()
                .condition(RoundWindowBlock.POSITION, WindowPosition.CENTER).condition(RoundWindowBlock.AXIS, Direction.Axis.X).end();
        states.part().modelFile(core).rotationY(90).addModel()
                .condition(RoundWindowBlock.POSITION, WindowPosition.CENTER).condition(RoundWindowBlock.AXIS, Direction.Axis.Z).end();
        // The arm is authored towards the north edge; each side across the plane turns it.
        arm(states, arm, Direction.Axis.X, CrossCollisionBlock.NORTH, 0);
        arm(states, arm, Direction.Axis.X, CrossCollisionBlock.SOUTH, 180);
        arm(states, arm, Direction.Axis.Z, CrossCollisionBlock.EAST, 90);
        arm(states, arm, Direction.Axis.Z, CrossCollisionBlock.WEST, 270);
        for (WindowPosition position : WindowPosition.values()) {
            if (position != WindowPosition.CENTER) {
                WindowPaneModelWriter.addParts(states, WindowProfile.ROUND, position.pose(Direction.Axis.X),
                        part -> part.condition(RoundWindowBlock.POSITION, position));
            }
        }
    }

    private static void arm(MultiPartBlockStateBuilder states, ModelFile arm, Direction.Axis axis, BooleanProperty side,
                            int rotation) {
        states.part().modelFile(arm).rotationY(rotation).addModel()
                .condition(RoundWindowBlock.POSITION, WindowPosition.CENTER).condition(RoundWindowBlock.AXIS, axis)
                .condition(side, true).end();
    }
}
