package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/**
 * Round window neighbours, in oak only: a stacked pair on different axes, one waterlogged, and one
 * with a stone block right behind it. (The junction demo, three windows round a vanilla glass pane, is
 * gone since 0.13.1: a window never connects to a neighbour any more.) Each pane's plane runs along the
 * builder's right unless stated. Three samples, each {@link ShowcaseSpacing#GAP} clear blocks from the
 * next, in the arches' row: occupies {@code r STACK_R..SOLID_R, f 18..19}.
 */
public final class WindowsSection implements ShowcaseSection {
    public static final int DEMO_F = WindowArchesSection.ROW_F;
    private static final int ONE = ShowcaseSpacing.pitch(1);
    public static final int STACK_R = WindowArchesSection.NEXT_R;
    public static final int WATER_R = STACK_R + ONE;
    public static final int SOLID_R = WATER_R + ONE;
    /** The column the next sample in this row starts in. */
    public static final int NEXT_R = SOLID_R + ONE;

    @Override
    public String id() {
        return "windows";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        Direction.Axis along = canvas.acrossAxis();
        Direction.Axis across = along == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Appearance oak = Appearance.DEFAULT;
        canvas.sign(STACK_R, DEMO_F, "Stacked", "axes differ");
        canvas.window(oak, STACK_R, 0, DEMO_F, along, false);
        canvas.window(oak, STACK_R, 1, DEMO_F, across, false);
        canvas.sign(WATER_R, DEMO_F, "Waterlogged");
        canvas.window(oak, WATER_R, 0, DEMO_F, along, true);
        canvas.sign(SOLID_R, DEMO_F, "Stone behind", "no bridge");
        canvas.set(SOLID_R, 0, DEMO_F + 1, Blocks.STONE.defaultBlockState());
        canvas.window(oak, SOLID_R, 0, DEMO_F, along, false);
    }
}
