package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.pane.OrientedWindowBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.placement.WindowOrientation;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;

/**
 * The oriented windows' models and multipart blockstates (both arches and both bottom corners, plain
 * and crossed): nine canonical poses per piece (upright facing south at each depth; front up or down
 * with the top north, at each depth) through {@link WindowPaneModelWriter}, and every one of the 36
 * orientation and depth combinations drawn as a quarter turn of one of them. Translucent, oak-bound.
 */
public class OrientedWindowModelProvider extends BlockStateProvider {
    public OrientedWindowModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood oriented window blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        for (OrientedWindowBlock block : ModBlocks.orientedWindows()) {
            for (WindowPose canonical : WindowPaneModelWriter.canonicalPoses(block.profile())) {
                WindowPaneModelWriter.writeModels(models(), block.design(), canonical);
            }
            panes(this, block, block.design());
        }
    }

    public static void panes(BlockStateProvider provider, OrientedWindowBlock block,
                             WindowDesign design) {
            MultiPartBlockStateBuilder states = provider.getMultipartBuilder(block);
            for (WindowOrientation orientation : WindowOrientation.values()) {
                for (WindowDepth depth : WindowDepth.values()) {
                    WindowPaneModelWriter.addParts(states, design, orientation.pose(depth),
                            part -> part.condition(OrientedWindowBlock.ORIENTATION, orientation)
                                    .condition(OrientedWindowBlock.DEPTH, depth));
                }
            }
    }
}
