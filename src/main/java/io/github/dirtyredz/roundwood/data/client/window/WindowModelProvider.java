package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * The round windows' blockstates and models (plain and crossed), multipart like a vanilla pane. Every
 * position draws through {@link WindowPaneModelWriter}: centred, the upright model across {@code axis}
 * (turned 90 degrees for {@code axis=z}) in its arm combination; flush against a side, floor or
 * ceiling, the posed model, which grows no glass. Each also trimmed. The models bind oak planks; the
 * client swaps only those quads' sprite per window appearance. All models are translucent
 * (Roundwood's tinted glazing); {@code waterlogged} never selects a model.
 */
public class WindowModelProvider extends BlockStateProvider {
    public WindowModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood round window blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        for (WindowPaneBlock block : ModBlocks.windows()) {
            if (block.profile().oriented()) {
                continue;
            }
            for (WindowPose canonical : WindowPaneModelWriter.canonicalPoses(block.profile())) {
                WindowPaneModelWriter.writeModels(models(), block.design(), canonical);
            }
            panes(this, block, block.design());
        }
    }

    /** The multipart blockstate of a round window block (canonical or alias) drawing {@code design}. */
    public static void panes(BlockStateProvider provider, Block window, WindowDesign design) {
        MultiPartBlockStateBuilder states = provider.getMultipartBuilder(window);
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            WindowPaneModelWriter.addParts(states, design, WindowPosition.CENTER.pose(axis),
                    part -> part.condition(RoundWindowBlock.POSITION, WindowPosition.CENTER).condition(RoundWindowBlock.AXIS, axis));
        }
        for (WindowPosition position : WindowPosition.values()) {
            if (position != WindowPosition.CENTER) {
                WindowPaneModelWriter.addParts(states, design, position.pose(Direction.Axis.X),
                        part -> part.condition(RoundWindowBlock.POSITION, position));
            }
        }
    }
}
