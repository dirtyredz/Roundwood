package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Arch sills (0.10.0): a separately fitted bar under each face of the upper arches, styled apart from
 * the perimeter trim and from the other face. An arched window with front sills; a crossed one with a
 * stone-brick trim and differently styled sills on each face; a full circle with a brick trim and a
 * deepslate sill; and a flush arch pair whose wall-side sill stands past the cell. Occupies
 * {@code r 48..59, f 75..77}.
 */
public final class WindowSillsSection implements ShowcaseSection {
    public static final int ROW_F = 76;

    @Override
    public String id() {
        return "window sills";
    }

    private static boolean upperArch(ShowcaseWindow window) {
        return window.design().profile().takesSill();
    }

    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        WindowPose centred = WindowArchesSection.centred(frame.forward());
        Appearance oak = Appearance.DEFAULT;
        Appearance spruce = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));
        Appearance bricks = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
        Appearance stone = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "stone_bricks"));
        Appearance mud = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "mud_bricks"));
        Appearance deepslate = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "deepslate_bricks"));
        List<ShowcaseWindow> out = new ArrayList<>();
        ShowcaseWindow.arched(WindowPattern.PLAIN, centred, frame, 48, 0, ROW_F, oak)
                .forEach(w -> out.add(upperArch(w) ? w.silled(AttachmentFace.FRONT, bricks) : w));
        ShowcaseWindow.arched(WindowPattern.CROSS, centred, frame, 51, 0, ROW_F, oak).forEach(w -> {
            ShowcaseWindow trimmed = w.trimmed(AttachmentFace.FRONT, stone);
            out.add(upperArch(w) ? trimmed.silled(AttachmentFace.FRONT, spruce).silled(AttachmentFace.BACK, mud) : trimmed);
        });
        ShowcaseWindow.circle(WindowPattern.PLAIN, centred, frame, 54, 0, ROW_F, oak).forEach(w -> {
            ShowcaseWindow trimmed = w.trimmed(AttachmentFace.FRONT, bricks);
            out.add(upperArch(w) ? trimmed.silled(AttachmentFace.FRONT, deepslate) : trimmed);
        });
        ShowcaseWindow.grid(WindowPattern.PLAIN, WindowArchesSection.flush(frame.forward()), frame, 57, 0, ROW_F, spruce,
                        new WindowProfile[][] {{WindowProfile.ARCH_LEFT, WindowProfile.ARCH_RIGHT}})
                .forEach(w -> out.add(w.silled(AttachmentFace.FRONT, oak.painted(DyeColor.RED))
                        .silled(AttachmentFace.BACK, oak.painted(DyeColor.RED))));
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(48, ROW_F - 1, "Arch Sill", "brick, front", "under the arches");
        canvas.sign(51, ROW_F - 1, "Stone trim,", "spruce sill front,", "mud brick back");
        canvas.sign(54, ROW_F - 1, "Round window", "brick trim,", "deepslate sill");
        canvas.sign(57, ROW_F - 1, "Flush arches", "red sills,", "both faces");
        windows(canvas.frame()).forEach(canvas::window);
    }
}
