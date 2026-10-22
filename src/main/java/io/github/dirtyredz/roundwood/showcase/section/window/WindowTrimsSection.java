package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
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
import static io.github.dirtyredz.roundwood.showcase.ShowcaseSamples.vanillaMasonry;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseSamples.vanillaWood;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Window trims (the Universal Trim since 0.9.0, the same item as the door's): one trim per face
 * (0.8.0), on every profile, in wood, paint and all four masonry materials, independent of the frame's own look. Row one, centred: both faces, front only, back
 * only, the two faces differently styled, crossed pieces, and a practice target separating trim,
 * frame, beam and tinted glass. Row two, raised one block so each side can be seen: a round window
 * flush against each of the six sides of its cell with trims on both faces, so the wall-side trim
 * projects one pixel beyond the cell. Plus how to fit, restyle and remove one. Occupies
 * {@code r 60..73, f 62..73}.
 */
public final class WindowTrimsSection implements ShowcaseSection {
    public static final int ROW1_F = 63;
    public static final int ROW2_F = 68;

    @Override
    public String id() {
        return "window trims";
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        Direction forward = frame.forward();
        Direction.Axis across = frame.right().getAxis();
        Appearance oak = Appearance.DEFAULT;
        WindowDesign round = WindowDesign.plain(WindowProfile.ROUND);
        WindowDesign roundCross = new WindowDesign(WindowProfile.ROUND, WindowPattern.CROSS);
        WindowPose centred = WindowArchesSection.centred(forward);
        WindowPose roundCentred = WindowPosition.CENTER.pose(across);
        List<ShowcaseWindow> out = new ArrayList<>();
        out.add(ShowcaseWindow.of(round, roundCentred, 60, 0, ROW1_F, oak).trimmed(oak));
        out.add(ShowcaseWindow.of(round, roundCentred, 62, 0, ROW1_F, oak).trimmed(AttachmentFace.FRONT, vanillaMasonry("bricks")));
        out.add(ShowcaseWindow.of(round, roundCentred, 63, 0, ROW1_F, oak).trimmed(AttachmentFace.BACK, vanillaMasonry("stone_bricks")));
        ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, 64, 0, ROW1_F, oak)
                .forEach(w -> out.add(w.trimmed(AttachmentFace.FRONT, vanillaMasonry("bricks")).trimmed(AttachmentFace.BACK, vanillaWood("spruce_planks"))));
        ShowcaseWindow.arched(WindowPattern.CROSS, centred, frame, 67, 0, ROW1_F, oak)
                .forEach(w -> out.add(w.trimmed(vanillaMasonry("stone_bricks"))));
        ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, 70, 0, ROW1_F, vanillaWood("spruce_planks"))
                .forEach(w -> out.add(w.trimmed(AttachmentFace.FRONT, vanillaMasonry("deepslate_bricks"))));
        out.add(ShowcaseWindow.of(roundCross, roundCentred, 72, 0, ROW1_F, vanillaWood("spruce_planks"))
                .trimmed(AttachmentFace.FRONT, vanillaMasonry("mud_bricks")).glass(DyeColor.LIGHT_BLUE));
        int r = 60;
        for (WindowPosition position : List.of(WindowPosition.NORTH, WindowPosition.EAST, WindowPosition.SOUTH,
                WindowPosition.WEST, WindowPosition.UP, WindowPosition.DOWN)) {
            out.add(ShowcaseWindow.of(round, position.pose(across), r, 1, ROW2_F, oak)
                    .trimmed(AttachmentFace.FRONT, vanillaMasonry("bricks")).trimmed(AttachmentFace.BACK, oak.painted(DyeColor.RED)));
            r += 2;
        }
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(60, ROW1_F - 1, "Universal Trim", "both faces");
        canvas.sign(62, ROW1_F - 1, "Front only", "| back only");
        canvas.sign(64, ROW1_F - 1, "Arched window", "brick front,", "spruce back");
        canvas.sign(67, ROW1_F - 1, "Crossed window", "stone brick trim");
        canvas.sign(70, ROW1_F - 1, "Spruce window", "deepslate front");
        canvas.sign(72, ROW1_F - 1, "Practice:", "trim, frame,", "beam, glass");
        canvas.sign(60, ROW2_F - 1, "Flush windows:", "N E S W up down", "trim projects", "past the cell");
        canvas.sign(72, ROW2_F - 1, "Universal Trim:", "window or door", "planks/bricks/dye", "shears remove");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
