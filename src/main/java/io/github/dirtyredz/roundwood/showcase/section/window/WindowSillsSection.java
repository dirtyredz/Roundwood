package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;

import java.util.List;

import static io.github.dirtyredz.roundwood.showcase.ShowcaseSamples.vanillaMasonry;

/**
 * One Arch Sill example (0.10.0): a separately fitted bar under the front face of the upper arches of
 * an oak arched window, in brick. One sample, two columns wide, in the arches' row.
 */
public final class WindowSillsSection implements ShowcaseSection {
    public static final int R = WindowTrimsSection.NEXT_R;
    public static final int F = WindowArchesSection.ROW_F;
    /** The column the next sample in this row starts in. */
    public static final int NEXT_R = R + ShowcaseSpacing.pitch(2);

    @Override
    public String id() {
        return "window sills";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        return ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.centred(frame.forward()), frame, R, 0, F, Appearance.DEFAULT)
                .stream().map(w -> w.design().profile().takesSill() ? w.silled(AttachmentFace.FRONT, vanillaMasonry("bricks")) : w).toList();
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(R, F, "Arch Sill", "brick, front", "under the arches");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
