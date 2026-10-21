package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
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
 * Window trims (0.7.0): the trim fitted to every profile, on both faces, in wood, paint and all four
 * masonry materials, independent of the pane's own look; centred, flush and on the floor; plus how to
 * fit, restyle and remove one. Occupies {@code r 60..73, f 62..73}.
 */
public final class WindowTrimsSection implements ShowcaseSection {
    public static final int ROW1_F = 63;
    public static final int ROW2_F = 68;

    @Override
    public String id() {
        return "window trims";
    }

    private static Appearance block(String id) {
        return Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", id));
    }

    private static Appearance wood(String id) {
        return Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", id + "_planks"));
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        Direction forward = frame.forward();
        Direction.Axis across = frame.right().getAxis();
        Appearance oak = Appearance.DEFAULT;
        WindowDesign round = WindowDesign.plain(WindowProfile.ROUND);
        WindowPose centred = WindowArchesSection.centred(forward);
        WindowPose roundCentred = WindowPosition.CENTER.pose(across);
        List<ShowcaseWindow> out = new ArrayList<>();
        out.add(ShowcaseWindow.of(round, roundCentred, 60, 0, ROW1_F, oak).trimmed(oak));
        out.add(ShowcaseWindow.of(round, roundCentred, 62, 0, ROW1_F, oak).trimmed(block("bricks")));
        ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, 64, 0, ROW1_F, oak)
                .forEach(w -> out.add(w.trimmed(block("bricks"))));
        ShowcaseWindow.arched(WindowPattern.CROSS, centred, frame, 67, 0, ROW1_F, oak)
                .forEach(w -> out.add(w.trimmed(block("stone_bricks"))));
        ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, 70, 0, ROW1_F, wood("spruce"))
                .forEach(w -> out.add(w.trimmed(block("deepslate_bricks"))));
        out.add(ShowcaseWindow.of(round, roundCentred, 60, 0, ROW2_F, oak).trimmed(block("mud_bricks")));
        out.add(ShowcaseWindow.of(round, roundCentred, 62, 0, ROW2_F, wood("spruce")).trimmed(oak.painted(DyeColor.RED)));
        out.add(ShowcaseWindow.of(round, WindowPosition.flush(forward).pose(across), 64, 0, ROW2_F, oak).trimmed(block("bricks")));
        out.add(ShowcaseWindow.of(round, WindowPosition.DOWN.pose(across), 66, 0, ROW2_F, oak).trimmed(wood("spruce")));
        ShowcaseWindow.arched(WindowPattern.CROSS, WindowArchesSection.floor(forward), frame, 68, 0, ROW2_F, oak)
                .forEach(w -> out.add(w.trimmed(block("mud_bricks"))));
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(60, ROW1_F - 1, "Window trim", "oak");
        canvas.sign(62, ROW1_F - 1, "Window trim", "bricks");
        canvas.sign(64, ROW1_F - 1, "Arched window", "brick trim");
        canvas.sign(67, ROW1_F - 1, "Crossed window", "stone brick trim");
        canvas.sign(70, ROW1_F - 1, "Spruce window", "deepslate trim");
        canvas.sign(60, ROW2_F - 1, "Window trim", "mud bricks");
        canvas.sign(62, ROW2_F - 1, "Spruce window", "red trim");
        canvas.sign(64, ROW2_F - 1, "Trimmed window", "flush far");
        canvas.sign(66, ROW2_F - 1, "Trimmed window", "on the floor");
        canvas.sign(68, ROW2_F - 1, "Floor window", "mud brick trim");
        canvas.sign(72, ROW2_F - 1, "Window Trim:", "use on a window", "planks/bricks/dye", "shears remove");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
