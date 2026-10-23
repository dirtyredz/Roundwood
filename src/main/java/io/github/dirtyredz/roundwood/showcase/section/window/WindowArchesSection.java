package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import net.minecraft.core.Direction;


/**
 * The arch pieces: pairs forming the upper half of a two-block round window (centred upright, flush
 * against the far side of the cell, on the floor, on the ceiling) and each piece alone, in oak only.
 * Every pair is a left piece on the builder's left and a right piece beside it, front towards the
 * builder (floor: top away from them; ceiling: top towards them, as placement would choose). One row
 * of six samples, each {@link ShowcaseSpacing#GAP} clear blocks from the next; occupies
 * {@code r 0..SINGLE_RIGHT_R, f 18}.
 */
public final class WindowArchesSection implements ShowcaseSection {
    public static final int ROW_F = ShowcaseSpacing.ROW_ARCHES;
    private static final int ONE = ShowcaseSpacing.pitch(1);
    private static final int TWO = ShowcaseSpacing.pitch(2);
    public static final int CENTRED_R = ShowcaseSpacing.FIRST_R;
    public static final int FLUSH_R = CENTRED_R + TWO;
    public static final int FLOOR_R = FLUSH_R + TWO;
    public static final int CEILING_R = FLOOR_R + TWO;
    /** The singles share the row of the pairs. */
    public static final int SINGLES_F = ROW_F;
    public static final int SINGLE_LEFT_R = CEILING_R + TWO;
    public static final int SINGLE_RIGHT_R = SINGLE_LEFT_R + ONE;
    /** The column the next sample in this row (the window neighbours) starts in. */
    public static final int NEXT_R = SINGLE_RIGHT_R + ONE;

    @Override
    public String id() {
        return "window arches";
    }

    /** The pose of the pair at a row: front towards the builder. */
    public static WindowPose centred(Direction forward) {
        return WindowPose.upright(forward.getOpposite(), WindowDepth.CENTER);
    }

    public static WindowPose flush(Direction forward) {
        return WindowPose.upright(forward.getOpposite(), WindowDepth.flushAgainst(forward));
    }

    public static WindowPose floor(Direction forward) {
        return new WindowPose(Direction.UP, forward, WindowDepth.NEGATIVE);
    }

    public static WindowPose ceiling(Direction forward) {
        return new WindowPose(Direction.DOWN, forward.getOpposite(), WindowDepth.POSITIVE);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        Direction forward = canvas.frame().forward();
        Appearance oak = Appearance.DEFAULT;
        pair(canvas, oak, CENTRED_R, ROW_F, centred(forward), "Arch pair", "centred");
        pair(canvas, oak, FLUSH_R, ROW_F, flush(forward), "Arch pair", "flush far");
        pair(canvas, oak, FLOOR_R, ROW_F, floor(forward), "Arch pair", "on the floor");
        pair(canvas, oak, CEILING_R, ROW_F, ceiling(forward), "Arch pair", "on the ceiling");
        canvas.sign(SINGLE_LEFT_R, SINGLES_F, "Left piece", "alone");
        canvas.arch(ModBlocks.archLeft(), oak, SINGLE_LEFT_R, 0, SINGLES_F, centred(forward));
        canvas.sign(SINGLE_RIGHT_R, SINGLES_F, "Right piece", "alone");
        canvas.arch(ModBlocks.archRight(), oak, SINGLE_RIGHT_R, 0, SINGLES_F, centred(forward));
    }

    private static void pair(ShowcaseCanvas canvas, Appearance look, int r, int f, WindowPose pose, String... label) {
        canvas.sign(r, f, label);
        canvas.arch(ModBlocks.archLeft(), look, r, 0, f, pose);
        canvas.arch(ModBlocks.archRight(), look, r + 1, 0, f, pose);
    }
}
