package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Windows: every finish in one connected row, a junction with a vanilla glass pane across the middle
 * of three, a stacked pair on different axes, one waterlogged, and one with a stone block right
 * behind it (a solid block on a face never grows a glass arm). Occupies {@code r 28..44, f 10..17}.
 * Each pane's plane runs along the builder's right unless stated.
 */
public final class WindowsSection implements ShowcaseSection {
    public static final int ROW_R = 28;
    public static final int ROW_F = 12;
    public static final int JUNCTION_R = 28;
    public static final int DEMO_F = 16;
    public static final int STACK_R = 34;
    public static final int WATER_R = 40;
    public static final int SOLID_R = 44;

    @Override
    public String id() {
        return "windows";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        Direction.Axis along = canvas.acrossAxis();
        Direction.Axis across = along == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        canvas.sign(ROW_R, ROW_F - 1, "Windows", "every finish", "left to right");
        for (int i = 0; i < FinishCatalog.ALL.size(); i++) {
            window(canvas, ModBlocks.window(FinishCatalog.ALL.get(i)), ROW_R + i, 0, ROW_F, along, false);
        }
        RoundWindowBlock oak = ModBlocks.window(FinishCatalog.PAINT_BASE);
        canvas.sign(JUNCTION_R, DEMO_F - 1, "Junction", "glass pane");
        for (int i = 0; i < 3; i++) {
            window(canvas, oak, JUNCTION_R + i, 0, DEMO_F, along, false);
        }
        canvas.set(JUNCTION_R + 1, 0, DEMO_F + 1, Blocks.GLASS_PANE.defaultBlockState());
        canvas.sign(STACK_R, DEMO_F - 1, "Stacked", "axes differ");
        window(canvas, oak, STACK_R, 0, DEMO_F, along, false);
        window(canvas, oak, STACK_R, 1, DEMO_F, across, false);
        canvas.sign(WATER_R, DEMO_F - 1, "Waterlogged");
        window(canvas, oak, WATER_R, 0, DEMO_F, along, true);
        canvas.sign(SOLID_R, DEMO_F - 1, "Stone behind", "no glass arm");
        canvas.set(SOLID_R, 0, DEMO_F + 1, Blocks.STONE.defaultBlockState());
        window(canvas, oak, SOLID_R, 0, DEMO_F, along, false);
    }

    /** Sets the pane with the connections it would get from its neighbours; they update on the write. */
    private static void window(ShowcaseCanvas canvas, RoundWindowBlock block, int r, int y, int f, Direction.Axis axis,
                               boolean waterlogged) {
        BlockPos pos = canvas.pos(r, y, f);
        BlockState state = block.defaultBlockState().setValue(RoundWindowBlock.AXIS, axis)
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        canvas.set(r, y, f, Block.updateFromNeighbourShapes(state, canvas.level(), pos));
    }
}
