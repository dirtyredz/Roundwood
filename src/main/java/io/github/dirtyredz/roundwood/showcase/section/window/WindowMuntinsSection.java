package io.github.dirtyredz.roundwood.showcase.section.window;

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
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;

/**
 * Cross beams (0.7.0): every crossed piece has its own cross through its middle. The crossed round
 * window centred, flush and on the floor beside a plain one, crossed arches and corners alone, and
 * crossed 2 x 2 windows on the floor and in other looks. Occupies {@code r 60..73, f 50..59}.
 */
public final class WindowMuntinsSection implements ShowcaseSection {
    public static final int ROW1_F = 51;
    public static final int ROW2_F = 55;
    private static final WindowDesign ROUND_CROSS = new WindowDesign(WindowProfile.ROUND, WindowPattern.CROSS);

    @Override
    public String id() {
        return "window cross beams";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        Direction forward = frame.forward();
        Direction.Axis across = frame.right().getAxis();
        Appearance oak = Appearance.DEFAULT;
        WindowPose centred = WindowArchesSection.centred(forward);
        List<ShowcaseWindow> out = new ArrayList<>();
        out.add(ShowcaseWindow.of(ROUND_CROSS, WindowPosition.CENTER.pose(across), 60, 0, ROW1_F, oak).beams(oak.painted(DyeColor.RED)).glass(DyeColor.LIGHT_BLUE));
        out.add(ShowcaseWindow.of(ROUND_CROSS, WindowPosition.flush(forward).pose(across), 62, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(ROUND_CROSS, WindowPosition.DOWN.pose(across), 64, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.ROUND), WindowPosition.CENTER.pose(across), 66, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(new WindowDesign(WindowProfile.ARCH_LEFT, WindowPattern.CROSS), centred, 68, 0, ROW1_F, oak)
                .beams(Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks")))
                .silled(AttachmentFace.FRONT, oak).trimmed(oak.painted(DyeColor.WHITE)));
        out.add(ShowcaseWindow.of(new WindowDesign(WindowProfile.ARCH_RIGHT, WindowPattern.CROSS), centred, 69, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(new WindowDesign(WindowProfile.BOTTOM_LEFT, WindowPattern.CROSS), centred, 71, 0, ROW1_F, oak));
        out.add(ShowcaseWindow.of(new WindowDesign(WindowProfile.BOTTOM_RIGHT, WindowPattern.CROSS), centred, 72, 0, ROW1_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.CROSS, WindowArchesSection.floor(forward), frame, 60, 0, ROW2_F, oak));
        out.addAll(ShowcaseWindow.arched(WindowPattern.CROSS, centred, frame, 63, 0, ROW2_F,
                Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"))));
        out.addAll(ShowcaseWindow.arched(WindowPattern.CROSS, WindowArchesSection.flush(forward), frame, 66, 0, ROW2_F,
                oak.painted(DyeColor.BLUE)));
        out.add(ShowcaseWindow.of(ROUND_CROSS, WindowPosition.CENTER.pose(across), 69, 0, ROW2_F,
                Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "dark_oak_planks"))));
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(60, ROW1_F - 1, "Oak frame", "red cross beams", "blue glazing");
        canvas.sign(62, ROW1_F - 1, "Cross beams", "flush far");
        canvas.sign(64, ROW1_F - 1, "Cross beams", "on the floor");
        canvas.sign(66, ROW1_F - 1, "Round window", "plain");
        canvas.sign(68, ROW1_F - 1, "Crossed arches", "alone");
        canvas.sign(71, ROW1_F - 1, "Crossed corners", "alone");
        canvas.sign(60, ROW2_F - 1, "Crossed window", "on the floor");
        canvas.sign(63, ROW2_F - 1, "Crossed window", "spruce");
        canvas.sign(66, ROW2_F - 1, "Crossed window", "flush far", "blue paint");
        canvas.sign(69, ROW2_F - 1, "Cross beams", "dark oak");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
