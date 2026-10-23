package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;

import java.util.List;

/**
 * A full circle (0.10.0): the lower arches under the upper arches make a circular glazed opening in
 * four square panes, in oak only. Occupies {@code r 58..60, f 7..9}.
 */
public final class WindowCirclesSection implements ShowcaseSection {
    public static final int R = 59;
    public static final int ROW_F = 8;

    @Override
    public String id() {
        return "window circles";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        return ShowcaseWindow.circle(WindowPattern.PLAIN, WindowArchesSection.centred(frame.forward()), frame, R, 0, ROW_F, Appearance.DEFAULT);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(R, ROW_F - 1, "Round window", "four arches,", "upper over lower");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
