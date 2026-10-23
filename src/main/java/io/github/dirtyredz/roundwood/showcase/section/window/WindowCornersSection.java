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
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * The bottom corners (0.7.0): complete 2 x 2 arched windows (arches over corners: one opening, a
 * continuous frame, square bottom corners), in oak only: centred, flush against the far side, on the
 * floor and on the ceiling, and each corner alone. Occupies {@code r 46..58, f 7..13}.
 */
public final class WindowCornersSection implements ShowcaseSection {
    public static final int ROW1_F = 8;
    public static final int ROW2_F = 12;

    @Override
    public String id() {
        return "window corners";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        Direction forward = frame.forward();
        Appearance oak = Appearance.DEFAULT;
        WindowPose centred = WindowArchesSection.centred(forward);
        List<ShowcaseWindow> out = new ArrayList<>();
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, 47, 0, ROW1_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.flush(forward), frame, 50, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.BOTTOM_LEFT), centred, 53, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.BOTTOM_RIGHT), centred, 55, 0, ROW1_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.floor(forward), frame, 47, 0, ROW2_F, oak));
        // a ceiling window's top points back towards the builder: its corners lie one row further away
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.ceiling(forward), frame, 50, 2, ROW2_F + 1, oak));
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(47, ROW1_F - 1, "Arched window", "arches over", "bottom corners");
        canvas.sign(50, ROW1_F - 1, "Arched window", "flush far");
        canvas.sign(53, ROW1_F - 1, "Bottom corner", "left, alone");
        canvas.sign(55, ROW1_F - 1, "Bottom corner", "right, alone");
        canvas.sign(47, ROW2_F - 1, "Arched window", "on the floor");
        canvas.sign(50, ROW2_F - 1, "Arched window", "on the ceiling");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
