package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;

import java.util.ArrayList;
import java.util.List;

/**
 * Top corners and single edges (0.10.0): rectangular windows of any size from the four corner kinds
 * and the edge runs between them, in oak only. A 3 x 2 window (bottom corners and a bottom edge under
 * top corners and a top edge), a 2 x 3 window (left and right edges between the corners), and each
 * single edge alone. Occupies {@code r 62..75, f 7..9}.
 */
public final class WindowEdgesSection implements ShowcaseSection {
    public static final int ROW_F = 8;
    public static final int WIDE_R = 63;
    public static final int TALL_R = 68;
    public static final int SINGLES_R = 72;
    private static final WindowProfile[][] WIDE = {
            {WindowProfile.BOTTOM_LEFT, WindowProfile.EDGE_BOTTOM, WindowProfile.BOTTOM_RIGHT},
            {WindowProfile.TOP_LEFT, WindowProfile.EDGE_TOP, WindowProfile.TOP_RIGHT}};
    private static final WindowProfile[][] TALL = {
            {WindowProfile.BOTTOM_LEFT, WindowProfile.BOTTOM_RIGHT},
            {WindowProfile.EDGE_LEFT, WindowProfile.EDGE_RIGHT},
            {WindowProfile.TOP_LEFT, WindowProfile.TOP_RIGHT}};

    @Override
    public String id() {
        return "window edges";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        WindowPose centred = WindowArchesSection.centred(frame.forward());
        Appearance oak = Appearance.DEFAULT;
        List<ShowcaseWindow> out = new ArrayList<>();
        out.addAll(ShowcaseWindow.grid(WindowPattern.PLAIN, centred, frame, WIDE_R, 0, ROW_F, oak, WIDE));
        out.addAll(ShowcaseWindow.grid(WindowPattern.PLAIN, centred, frame, TALL_R, 0, ROW_F, oak, TALL));
        int r = SINGLES_R;
        for (WindowProfile edge : List.of(WindowProfile.EDGE_TOP, WindowProfile.EDGE_LEFT, WindowProfile.EDGE_BOTTOM,
                WindowProfile.EDGE_RIGHT)) {
            out.add(ShowcaseWindow.of(WindowDesign.plain(edge), centred, r++, 0, ROW_F, oak));
        }
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(WIDE_R, ROW_F - 1, "Top corners", "and edges:", "a 3 x 2 window");
        canvas.sign(TALL_R, ROW_F - 1, "2 x 3 window", "side edges");
        canvas.sign(SINGLES_R, ROW_F - 1, "Single edges:", "top, left,", "bottom, right");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
