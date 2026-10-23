package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;

import java.util.List;

import static io.github.dirtyredz.roundwood.showcase.ShowcaseSamples.vanillaMasonry;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseSamples.vanillaWood;

/**
 * One Universal Trim example (the same item as the door's and the archway's, one trim per face since
 * 0.8.0): an oak round window with a brick trim on its front and a spruce trim behind. One sample
 * in the arches' row.
 */
public final class WindowTrimsSection implements ShowcaseSection {
    public static final int R = WindowCirclesSection.NEXT_R;
    public static final int F = WindowArchesSection.ROW_F;
    /** The column the next sample in this row starts in. */
    public static final int NEXT_R = R + ShowcaseSpacing.pitch(1);

    @Override
    public String id() {
        return "window trims";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        Appearance oak = Appearance.DEFAULT;
        return List.of(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.ROUND), WindowPosition.CENTER.pose(frame.right().getAxis()), R, 0, F, oak)
                .trimmed(AttachmentFace.FRONT, vanillaMasonry("bricks")).trimmed(AttachmentFace.BACK, vanillaWood("spruce_planks")));
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(R, F, "Universal Trim", "brick front,", "spruce back");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
