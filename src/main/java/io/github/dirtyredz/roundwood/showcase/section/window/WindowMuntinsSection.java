package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;

import java.util.List;

/**
 * One Cross Beams example (0.7.0; an attachment since the retired {@code _cross} blocks): an oak round
 * window with an oak cross through its middle. One sample in the arches' row.
 */
public final class WindowMuntinsSection implements ShowcaseSection {
    public static final int R = WindowSillsSection.NEXT_R;
    public static final int F = WindowArchesSection.ROW_F;
    /** The column the next sample in this row starts in. */
    public static final int NEXT_R = R + ShowcaseSpacing.pitch(1);
    private static final WindowDesign ROUND_CROSS = new WindowDesign(WindowProfile.ROUND, WindowPattern.CROSS);

    @Override
    public String id() {
        return "window cross beams";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        return List.of(ShowcaseWindow.of(ROUND_CROSS, WindowPosition.CENTER.pose(frame.right().getAxis()), R, 0, F, Appearance.DEFAULT));
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(R, F, "Cross Beams", "oak cross");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
