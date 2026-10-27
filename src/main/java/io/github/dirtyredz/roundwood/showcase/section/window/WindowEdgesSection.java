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

import java.util.ArrayList;
import java.util.List;

/**
 * Top corners and single edges (0.10.0): rectangular windows of any size from the four corner kinds
 * and the edge runs between them, in oak only. A 3 x 3 window (a frameless centre between edge runs and
 * top/bottom corners), a 2 x 3 window (left and right edges between the corners), and each
 * single edge alone (the four single edges stand together as one sample). Three samples in the corners' row.
 */
public final class WindowEdgesSection implements ShowcaseSection {
    public static final int ROW_F = WindowCornersSection.ROW_F;
    public static final int WIDE_R = WindowCornersSection.NEXT_R;
    public static final int TALL_R = WIDE_R + ShowcaseSpacing.pitch(3);
    public static final int SINGLES_R = TALL_R + ShowcaseSpacing.pitch(2);
    /** The last column of the section: four single edges side by side. */
    public static final int R_MAX = SINGLES_R + 3;
    private static final WindowProfile[][] WIDE = {
            {WindowProfile.BOTTOM_LEFT, WindowProfile.EDGE_BOTTOM, WindowProfile.BOTTOM_RIGHT},
            {WindowProfile.EDGE_LEFT, WindowProfile.BLANK, WindowProfile.EDGE_RIGHT},
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
        canvas.sign(WIDE_R, ROW_F, "Top corners", "and edges:", "3 x 3, blank centre");
        canvas.sign(TALL_R, ROW_F, "2 x 3 window", "side edges");
        canvas.sign(SINGLES_R, ROW_F, "Single edges:", "top, left,", "bottom, right");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
