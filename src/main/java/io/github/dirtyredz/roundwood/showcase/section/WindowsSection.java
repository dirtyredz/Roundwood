package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Windows: every wood sample in one connected row, a junction with a vanilla glass pane across the
 * middle of three, a stacked pair on different axes, one waterlogged, and one with a stone block
 * right behind it (a solid block on a face never grows a glass arm). Paints are in
 * {@link PaintsSection}. Occupies {@code r 28..46, f 10..17}.
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
        canvas.sign(ROW_R, ROW_F - 1, "Windows", "every wood", "left to right");
        List<Appearance> woods = canvas.woods();
        for (int i = 0; i < woods.size(); i++) {
            canvas.window(woods.get(i), ROW_R + i, 0, ROW_F, along, false);
        }
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
