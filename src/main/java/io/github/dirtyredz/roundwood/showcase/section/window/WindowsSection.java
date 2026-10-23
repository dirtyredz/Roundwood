package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/**
 * Round window neighbours, in oak only: a junction with a vanilla glass pane across the middle of
 * three, a stacked pair on different axes, one waterlogged, and one with a stone block right behind
 * it (a solid block on a face never grows a glass arm). Occupies {@code r 26..44, f 7..9}. Each
 * pane's plane runs along the builder's right unless stated.
 */
public final class WindowsSection implements ShowcaseSection {
    public static final int JUNCTION_R = 27;
    public static final int DEMO_F = 8;
    public static final int STACK_R = 33;
    public static final int WATER_R = 39;
    public static final int SOLID_R = 43;

    @Override
    public String id() {
        return "windows";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        Direction.Axis along = canvas.acrossAxis();
        Direction.Axis across = along == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Appearance oak = Appearance.DEFAULT;
        canvas.sign(JUNCTION_R, DEMO_F - 1, "Junction", "glass pane");
        for (int i = 0; i < 3; i++) {
            canvas.window(oak, JUNCTION_R + i, 0, DEMO_F, along, false);
        }
        canvas.set(JUNCTION_R + 1, 0, DEMO_F + 1, Blocks.GLASS_PANE.defaultBlockState());
        canvas.sign(STACK_R, DEMO_F - 1, "Stacked", "axes differ");
        canvas.window(oak, STACK_R, 0, DEMO_F, along, false);
        canvas.window(oak, STACK_R, 1, DEMO_F, across, false);
        canvas.sign(WATER_R, DEMO_F - 1, "Waterlogged");
        canvas.window(oak, WATER_R, 0, DEMO_F, along, true);
        canvas.sign(SOLID_R, DEMO_F - 1, "Stone behind", "no glass arm");
        canvas.set(SOLID_R, 0, DEMO_F + 1, Blocks.STONE.defaultBlockState());
        canvas.window(oak, SOLID_R, 0, DEMO_F, along, false);
    }
}
