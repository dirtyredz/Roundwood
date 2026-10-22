package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Full circles (0.10.0): the lower arches under the upper arches make a circular glazed opening in four
 * square panes, plain and crossed (four crosses, one per piece), with a full masonry ring on both faces,
 * and crossed in spruce with differently styled faces and tinted glass. Occupies {@code r 36..47, f 75..77}.
 */
public final class WindowCirclesSection implements ShowcaseSection {
    public static final int ROW_F = 76;

    @Override
    public String id() {
        return "window circles";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        WindowPose centred = WindowArchesSection.centred(frame.forward());
        Appearance oak = Appearance.DEFAULT;
        Appearance spruce = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));
        Appearance bricks = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
        Appearance stone = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "stone_bricks"));
        List<ShowcaseWindow> out = new ArrayList<>();
        out.addAll(ShowcaseWindow.circle(WindowPattern.PLAIN, centred, frame, 36, 0, ROW_F, oak));
        out.addAll(ShowcaseWindow.circle(WindowPattern.CROSS, centred, frame, 39, 0, ROW_F, oak));
        ShowcaseWindow.circle(WindowPattern.PLAIN, centred, frame, 42, 0, ROW_F, oak).forEach(w -> out.add(w.trimmed(bricks)));
        ShowcaseWindow.circle(WindowPattern.CROSS, centred, frame, 45, 0, ROW_F, spruce)
                .forEach(w -> out.add(w.trimmed(AttachmentFace.FRONT, stone).trimmed(AttachmentFace.BACK, oak.painted(DyeColor.RED))
                        .glass(DyeColor.LIGHT_BLUE)));
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(36, ROW_F - 1, "Round window", "four arches,", "upper over lower");
        canvas.sign(39, ROW_F - 1, "Round window", "cross beams");
        canvas.sign(42, ROW_F - 1, "Round window", "brick trim", "both faces");
        canvas.sign(45, ROW_F - 1, "Spruce, crossed", "stone front,", "red back, tinted");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
