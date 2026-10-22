package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Top corners and single edges (0.10.0): rectangular windows of any size from the four corner kinds
 * and the edge runs between them. A 3 x 2 window (bottom corners and a bottom edge under top corners
 * and a top edge), a crossed 2 x 3 window (left and right edges between the corners), the 3 x 2 window
 * with a brick trim in front and a spruce one behind (each edge's trim only on its framed side), and
 * each single edge alone with a stone-brick trim. Occupies {@code r 60..74, f 75..77}.
 */
public final class WindowEdgesSection implements ShowcaseSection {
    public static final int ROW_F = 76;
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
        Appearance spruce = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));
        Appearance bricks = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
        Appearance stone = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "stone_bricks"));
        List<ShowcaseWindow> out = new ArrayList<>();
        out.addAll(ShowcaseWindow.grid(WindowPattern.PLAIN, centred, frame, 60, 0, ROW_F, oak, WIDE));
        out.addAll(ShowcaseWindow.grid(WindowPattern.CROSS, centred, frame, 64, 0, ROW_F, oak, TALL));
        ShowcaseWindow.grid(WindowPattern.PLAIN, centred, frame, 67, 0, ROW_F, oak, WIDE)
                .forEach(w -> out.add(w.trimmed(AttachmentFace.FRONT, bricks).trimmed(AttachmentFace.BACK, spruce)));
        int r = 71;
        for (WindowProfile edge : List.of(WindowProfile.EDGE_TOP, WindowProfile.EDGE_LEFT, WindowProfile.EDGE_BOTTOM,
                WindowProfile.EDGE_RIGHT)) {
            out.add(ShowcaseWindow.of(WindowDesign.plain(edge), centred, r++, 0, ROW_F, oak).trimmed(AttachmentFace.FRONT, stone));
        }
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(60, ROW_F - 1, "Top corners", "and edges:", "a 3 x 2 window");
        canvas.sign(64, ROW_F - 1, "2 x 3 window", "cross beams", "side edges");
        canvas.sign(67, ROW_F - 1, "3 x 2 window", "brick front,", "spruce back");
        canvas.sign(71, ROW_F - 1, "Single edges:", "top, left,", "bottom, right", "trim on the edge");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
