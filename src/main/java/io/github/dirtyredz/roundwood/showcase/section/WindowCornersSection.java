package io.github.dirtyredz.roundwood.showcase.section;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * The bottom corners (0.7.0): complete 2 x 2 arched windows (arches over corners: one opening, a
 * continuous frame, square bottom corners), plain and crossed, centred, flush against the far side,
 * on the floor and on the ceiling, in other looks, and each corner alone. Occupies
 * {@code r 60..73, f 38..47}.
 */
public final class WindowCornersSection implements ShowcaseSection {
    public static final int ROW1_F = 39;
    public static final int ROW2_F = 43;

    @Override
    public String id() {
        return "window corners";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        Direction forward = frame.forward();
        Appearance oak = Appearance.DEFAULT;
        Appearance spruce = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));
        WindowPose centred = WindowArchesSection.centred(forward);
        List<ShowcaseWindow> out = new ArrayList<>();
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, 60, 0, ROW1_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.CROSS, centred, frame, 63, 0, ROW1_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.flush(forward), frame, 66, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.BOTTOM_LEFT), centred, 69, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.BOTTOM_RIGHT), centred, 71, 0, ROW1_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.floor(forward), frame, 60, 0, ROW2_F, oak));
        // a ceiling window's top points back towards the builder: its corners lie one row further away
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, WindowArchesSection.ceiling(forward), frame, 63, 2, ROW2_F + 1, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, 66, 0, ROW2_F, spruce));
        out.addAll(ShowcaseWindow.arched(WindowPattern.CROSS, centred, frame, 69, 0, ROW2_F, oak.painted(DyeColor.RED)));
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(60, ROW1_F - 1, "Arched window", "arches over", "bottom corners");
        canvas.sign(63, ROW1_F - 1, "Arched window", "cross beams");
        canvas.sign(66, ROW1_F - 1, "Arched window", "flush far");
        canvas.sign(69, ROW1_F - 1, "Bottom corner", "left, alone");
        canvas.sign(71, ROW1_F - 1, "Bottom corner", "right, alone");
        canvas.sign(60, ROW2_F - 1, "Arched window", "on the floor");
        canvas.sign(63, ROW2_F - 1, "Arched window", "on the ceiling");
        canvas.sign(66, ROW2_F - 1, "Arched window", "spruce");
        canvas.sign(69, ROW2_F - 1, "Arched window", "cross beams", "red paint");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
