package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * The bottom corners (0.7.0): complete 2 x 2 arched windows (arches over corners: one opening, a
 * continuous frame, square bottom corners), in oak only: centred, flush against the far side, on the
 * floor and on the ceiling, and each corner alone. One row of six samples, each
 * {@link ShowcaseSpacing#GAP} clear blocks from the next; occupies {@code r 0..CEILING_R + 1, f 24..25}.
 */
public final class WindowCornersSection implements ShowcaseSection {
    public static final int ROW_F = ShowcaseSpacing.ROW_CORNERS;
    private static final int ONE = ShowcaseSpacing.pitch(1);
    private static final int TWO = ShowcaseSpacing.pitch(2);
    public static final int CENTRED_R = ShowcaseSpacing.FIRST_R;
    public static final int FLUSH_R = CENTRED_R + TWO;
    public static final int LEFT_R = FLUSH_R + TWO;
    public static final int RIGHT_R = LEFT_R + ONE;
    public static final int FLOOR_R = RIGHT_R + ONE;
    public static final int CEILING_R = FLOOR_R + TWO;
    /** The column the next sample in this row (the edges) starts in. */
    public static final int NEXT_R = CEILING_R + TWO;

    @Override
    public String id() {
        return "window corners";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        Direction forward = frame.forward();
        Appearance oak = Appearance.DEFAULT;
        WindowPose centred = WindowArchesSection.centred(forward);
        List<ShowcaseWindow> out = new ArrayList<>();
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, CENTRED_R, 0, ROW_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.flush(forward), frame, FLUSH_R, 0, ROW_F, oak));
        out.add(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.BOTTOM_LEFT), centred, LEFT_R, 0, ROW_F, oak));
        out.add(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.BOTTOM_RIGHT), centred, RIGHT_R, 0, ROW_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.floor(forward), frame, FLOOR_R, 0, ROW_F, oak));
        // a ceiling window's top points back towards the builder: its corners lie one row further away
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.ceiling(forward), frame, CEILING_R, 2, ROW_F + 1, oak));
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(CENTRED_R, ROW_F, "Arched window", "arches over", "bottom corners");
        canvas.sign(FLUSH_R, ROW_F, "Arched window", "flush far");
        canvas.sign(LEFT_R, ROW_F, "Bottom corner", "left, alone");
        canvas.sign(RIGHT_R, ROW_F, "Bottom corner", "right, alone");
        canvas.sign(FLOOR_R, ROW_F, "Arched window", "on the floor");
        canvas.sign(CEILING_R, ROW_F, "Arched window", "on the ceiling");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
